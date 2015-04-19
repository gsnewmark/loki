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
  {:geojson nil})


(register-sub
 :geojson
 (fn
   [db _]
   (reaction (:geojson @db))))


(defn event-type
  "Generates Schema description for vector with first element equal to the
  passed type and all other equal to the rest arguments"
  [type & r]
  (into [(s/one (s/eq type) "type")] r))

(def GeoJSON js/Object)

(def GeoJSONReceivedEvent
  (event-type :geojson-received (s/one GeoJSON "geojson")))

(def InitializeEvent
  (event-type :initialize))

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

(register-handler
 :initialize
 [validate]
 initialize-handler)

(register-handler
 :geojson-received
 [(path [:geojson]) validate]
 geojson-received-handler)


(defn map-holder []
  [:div [:h3 "Map"]
   [:div#map]])

(defn add-map-with [geojson]
  (fn []
    (let [map (.setView (.map js/L "map") #js [49 31] 5)]
      (.addTo (.tileLayer js/L "http://{s}.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={token}"
                          (clj->js {:attribution "Map data &copy; [...]"
                                    :id "gsnewmark.lp1opmko"
                                    :token "pk.eyJ1IjoiZ3NuZXdtYXJrIiwiYSI6IkFfcXR6Q3cifQ.-8u0leDtUFLwPkO9kQRcTQ"
                                    :minZoom 5
                                    :maxZoom 6}))
              map)
      (.addTo (.geoJson js/L geojson)
              map))))

(defn map-component [geojson]
  (reagent/create-class {:reagent-render map-holder
                         :component-did-mount (add-map-with geojson)}))

(defn page-component []
  (let [geojson (subscribe [:geojson])]
    (fn []
      [:div
       [:h2 "loki"]
       (if @geojson
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
