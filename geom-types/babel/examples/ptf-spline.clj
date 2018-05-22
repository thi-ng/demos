(ns ptf-spline
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.bezier :as b]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.types.utils.ptf :as ptf]
   [thi.ng.math.core :as m]
   [thi.ng.luxor.core :as lux]
   [thi.ng.luxor.scenes :as scene]
   [thi.ng.luxor.io :as lio]))

(-> (scene/base-scene {:width 640 :height 360})
    (assoc-in [:camera "perspective" :focaldistance 1] 1.4)
    (scene/add-main-mesh
     (ptf/sweep-mesh
      (-> (for [i (range 100)] (v/randvec3))
          (b/auto-spline3)
          (g/vertices))
      (g/vertices (c/circle 0.025) 10))
     {:id :spline :bounds (a/aabb 1.25) :target [0 0.625 -1.75] :rx (- m/THIRD_PI)})
    (lio/serialize-scene "ptf-spline" false)
    (lio/export-scene)
    (dorun))
