(ns loki.app
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [dispatch
                                   path
                                   register-handler
                                   register-sub
                                   subscribe]]
            [re-frame.utils :refer [warn]]
            [schema.core :as s :include-macros true]))

(defonce initial-state
  {:color "green"})


(register-sub
 :color
 (fn
   [db _]
   (reaction (:color @db))))


(defn event-type [type & r]
  (into [(s/one (s/eq type) "type")] r))

(def ColorEvent (event-type :color (s/one s/Str "color")))

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

(defn calling-component []
  [:div "Parent component"
   [some-component]
   [color-input]])


(defn ^:export run []
  (dispatch [:initialize])
  (reagent/render-component [calling-component]
                            (.getElementById js/document "app")))
