(ns thi.ng.geom.types.test.aabb
  #?(:cljs
     (:require-macros
      [cemerick.cljs.test :refer (is deftest with-test run-tests testing)]))
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :refer [vec3]]
   [thi.ng.geom.core.matrix :refer [M44]]
   [thi.ng.geom.types]
   [thi.ng.geom.types.utils :as tu]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.cuboid :as cu]
   [thi.ng.geom.sphere :as s]
   [thi.ng.geom.gmesh :as gm]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.geom.triangle :as t]
   #?(:clj
      [clojure.test :refer :all]
      :cljs
      [cemerick.cljs.test])))

(deftest test-ctors
  (is (= (a/aabb [100 200 300] [10 20 30])
         (a/aabb-from-minmax [100 200 300] [110 220 330]))
      "aabb-from-minmax")
  (is (= (a/aabb [0 0 0] [10 10 10]) (a/aabb 10))
      "aabb n")
  (is (= (a/aabb [0 0 0] [10 20 30]) (a/aabb 10 20 30))
      "aabb sz sy sz"))

(deftest test-impls
  (let [[px py pz :as p] (vec3 100 200 300)
        [w h d :as s]    (vec3 10 20 30)
        [qx qy qz :as q] (g/+ p s)
        c (g/madd s 0.5 p)
        a (a/aabb p s)]
    (is (== (* 2 (+ (* w h) (* w d) (* h d))) (g/area a)) "area")
    (is (== (* w h d) (g/volume a)) "volume")
    (is (= a (g/bounds a)) "bounds")
    (is (= w (g/width a)) "width")
    (is (= h (g/height a)) "height")
    (is (= d (g/depth a)) "depth")
    (is (= (s/sphere c (g/dist c (g/+ p s))) (g/bounding-sphere a)) "bounding sphere")
    (is (= c (g/centroid a)) "centroid")
    (is (= (vec3) (g/centroid (g/center a))) "center + centroid")
    (is (= (vec3 -1 -2 -3) (g/centroid (g/center a (vec3 -1 -2 -3)))) "center p + centroid")
    (is (= 8 (count (g/vertices a))) "vert count")
    (is (= 6 (count (g/faces a))) "face count")
    (is (= 12 (count (g/edges a))) "edge count")
    (is (instance? thi.ng.geom.types.BasicMesh (g/as-mesh a)) "as bmesh")
    (is (instance? thi.ng.geom.types.GMesh (g/as-mesh a {:mesh (gm/gmesh)})) "as gmesh")
    (is (= 1 (count (g/faces (g/as-mesh a {:flags :n})))))
    (is (= 1 (count (g/faces (g/as-mesh a {:flags :s})))))
    (is (= 1 (count (g/faces (g/as-mesh a {:flags :e})))))
    (is (= 1 (count (g/faces (g/as-mesh a {:flags :w})))))
    (is (= 1 (count (g/faces (g/as-mesh a {:flags :f})))))
    (is (= 1 (count (g/faces (g/as-mesh a {:flags :b})))))
    (is (every? #(g/contains-point? a %)
                (take 1000 (repeatedly #(g/random-point-inside a))))
        "random-p-inside contains")
    (is (every? pos?
                (take 1000 (repeatedly #(g/classify-point a (g/random-point-inside a)))))
        "random-p-inside classify")
    (is (every? zero?
                (take 1000 (repeatedly #(g/classify-point a (g/random-point a)))))
        "random-p classify on surface")
    (is (= 27 (count (g/subdivide a {:num 3}))) "subdiv :num")
    (is (= 6 (count (g/subdivide a {:cols 3 :rows 2}))) "subdiv :cols :rows")
    (is (= 12 (count (g/subdivide a {:cols 3 :rows 2 :slices 2}))) "subdiv :cols :rows :slices")
    (is (= 12 (count (g/tessellate a))) "tessellate")
    (is (= (a/aabb s) (g/translate a (g/- p))) "translate")
    (is (= (a/aabb (g/* p 2) (g/* s 2)) (g/scale a 2)) "scale")
    (is (= (a/aabb (g/madd s -0.5 p) (g/* s 2)) (g/scale-size a 2)) "scale-size")
    (is (= (cu/cuboid (vec3) s) (g/transform a (g/translate M44 (g/- p)))) "translate via mat")
    (is (= (cu/cuboid (g/* p 2) (g/* s 2)) (g/transform a (g/scale M44 2))) "scale via mat")
    (is (= (a/aabb [-1 -2 -3] [3 5 7]) (g/union (a/aabb [-1 -2 -3] 1) (a/aabb [1 2 3] 1))) "union")
    (is (= (a/aabb) (g/union (a/aabb) (a/aabb))) "union self")
    (is (= (a/aabb 0.5 0.5) (g/intersection (a/aabb) (a/aabb 0.5 1))) "intersection aabb 1")
    (is (= (a/aabb 1 0) (g/intersection (a/aabb) (a/aabb 1 1))) "intersection aabb 2")
    (is (= (vec3) (g/map-point a p)) "map-point 1")
    (is (= (vec3 1) (g/map-point a q)) "map-point 2")
    (is (= (vec3 0 1 1) (g/map-point a (vec3 px qy qz))) "map-point 3")
    (is (= (vec3 1 0 1) (g/map-point a (vec3 qx py qz))) "map-point 4")
    (is (= (vec3 1 1 0) (g/map-point a (vec3 qx qy pz))) "map-point 5")
    (is (= (vec3 0.5) (g/map-point a (g/centroid a))) "map-point centroid")
    (is (= p (g/unmap-point a (vec3))) "unmap-point 1")
    (is (= q (g/unmap-point a (vec3 1))) "unmap-point 2")
    (is (= c (g/unmap-point a (vec3 0.5))) "unmap-point 3")
    ))
