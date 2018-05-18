(ns day2.ex03.canvas
  (:require-macros
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [day2.ex03.state :as state]
   [reagent.core :as reagent]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.math.core :as m]
   [thi.ng.color.core :as col]))

(defn canvas-component
  [props]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (reagent/set-state this {:active true})
      ((:init props) this)
      (anim/animate ((:loop props) this)))
    :component-will-unmount
    (fn [this]
      (debug "unmount GL")
      (reagent/set-state this {:active false}))
    :reagent-render
    (fn [_]
      [:canvas
       (merge
        {:width  (.-innerWidth js/window)
         :height (.-innerHeight js/window)}
        props)])}))

(defn draw-dot
  [e]
  (let [canvas (.-target e)
        ctx    (.getContext canvas "2d")
        offx   (.-offsetLeft canvas)
        offy   (.-offsetTop canvas)
        x      (- (.-clientX e) offx)
        y      (- (.-clientY e) offy)]
    (set! (.-fillStyle ctx) (-> (col/random-rgb) (col/as-css) deref))
    (doto ctx
      (.beginPath)
      (.arc x y 20 0 m/TWO_PI false)
      (.fill)
      (.closePath))))

(defn canvas
  [route]
  [canvas-component
   {:width         640
    :height        480
    :init          (fn [comp]
                     (debug "init canvas"))
    :loop          (fn [comp]
                     (let [canvas (reagent/dom-node comp)
                           ctx (.getContext canvas "2d")]
                       (set! (.-fillStyle ctx) "blue"
                             #_(-> (col/random-rgb) (col/as-css) deref))
                       (.fillRect ctx 0 0 (.-width canvas) (.-height canvas))
                       (fn [] (:active (reagent/state comp)))))
    :on-mouse-down (fn [e]
                     (state/set-state! :clicked true)
                     (draw-dot e))
    :on-mouse-up   (fn [e]
                     (state/set-state! :clicked false))
    :on-mouse-move (fn [e]
                     (when (:clicked @state/app)
                       (draw-dot e)))}])
