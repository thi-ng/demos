(ns ptf-knot
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.types.utils.ptf :as ptf]
   [thi.ng.math.core :as m :refer [THIRD_PI TWO_PI]]
   [thi.ng.luxor.scenes :as scene]
   [thi.ng.luxor.io :as lio]))

(defn cinquefoil
  [t]
  (let [t  (* t m/TWO_PI)
        pt (* 2.0 t)
        qt (* 5.0 t)
        qc (+ 3.0 (Math/cos qt))]
    (v/vec3 (* qc (Math/cos pt)) (* qc (Math/sin pt)) (Math/sin qt))))

(-> (scene/base-scene {:width 640 :height 360})
    (scene/add-main-mesh
     (ptf/sweep-mesh
      (mapv cinquefoil (m/norm-range 400))
      (g/vertices (c/circle 0.5) 20)
      {:align? true})
     {:id :knot :bounds (a/aabb 1.5) :target [0 0.5 -2] :rx (- m/THIRD_PI)})
    (lio/serialize-scene "ptf-knot" false)
    (lio/export-scene)
    (dorun))
