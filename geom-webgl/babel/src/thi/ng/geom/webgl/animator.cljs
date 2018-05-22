(ns thi.ng.geom.webgl.animator)

(def animframe-provider
  (or
   (.-requestAnimationFrame js/window)
   (.-webkitRequestAnimationFrame js/window)
   (.-mozRequestAnimationFrame js/window)
   (.-msRequestAnimationFrame js/window)
   (.-oRequestAnimationFrame js/window)))

(defn now
  []
  (or
   (.now js/performance)
   (.webkitNow js/performance)
   (.mozNow js/performance)
   (.msNow js/performance)
   (.oNow js/performance)))

(defn animate
  [f & [element]]
  (let [t0 (.getTime (js/Date.))
        t  (volatile! [0 0])
        f' (fn animate* []
             (if (f (vreset! t [(* (- (.getTime (js/Date.)) t0) 0.001) (inc (@t 1))]))
               (if element
                 (animframe-provider animate* element)
                 (animframe-provider animate*))))]
    (f')))
