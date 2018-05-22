(ns thi.ng.geom.circle
  (:require
   [thi.ng.geom.core :as g :refer [*resolution*]]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.core.intersect :as isec]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.geom.types :as types]
   [thi.ng.dstruct.core :as d]
   [thi.ng.xerror.core :as err]
   [thi.ng.math.core :as m :refer [PI TWO_PI *eps*]])
  #?(:clj
     (:import
      [thi.ng.geom.types Circle2 Line2 Polygon2 Rect2 Triangle2])))

(defn circle
  ([] (thi.ng.geom.types.Circle2. (vec2) 1.0))
  ([r] (thi.ng.geom.types.Circle2. (vec2) r))
  ([p r] (thi.ng.geom.types.Circle2. (vec2 p) r))
  ([x y r] (thi.ng.geom.types.Circle2. (vec2 x y) r)))

(defn tangent-points
  [{p :p :as c} q]
  (let [m (g/mix p q)]
    (isec/intersect-circle-circle? c (circle m (g/dist m p)))))

(extend-type thi.ng.geom.types.Circle2
g/PArea
(area [{r :r}] (* PI (* r r)))
g/PBounds
(bounds
 [{p :p r :r}]
 (let [d (* 2 r)] (thi.ng.geom.types.Rect2. (g/- p r) (vec2 d))))
(width  [_] (* 2.0 (:r _)))
(height [_] (* 2.0 (:r _)))
(depth  [_] 0)
g/PBoundingCircle
(bounding-circle [_] _)
g/PBoundary
(contains-point?
 [{p :p r :r} q]
 (<= (g/dist-squared p q) (* r r)))
g/PCenter
(center
 ([_] (thi.ng.geom.types.Circle2. (vec2) (:r _)))
 ([_ p] (thi.ng.geom.types.Circle2. (vec2 p) (:r _))))
(centroid [_] (:p _))
g/PCircumference
(circumference [_] (* TWO_PI (:r _)))
g/PClassify
(classify-point
 [_ q]
 (m/signum (- (:r _) (g/dist (:p _) q)) *eps*))
g/PExtrude
(extrude
 [_ {:keys [mesh res depth offset scale top? bottom?]
     :or {res *resolution*, depth 1.0, scale 1.0, top? true, bottom? true}}]
 (let [points (g/vertices _ res)
       tpoints (if (= 1.0 scale)
                 points
                 (g/vertices (circle (:p _) (* scale (:r _))) res))
       off (or offset (vec3 0 0 depth))
       points3 (mapv vec3 points)
       tpoints3 (mapv #(g/+ off %) tpoints)]
   (g/into
    (or mesh (bm/basic-mesh))
    (concat
     (when bottom?
       (->> points
            (gu/tessellate-with-point (:p _))
            (mapv (fn [[a b c]] [(vec3 b) (vec3 a) (vec3 c)]))))
     (mapcat (fn [[a1 b1] [a2 b2]] [[a1 b1 b2 a2]])
             (d/successive-nth 2 (conj points3 (points3 0)))
             (d/successive-nth 2 (conj tpoints3 (tpoints3 0))))
     (when top?
       (->> tpoints
            (gu/tessellate-with-point (:p _))
            (mapv (fn [[a b c]] [(g/+ off a) (g/+ off b) (g/+ off c)]))))))))
(extrude-shell
 [_ opts] (g/extrude-shell (g/as-polygon _) opts))
g/PVertexAccess
(vertices
 ([_] (g/vertices _ *resolution*))
 ([_ res]
    (mapv #(g/point-at _ %) (butlast (m/norm-range res)))))
g/PEdgeAccess
(edges
 ([_] (g/edges _ *resolution*))
 ([_ res]
    (let [verts (g/vertices _ res)]
      (d/successive-nth 2 (conj verts (first verts))))))
g/PIntersect
(intersect-shape
 [_ s]
 (cond
  (instance? thi.ng.geom.types.Circle2 s) (isec/intersect-circle-circle? _ s)
  (instance? thi.ng.geom.types.Rect2 s) (isec/intersect-rect-circle? s _)
  :default (err/type-error! "Circle2" s)))
g/PMeshConvert
(as-mesh
 ([_] (g/as-mesh _ {:res *resolution*}))
 ([_ {:keys [mesh res]}]
    (->> res
         (g/vertices _)
         (mapv vec3)
         (gu/tessellate-with-point (vec3 (:p _)))
         (g/into (or (bm/basic-mesh))))))
g/PPolygonConvert
(as-polygon
 ([_] (g/as-polygon _ *resolution*))
 ([_ res] (thi.ng.geom.types.Polygon2. (vec (g/vertices _ res)))))
g/PProximity
(closest-point
 [{p :p r :r} q]
 (g/+! (g/normalize (g/- q p) r) p))
g/PSample
(point-at
 [_ t]
 (g/+ (:p _) (g/as-cartesian (vec2 (:r _) (* t TWO_PI)))))
(random-point
 [_] (g/point-at _ (m/random)))
(random-point-inside
 [_]
 (g/+ (:p _) (v/randvec2 (m/random (:r _)))))
(sample-uniform
 [_ udist include-last?]
 (let [points (g/vertices _)]
   (gu/sample-uniform udist include-last? (conj (vec points) (first points)))))
g/PTessellate
(tessellate
 ([_] (g/tessellate _ *resolution*))
 ([{p :p :as _} res]
    (->> res
         (g/vertices _)
         (gu/tessellate-with-point p)
         (map #(thi.ng.geom.types.Triangle2. %)))))
g/PRotate
(rotate [_ theta] (thi.ng.geom.types.Circle2. (g/rotate (:p _) theta) (:r _)))
g/PScale
(scale [_ s] (thi.ng.geom.types.Circle2. (g/* (:p _) s) (* (:r _) s)))
(scale-size [_ s] (thi.ng.geom.types.Circle2. (:p _) (* (:r _) s)))
g/PTranslate
(translate [_ t] (thi.ng.geom.types.Circle2. (g/+ (:p _) t) (:r _)))
g/PTransform
(transform [_ m] (g/transform (g/as-polygon _) m))
g/PVolume
(volume [_] 0)
)
