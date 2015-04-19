(ns loki.app
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch
                                   path
                                   register-handler
                                   register-sub
                                   subscribe]]
            [re-frame.utils :refer [warn]]
            [schema.core :as s :include-macros true]
            [goog.net.XhrIo :as xhr]
            [cljsjs.leaflet]))

(defonce initial-state
  {:geojson nil
   :leaflet-geojson nil
   :regions-data {}})


(register-sub
 :geojson
 (fn
   [db _]
   (reaction (:geojson @db))))

(register-sub
 :regions-data
 (fn
   [db _]
   (reaction (:regions-data @db))))

(register-sub
 :leaflet-geojson
 (fn
   [db _]
   (reaction (:leaflet-geojson @db))))


(defn event-type
  "Generates Schema description for vector with first element equal to the
  passed type and all other equal to the rest arguments"
  [type & r]
  (into [(s/one (s/eq type) "type")] r))

(def GeoJSON js/Object)

(def LeafletGeoJson js/Object)

(def GeoJSONReceivedEvent
  (event-type :geojson-received (s/one GeoJSON "geojson")))

(def InitializeEvent
  (event-type :initialize))

(def TweetReceivedEvent
  (event-type :tweet-received
              {:tweet s/Any :rating (s/enum :win :treason) :region s/Str}))

(def LeafletGeoJsonAddedEvent
  (event-type :leaflet-geojson-added (s/one LeafletGeoJson "leafletGeoJson")))

(defn validate
  "Middleware which augments underlying handler call with Schema validation.

  It's expected that handler itself is pure and provides required Schema
  annotations."
  [handler]
  (fn new-handler
    [db v]
    (try
      (s/with-fn-validation (handler db v))
      (catch :default e
        (warn e)
        db))))

(s/defn initialize-handler
  [db _ :- InitializeEvent]
  (merge db initial-state))

(s/defn geojson-received-handler
  [geojson [_ value] :- GeoJSONReceivedEvent]
  value)

(s/defn tweet-received-handler
  [db [_ tweet-event] :- TweetReceivedEvent]
  (let [{:keys [region rating tweet]} tweet-event]
    (update-in db [:regions-data region]
               (fn [val]
                 (let [num-rating (if (= :win rating) 1 -1)]
                   (if val
                     (let [{:keys [tweets rating]} val]
                       {:tweets (conj tweets tweet)
                        :rating (+ rating num-rating)})
                     {:tweets [tweet] :rating num-rating}))))))

(s/defn leaflet-geojson-added-handler
  [db [_ map] :- LeafletGeoJsonAddedEvent]
  (assoc db :leaflet-geojson map))

(register-handler
 :initialize
 [validate]
 initialize-handler)

(register-handler
 :geojson-received
 [(path [:geojson]) validate]
 geojson-received-handler)

(register-handler
 :tweet-received
 [validate]
 tweet-received-handler)

(register-handler
 :leaflet-geojson-added
 [validate]
 leaflet-geojson-added-handler)


(defn map-holder [regions-data-ratom]
  (fn []
    ;; Hack to run component-did-update on regions-data-ratom change
    @regions-data-ratom
    [:div [:h3 "Map"]
     [:div#map]]))

(defn get-color [rating]
  (cond
    (< rating -40) "#a50026"
    (< rating -30) "#d73027"
    (< rating -20) "#f46d43"
    (< rating -10) "#fdae61"
    (< rating 0) "#fee090"

    (> rating 40) "#313695"
    (> rating 30) "#4575b4"
    (> rating 20) "#74add1"
    (> rating 10) "#abd9e9"
    (> rating 0) "#e0f3f8"

    :else "#ffffbf"))

(defn style [regions-data]
  (fn [feature]
    {:fillColor (let [region (-> feature
                                  (.-properties)
                                  (.-name))
                      region-data (get regions-data region {})]
                  (get-color (get region-data :rating 0)))
     :weight 2
     :opacity 1
     :color "white"
     :dashArray "3"
     :fillOpacity 0.7}))

(defn update-geojson-style [regions-data-ratom leaflet-geojson-ratom]
  (fn []
    (.eachLayer @leaflet-geojson-ratom
                (fn [l]
                  (->> ((style @regions-data-ratom) (.-feature l))
                       clj->js
                       (.setStyle l))))))

(defn add-map-with [geojson regions-data]
  (fn []
    (let [map (.setView (.map js/L "map") #js [49 31] 5)
          leaflet-geojson
          (.geoJson js/L geojson
                    (clj->js {:style (comp clj->js (style regions-data))}))]
      (dispatch [:leaflet-geojson-added leaflet-geojson])
      (.addTo (.tileLayer js/L "http://{s}.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={token}"
                          (clj->js {:id "gsnewmark.lp1opmko"
                                    :token "pk.eyJ1IjoiZ3NuZXdtYXJrIiwiYSI6IkFfcXR6Q3cifQ.-8u0leDtUFLwPkO9kQRcTQ"
                                    :minZoom 5
                                    :maxZoom 6}))
              map)
      (.addTo leaflet-geojson map))))

(defn map-component [geojson]
  (let [regions-data (subscribe [:regions-data])
        leaflet-geojson (subscribe [:leaflet-geojson])]
    (reagent/create-class
     {:reagent-render (map-holder regions-data)
      :component-did-mount (add-map-with geojson @regions-data)
      :component-did-update (update-geojson-style regions-data leaflet-geojson)})))

(defn page-component []
  (let [geojson (subscribe [:geojson])]
    (fn []
      [:div
       [:h2 "loki"]
       (if (and @geojson)
         [map-component @geojson]
         "Loading...")])))


(defn ^:export run []
  (dispatch [:initialize])
  (xhr/send "ua-regions.geojson"
            (fn [event]
              (let [res (-> event .-target .getResponseText)]
                (dispatch [:geojson-received (.parse js/JSON res)]))))
  (reagent/render-component [page-component]
                            (.getElementById js/document "app")))
