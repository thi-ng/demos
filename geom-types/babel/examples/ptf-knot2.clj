(ns ptf-knot2
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.types.utils.ptf :as ptf]
   [thi.ng.math.core :as m]
   [thi.ng.luxor.scenes :as scene]
   [thi.ng.luxor.io :as lio]))

(defn cinquefoil
  [t]
  (let [t  (* t m/TWO_PI)
        pt (* 2.0 t)
        qt (* 5.0 t)
        qc (+ 3.0 (Math/cos qt))]
    (v/vec3 (* qc (Math/cos pt)) (* qc (Math/sin pt)) (Math/sin qt))))

(def knot
  (-> (mapv cinquefoil (m/norm-range 400))
      (ptf/compute-frames)
      (ptf/align-frames)))

(-> (scene/base-scene {:width 640 :height 360})
    (scene/add-main-mesh
     (ptf/sweep-strand-mesh knot 0.5 10 7 (g/vertices (c/circle 0.1) 20))
      ;;(ptf/sweep-strand-mesh knot 0.8 6 12 (g/vertices (c/circle 0.1) 20))
     {:id :knot-weave :bounds (a/aabb 1.5) :target [0 0.5 -2] :rx (- m/THIRD_PI)})
    (lio/serialize-scene "ptf-knot-weave" false)
    (lio/export-scene)
    (dorun))
