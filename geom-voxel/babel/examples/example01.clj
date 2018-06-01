(ns example01
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :refer [vec3]]
   [thi.ng.geom.voxel.svo :as svo]
   [thi.ng.geom.voxel.isosurface :as iso]
   [thi.ng.geom.mesh.io :as mio]
   [thi.ng.math.core :as m]
   [clojure.java.io :as io]))

(def res (double 1/8))
(def wall 0.25)

(defn gyroid ^double [s t p]
  "Evaluates gyroid function at scaled point `p`."
  (let [[x y z] (g/* p s)]
    (- (m/abs
        (+ (* (Math/cos x) (Math/sin z))
           (* (Math/cos y) (Math/sin x))
           (* (Math/cos z) (Math/sin y))))
       t)))

(defn voxel-box
  ([tree op flt r]
   (voxel-box tree op flt r r r))
  ([tree op flt rx ry rz]
   (->> (for [x rx y ry, z rz] (vec3 x y z))
        (filter flt)
        (svo/apply-voxels op tree))))

(time
 (def v
   (reduce
    (fn [tree [op r]] (voxel-box tree op identity r))
    (svo/voxeltree 32 res)
    [[svo/set-at (range 10 20 res)] [svo/set-at (range 15 25 res)]])))

(time
 (def v2
   (reduce
    (fn [tree [op r]] (voxel-box tree op identity r))
    v [[svo/delete-at (range (+ 10 wall) (- 20 wall) res)]
       [svo/delete-at (range (+ 15 wall) (- 25 wall) res)]])))

(time
 (def v3
   (reduce
    (fn [tree [op r]]
      (voxel-box tree op #(m/in-range? 0.0 10.0 (gyroid 1.0 1.2 %)) r))
    v2 [[svo/set-at (range (+ 10 wall) (- 20 wall) res)]
        [svo/set-at (range (+ 15 wall) (- 25 wall) res)]])))

(time
 (def v4
   (reduce
    (fn [tree [op rx ry rz]] (voxel-box tree op identity rx ry rz))
    v3 [[svo/delete-at (range 9 26 res) (range 9 26 res) (range 18 26 res)]])))

(time
 (with-open [o (io/output-stream "voxel.stl")]
   (mio/write-stl
    (mio/wrapped-output-stream o)
    (g/tessellate (iso/surface-mesh v4 11 0.5)))))
