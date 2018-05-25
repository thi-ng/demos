(ns day2.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs.core.async.macros :refer [go go-loop]]
   [cljs-log.core :refer [debug info warn]])
  (:require
    [reagent.core :as reagent]
    [thi.ng.color.core :as col]
    [thi.ng.geom.core.vector :as v]
    [thi.ng.geom.svg.core :as svg]))

(defonce app
  (reagent/atom
    {:color [1 0 0]}))

(defn input
  []
  [:input
   {:type "text"
    :on-change (fn [e]
                 (swap! app assoc :name
                        (-> e .-target .-value)))}])

(defn triangle
  [a b c col]
  (svg/polygon [a b c] {:fill (col/as-hsva (col/rgba (:color @app))) :stroke "black"}))

(defn with-key
  [attr] (assoc attr :key (rand)))

(defn ^:export app-component
  []
  (let [name (reaction (:name @app))
        mpos (reaction (v/vec2 (:mouseX @app) (:mouseY @app)))]
    (fn []
      [:div
       [:h1 "hello " @name]
       [input]
       [svg/svg
         {:width 600
          :height 600
          :on-mouse-move
          (fn [e]
            (swap! app
                   (fn [state]
                     (-> state
                       (assoc-in [:color 0] (* (.-clientX e) 0.001))
                       (assoc-in [:color 2] (* (.-clientY e) 0.001))))))
          }
         ^{:key "t1"} [triangle [0 0] [300 0] @mpos "red"]
         ]])))

(defn init-listeners
  []
  (.addEventListener js/window "mousemove"
    (fn [e]
      (swap! app assoc
             :mouseX (-> e .-clientX)
             :mouseY (-> e .-clientY))
      (debug (select-keys @app [:mouseX :mouseY]))))
  (swap! app assoc :inited true)
  (debug :inited))

(defn main
  []
  (when (not (:inited @app))
    (init-listeners))
  (reagent/render-component
    [app-component]
    (.-body js/document)))

(main)