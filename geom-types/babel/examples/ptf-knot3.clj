(ns ptf-knot3
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.types.utils.ptf :as ptf]
   [thi.ng.math.core :as m]
   [thi.ng.luxor.core :as lux]
   [thi.ng.luxor.scenes :as scene]
   [thi.ng.luxor.io :as lio]
   [thi.ng.color.core :as col]))

(defn cinquefoil
  [t]
  (let [t  (* t m/TWO_PI)
        pt (* 2.0 t)
        qt (* 5.0 t)
        qc (+ 3.0 (Math/cos qt))]
    (v/vec3 (* qc (Math/cos pt)) (* qc (Math/sin pt)) (Math/sin qt))))

(defn add-meshes
  [scene meshes opts]
  (let [hue (/ 1.0 (count meshes))]
    (reduce
     (fn [scene [i mesh]]
       (let [mat (str "matte-hue-" i)]
         (-> scene
             (lux/material-matte mat {:diffuse (col/hsva (* i hue) 1.0 0.9)})
             (scene/add-mesh mesh (assoc opts :material mat :id (str "strand-" i))))))
     scene (zipmap (range) meshes))))

(def knot
  (-> (mapv cinquefoil (m/norm-range 400))
      (ptf/compute-frames)
      (ptf/align-frames)))

(-> (scene/base-scene {:width 640 :height 360})
    (add-meshes
     (ptf/sweep-strands knot 0.5 10 7 (g/vertices (c/circle 0.1) 20))
     {:tx {:translate [0 0.5 -2] :scale 0.175 :rx -65}})
    (lio/serialize-scene "ptf-knot-weave-spectrum" false)
    (lio/export-scene)
    (dorun))
