(ns thi.ng.geom.tetrahedron
  #?(:cljs (:require-macros [thi.ng.math.macros :as mm]))
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.core.intersect :as isec]
   [thi.ng.geom.core.vector :as v :refer [vec3]]
   [thi.ng.geom.triangle :as t]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.geom.types]
   [thi.ng.geom.types.utils :as tu]
   [thi.ng.dstruct.core :as d]
   [thi.ng.xerror.core :as err]
   [thi.ng.math.core :as m :refer [PI HALF_PI THIRD SQRT3 *eps*]]
   #?(:clj [thi.ng.math.macros :as mm])))

(defn orient-tetra
  "Takes a seq of 4 3D points, returns them as vector in the order so
  that the last point is on the opposite side of the plane defined by
  the first three points."
  [[a b c d :as t]]
  (let [dp (-> d (g/- a) (g/normalize) (g/dot (gu/ortho-normal a b c)))]
    (if (neg? dp) [a b c d] [a c b d])))

(defn tetrahedron
  ([points]
     (thi.ng.geom.types.Tetrahedron.
      (orient-tetra (mapv vec3 points))))
  ([a b c d] (tetrahedron [a b c d])))

(extend-type thi.ng.geom.types.Tetrahedron
g/PArea
(area
 [_] (transduce (map #(m/abs (apply gu/tri-area3 %))) + (g/faces _)))
g/PClassify
(classify-point [_ p] nil) ; TODO
g/PProximity
(closest-point [_ p] nil) ; TODO
g/PBoundary
(contains-point? [_ p] nil) ; TODO
g/PBounds
(bounds [_] (tu/bounding-box (g/vertices _)))
(width [_] (gu/axis-range 0 (g/vertices _)))
(height [_] (gu/axis-range 1 (g/vertices _)))
(depth [_] (gu/axis-range 2 (g/vertices _)))
g/PBoundingSphere
(bounding-sphere
 [_] (tu/bounding-sphere (g/centroid _) (g/vertices _)))
g/PCenter
(center
 ([_] (thi.ng.geom.types.Tetrahedron. (gu/center v/V3 (:points _))))
 ([_ o] (thi.ng.geom.types.Tetrahedron. (gu/center o (:points _)))))
(centroid [_] (gu/centroid (:points _)))
g/PFlip
(flip
 [{[a b c d] :points}] (thi.ng.geom.types.Tetrahedron. [b a c d]))
g/PVertexAccess
(vertices
 [_] (:points _))
g/PEdgeAccess
(edges
 [{[a b c d] :points}]
 [[a b] [b c] [c a] [a d] [b d] [c d]])
g/PFaceAccess
(faces
 [{[a b c d] :points}]
 [[a b c] [a d b] [b d c] [c d a]])
g/PGraph
(vertex-neighbors
 [{[a b c d] :points} v]
 (condp = v
   a [c b d]
   b [a c d]
   c [b a d]
   d [a b c]
   nil))
(vertex-valence
 [_ v] (if ((set (:points _)) v) 3 0))
g/PIntersect
(intersect-shape
 [_ s]
 (cond
  (instance? thi.ng.geom.types.Tetrahedron s)
  (isec/intersect-tetrahedra?
   (orient-tetra (g/vertices _)) (orient-tetra (g/vertices s)))
  (and (sequential? s) (= 4 (count s)))
  (isec/intersect-tetrahedra? (g/vertices _) (orient-tetra s))
  :default (err/type-error! "Tetrahedron" s)))
(intersect-line
 [_ l] nil)
(intersect-ray
 [_ {p :p dir :dir}] nil)
g/PMeshConvert
(as-mesh
 ([_] (g/as-mesh _ {}))
 ([_ opts]
    (let [[a b c d] (orient-tetra (:points _))]
      (g/into (or (:mesh opts) (bm/basic-mesh)) (g/faces _)))))
g/PSample
(point-at [_ t] nil) ; TODO
(random-point
 [_] (g/point-at _ (m/random)))
(random-point-inside
 [_] (gu/from-barycentric (g/vertices _) (m/normdist-weights 4)))
g/PSlice
(slice-with
 ([_ e] nil)
 ([_ e classifier] nil))
g/PSubdivide
(subdivide
 [_]
 (let [cp (gu/centroid (:points _))]
   (map #(tetrahedron (conj % cp)) (g/faces _))))
g/PTessellate
(tessellate
 [_] (g/faces _))
g/PRotate
(rotate [_ theta] (g/rotate-z _ theta))
g/PRotate3D
(rotate-x
 [_ theta]
 (thi.ng.geom.types.Tetrahedron. (mapv #(g/rotate-x % theta) (:points _))))
(rotate-y
 [_ theta]
 (thi.ng.geom.types.Tetrahedron. (mapv #(g/rotate-y % theta) (:points _))))
(rotate-z
 [_ theta]
 (thi.ng.geom.types.Tetrahedron. (mapv #(g/rotate-z % theta) (:points _))))
(rotate-around-axis
 [_ axis theta]
 (thi.ng.geom.types.Tetrahedron.
  (mapv #(g/rotate-around-axis % axis theta) (:points _))))
g/PScale
(scale
 ([_ s]
    (thi.ng.geom.types.Tetrahedron. (mapv #(g/* % s) (:points _))))
 ([_ sx sy]
    (thi.ng.geom.types.Tetrahedron. (mapv #(g/* % sx sy) (:points _))))
 ([_ sx sy sz]
    (thi.ng.geom.types.Tetrahedron. (mapv #(g/* % sx sy sz) (:points _)))))
(scale-size
 [_ s] (thi.ng.geom.types.Tetrahedron. (gu/scale-size s (:points _))))
g/PTranslate
(translate
 [_ t]
 (thi.ng.geom.types.Tetrahedron. (mapv #(g/+ % t) (:points _))))
g/PTransform
(transform
 [_ m]
 (thi.ng.geom.types.Tetrahedron. (mapv #(g/transform-vector m %) (:points _))))
g/PVolume
(volume [{[a b c d] :points}]
  (/ (g/dot (g/- a d) (gu/ortho-normal d b c)) 6.0))
)
