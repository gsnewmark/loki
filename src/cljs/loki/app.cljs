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
  {:color "green"
   :ua-regions-json nil})


(register-sub
 :color
 (fn
   [db _]
   (reaction (:color @db))))

(register-sub
 :ua-regions-json
 (fn
   [db _]
   (reaction (:ua-regions-json @db))))


(defn event-type [type & r]
  (into [(s/one (s/eq type) "type")] r))

(def ColorEvent (event-type :color (s/one s/Str "color")))

(def UaRegionsJsonEvent (event-type :ua-regions-json (s/one js/Object "json")))

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

(register-handler
 :initialize
 (fn
   [db _]
   (merge db initial-state)))

(register-handler
 :color
 [(path [:color]) validate]
 (s/fn color-handler
   [time-color [_ value] :- ColorEvent]
   value))

(register-handler
 :ua-regions-json
 [(path [:ua-regions-json]) validate]
 (s/fn color-handler
   [json [_ value] :- UaRegionsJsonEvent]
   value))


(defn some-component []
  (let [color (subscribe [:color])]
    (fn []
      (when @color
        [:div
         [:h3 "I am a component!"]
         [:p.someclass
          "I have " [:strong "bold"]
          [:span {:style {:color @color}} " and colored"]
          " text."]]))))

(defn color-input []
  (let [color (subscribe [:color])]
    (fn []
      [:div.color-input
       "Color: "
       [:input {:type "text"
                :value @color
                :on-change #(dispatch
                             [:color (-> % .-target .-value)])}]])))

(defn map-holder []
  [:div [:h1 "Map"]
   [:div#map ]
   ])

(defn map-did-mount-gen [regions]
  (fn []
    (let [map (.setView (.map js/L "map") #js [49 31] 5)]
      (doto map)
      (.addTo (.tileLayer js/L "http://{s}.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={token}"
                          (clj->js {:attribution "Map data &copy; [...]"
                                    :id "gsnewmark.lp1opmko"
                                    :token ""
                                    :minZoom 5
                                    :maxZoom 6}))
              map)
      (.addTo (.geoJson js/L regions)
              map))))

(defn map-component [regions]
  (reagent/create-class {:reagent-render map-holder
                         :component-did-mount (map-did-mount-gen regions)}))

(defn calling-component []
  (let [regions (subscribe [:ua-regions-json])]
    (fn []
      [:div "Parent component"
       [some-component]
       [color-input]
       (when @regions
         [map-component @regions])])))


(defn ^:export run []
  (dispatch [:initialize])
  (xhr/send "ua-regions.geojson"
            (fn [event]
              (let [res (-> event .-target .getResponseText)]
                (dispatch [:ua-regions-json (.parse js/JSON res)]))))
  (reagent/render-component [calling-component]
                            (.getElementById js/document "app")))
