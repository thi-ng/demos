(ns thi.ng.geom.triangle
  #?(:cljs (:require-macros [thi.ng.math.macros :as mm]))
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.core.intersect :as isec]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3 V3Z]]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.geom.types]
   [thi.ng.geom.types.utils :as tu]
   [thi.ng.dstruct.core :as d]
   [thi.ng.math.core :as m :refer [PI HALF_PI THIRD SQRT3 *eps*]]
   [thi.ng.xerror.core :as err]
   #?(:clj [thi.ng.math.macros :as mm]))
  #?(:clj
     (:import
      [thi.ng.geom.types Circle2 Rect2 Polygon2 Triangle2 Triangle3])))

(defn triangle2
  ([t]
     (cond
      (map? t)        (thi.ng.geom.types.Triangle2.
                       [(vec2 (:a t)) (vec2 (:b t)) (vec2 (:c t))])
      (sequential? t) (thi.ng.geom.types.Triangle2.
                       [(vec2 (first t)) (vec2 (nth t 1)) (vec2 (nth t 2))])
      :default (err/illegal-arg! t)))
  ([a b c] (thi.ng.geom.types.Triangle2. [(vec2 a) (vec2 b) (vec2 c)])))

(defn triangle3
  ([t]
     (cond
      (map? t)        (thi.ng.geom.types.Triangle3.
                       [(vec3 (:a t)) (vec3 (:b t)) (vec3 (:c t))])
      (sequential? t) (thi.ng.geom.types.Triangle3.
                       [(vec3 (first t)) (vec3 (nth t 1)) (vec3 (nth t 2))])
      :default (err/illegal-arg! t)))
  ([a b c] (thi.ng.geom.types.Triangle3. [(vec3 a) (vec3 b) (vec3 c)])))

(defn equilateral2
  ([l]
     (cond
      (map? l) (equilateral2 (:p l) (:q l))
      (sequential? l) (equilateral2 (first l) (nth l 1))
      :default (err/illegal-arg! l)))
  ([a b]
     (let [a (vec2 a) b (vec2 b)
           dir (g/- a b)
           n (g/normal dir)
           c (-> n (g/normalize (mm/mul (g/mag dir) SQRT3 0.5)) (g/+ (g/mix a b)))]
       (triangle2 a b c)))
  ([x1 y1 x2 y2]
     (equilateral2 (vec2 x1 y1) (vec2 x2 y2))))

(defn equilateral3
 [a b n]
 (let [a (vec3 a) b (vec3 b)
       dir (g/- b a)
       n (g/normalize (g/cross dir n))
       c (-> n (g/normalize (mm/mul (g/mag dir) SQRT3 0.5)) (g/+ (g/mix a b)))]
    (thi.ng.geom.types.Triangle3. [a b c])))

(defn other-point-in-tri
  [[ta tb tc] a b]
  (if (= a ta)
    (if (= b tb) tc tb)
    (if (= a tb)
      (if (= b ta) tc ta)
      (if (= b ta) tb ta))))

(defn altitude
  ([[a b c] id]
     (case id
       :a (altitude b c a)
       :b (altitude a c b)
       :c (altitude a b c)))
  ([a b c]
     [(g/mix a b (gu/closest-point-coeff c a b)) c]))

(defn norm-altitude
  ([points id]
     (let [[a b] (altitude points id)]
       (g/normalize (g/- b a))))
  ([a b c]
     (g/normalize (g/- c (g/mix a b (gu/closest-point-coeff c a b))))))

(defn centroid
  ([a b c] (g/* (g/+ a b c) THIRD))
  ([[a b c]] (g/* (g/+ a b c) THIRD)))

(defn check-edge
  [splits classifier e p q add-p? add-q?]
  (let [pc (classifier e p)
        qc (classifier e q)
        splits (if add-p? (conj splits [p pc]) splits)]
    (if (neg? (* pc qc))
      (let [ip (:p (g/intersect-line e p q))]
        (if add-q?
          (-> splits (conj [ip 0]) (conj [q qc]))
          (conj splits [ip 0])))
      (if add-q?
        (conj splits [q qc])
        splits))))

(defn slice-with*
  ([t e] (slice-with* t e g/classify-point))
  ([[a b c] e classifier] (slice-with* a b c e classifier))
  ([a b c e classifier]
     (let [verts (-> []
                     (check-edge classifier e a b true true)
                     (check-edge classifier e b c false true)
                     (check-edge classifier e c a false false))
           cmap (fn [ids]
                  (->> ids
                       (map (fn [[a b c]] [(verts a) (verts b) (verts c)])) ;; TODO transducer
                       (reduce
                        (fn [m [a b c]]
                          (update-in m [(a 1)] conj [(a 0) (b 0) (c 0)]))
                        {-1 [] 1 []})))]
       (condp = (count verts)
         4 (let [triverts #{a b c}
                 d (loop [i 3]
                     (if-let [vc (verts i)]
                       (if (and (zero? (vc 1)) (triverts (vc 0)))
                         i (recur (dec i)))))]
             (cmap [[(m/wrap-range (inc d) 4) (m/wrap-range (+ d 2) 4) d]
                    [(m/wrap-range (dec d) 4) d (m/wrap-range (+ d 2) 4)]]))
         5 (if (zero? (get-in verts [1 1]))
             (if (zero? (get-in verts [3 1]))
               (cmap [[0 1 3] [0 3 4] [2 3 1]])
               (cmap [[0 1 4] [2 4 1] [2 3 4]]))
             (cmap [[0 1 2] [0 2 4] [3 4 2]]))
         nil))))

;; http://astronomy.swin.edu.au/~pbourke/modelling/triangulate/
(defn circumcircle-raw
  [[ax ay :as a] [bx by :as b] [cx cy :as c]]
  (let [eq-ab? (m/delta= ay by *eps*)
        eq-bc? (m/delta= by cy *eps*)]
    (when-not (and eq-ab? eq-bc?)
      (let [o (cond
               eq-ab? (let [cx (mm/addm ax bx 0.5)]
                        (vec2 cx (mm/submadd
                                  cx (mm/addm bx cx 0.5)
                                  (- (mm/subdiv cx bx cy by))
                                  (mm/addm by cy 0.5))))
               eq-bc? (let [cx (mm/addm bx cx 0.5)]
                        (vec2 cx (mm/submadd
                                  cx (mm/addm ax bx 0.5)
                                  (- (mm/subdiv bx ax by ay))
                                  (mm/addm ay by 0.5))))
               :default (let [m1 (- (mm/subdiv bx ax by ay))
                              m2 (- (mm/subdiv cx bx cy by))
                              mx1 (mm/addm ax bx 0.5)
                              my1 (mm/addm ay by 0.5)
                              cx (/ (mm/add
                                     (mm/msub m1 mx1 m2 (mm/addm bx cx 0.5))
                                     (mm/addm by cy 0.5)
                                     (- my1))
                                    (- m1 m2))]
                          (vec2 cx (mm/submadd cx mx1 m1 my1))))]
        [o (g/dist o b)]))))

(defn circumcircle
  ([t] (circumcircle (:a t) (:b t) (:c t)))
  ([a b c]
     (let [[o r] (circumcircle-raw a b c)]
       (thi.ng.geom.types.Circle2. o r))))

(extend-type thi.ng.geom.types.Triangle2
g/PArea
(area [_] (apply gu/tri-area2 (:points _)))
g/PClassify
(classify-point
 [_ p] (->> (g/edges _)
            (map #(m/signum (apply gu/closest-point-coeff p %) *eps*))
            (reduce min)))
g/PBoundary
(contains-point?
 [_ p] (apply gu/point-in-triangle2? p (:points _)))
g/PBounds
(bounds [_] (tu/bounding-rect (:points _)))
(width [_] (gu/axis-range 0 (:points _)))
(height [_] (gu/axis-range 1 (:points _)))
(depth [_] 0)
g/PBoundingCircle
(bounding-circle
 [_] (tu/bounding-circle (g/centroid _) (:points _)))
g/PCenter
(center
 ([_] (thi.ng.geom.types.Triangle2. (gu/center (vec2) (:points _))))
 ([_ o] (thi.ng.geom.types.Triangle2. (gu/center (g/centroid _) (vec2 o) (:points _)))))
(centroid [_] (centroid (:points _)))
g/PCircumference
(circumference
 [{[a b c] :points}] (mm/add (g/dist a b) (g/dist b c) (g/dist c a)))
g/PExtrude
(extrude [_ opts] (g/extrude (g/as-polygon _) opts))
(extrude-shell [_ opts] (g/extrude-shell (g/as-polygon _) opts))
g/PFlip
(flip
 [_] (thi.ng.geom.types.Triangle2. (reverse (:points _))))
g/PVertexAccess
(vertices
 [_] (:points _))
g/PEdgeAccess
(edges
 [{[a b c] :points}] [[a b] [b c] [c a]])
g/PGraph
(vertex-neighbors
 [{[a b c] :points} v] (condp = v, a [c b], b [a c], c [b a], nil))
(vertex-valence
 [_ v] (if ((set (:points _)) v) 2 0))
g/PIntersect
(intersect-line
 [_ {[p q] :points}]
 (if (and (g/contains-point? _ p) (g/contains-point? _ q))
   {:type :coincident}
   (isec/intersect-line2-edges? p q (g/edges _))))
(intersect-ray
 ([_ ray]
  (let [[p dir] (if (map? ray) [(:p ray) (:dir ray)] ray)]
    (isec/intersect-ray2-edges? p dir (g/edges _))))
 ([_ p dir]
  (isec/intersect-ray2-edges? p dir (g/edges _))))
g/PMeshConvert
(as-mesh
 ([_] (g/as-mesh _ {}))
 ([_ opts] (g/add-face (or (:mesh opts) (bm/basic-mesh)) (mapv vec3 (:points _)))))
g/PPolygonConvert
(as-polygon
 [_] (thi.ng.geom.types.Polygon2. (:points _)))
g/PProximity
(closest-point
 [_ p]
 (first (gu/closest-point-on-segments p (g/edges _))))
g/PSample
(point-at
 [{p :points} t] (gu/point-at t (conj p (first p))))
(random-point
 [{p :points}] (gu/point-at (m/random) (conj p (first p))))
(random-point-inside
 [_] (gu/from-barycentric (:points _) (m/normdist-weights 3)))
(sample-uniform
 [{p :points} udist include-last?]
 (gu/sample-uniform udist include-last? (conj p (first p))))
g/PSlice
(slice-with
 ([_ e]
  (slice-with* (:points _) e g/classify-point))
 ([_ e classifier]
  (slice-with* (:points _) e classifier)))
g/PSubdivide
(subdivide
 [_] (->> (:points _)
          (gu/tessellate-with-point)
          (map #(thi.ng.geom.types.Triangle2. %))))
g/PTessellate
(tessellate [_] [_])
g/PRotate
(rotate
 [_ theta]
 (thi.ng.geom.types.Triangle2. (mapv #(g/rotate % theta) (:points _))))
g/PScale
(scale
 ([_ s]
    (thi.ng.geom.types.Triangle2. (mapv #(g/* % s) (:points _))))
 ([_ sx sy]
    (thi.ng.geom.types.Triangle2. (mapv #(g/* % sx sy) (:points _))))
 ([_ sx sy sz]
    (thi.ng.geom.types.Triangle2. (mapv #(g/* % sx sy sz) (:points _)))))
(scale-size
 [_ s] (thi.ng.geom.types.Triangle2. (gu/scale-size s (:points _))))
g/PTranslate
(translate
 [_ t]
 (thi.ng.geom.types.Triangle2. (mapv #(g/+ % t) (:points _))))
g/PTransform
(transform
 [_ m]
 (thi.ng.geom.types.Triangle2. (mapv #(g/transform-vector m %) (:points _))))
g/PVolume
(volume [_] 0.0)
)

(extend-type thi.ng.geom.types.Triangle3
g/PArea
(area [_] (apply gu/tri-area3 (:points _)))
g/PBoundary
(contains-point? [_ p] (apply gu/point-in-triangle3? p (:points _)))
g/PBounds
(bounds [_] (tu/bounding-box (:points _)))
(width [_] (gu/axis-range 0 (:points _)))
(height [_] (gu/axis-range 1 (:points _)))
(depth [_] (gu/axis-range 2 (:points _)))
g/PBoundingSphere
(bounding-sphere
 [_] (tu/bounding-sphere (g/centroid _) (:points _)))
g/PCenter
(center
 ([_] (thi.ng.geom.types.Triangle3. (gu/center (vec3) (:points _))))
 ([_ o] (thi.ng.geom.types.Triangle3. (gu/center (g/centroid _) (vec3 o) (:points _)))))
(centroid [_] (centroid (:points _)))
g/PCircumference
(circumference
 [{[a b c] :points}] (mm/add (g/dist a b) (g/dist b c) (g/dist c a)))
g/PClassify
(classify-point [_ p] nil) ; TODO
g/PFlip
(flip
 [_] (thi.ng.geom.types.Triangle3. (reverse (:points _))))
g/PVertexAccess
(vertices
 [_] (:points _))
g/PEdgeAccess
(edges
 [{[a b c] :points}] [[a b] [b c] [c a]])
g/PGraph
(vertex-neighbors
 [{[a b c] :points} v] (condp = v, a [c b], b [a c], c [b a], nil))
(vertex-valence
 [_ v] (if ((set (:points _)) v) 2 0))
g/PIntersect
(intersect-ray
 ([{[a b c] :points} ray]
    (let [[p dir] (if (map? ray) [(:p ray) (:dir ray)] ray)]
      (isec/intersect-ray-triangle3? p dir a b c)))
 ([{[a b c] :points} p dir]
    (isec/intersect-ray-triangle3? p dir a b c)))
g/PMeshConvert
(as-mesh
 ([_] (g/as-mesh _ {}))
 ([_ opts] (g/add-face (or (:mesh opts) (bm/basic-mesh)) (:points _))))
g/PProximity
(closest-point
 [_ p]
 (first (gu/closest-point-on-segments p (g/edges _))))
g/PSample
(point-at
 [{p :points} t] (gu/point-at t (conj p (first p))))
(random-point
 [{p :points}] (gu/point-at (m/random) (conj p (first p))))
(random-point-inside
 [_] (gu/from-barycentric (:points _) (m/normdist-weights 3)))
(sample-uniform
 [{p :points} udist include-last?]
 (gu/sample-uniform udist include-last? (conj p (first p))))
g/PSlice
(slice-with
 ([_ e]
  (slice-with* (:points _) e g/classify-point))
 ([_ e classifier]
  (slice-with* (:points _) e classifier)))
g/PSubdivide
(subdivide
 [_] (->> (:points _)
          (gu/tessellate-with-point)
          (map #(thi.ng.geom.types.Triangle3. %))))
g/PTessellate
(tessellate [_] [_])
g/PRotate
(rotate
 [_ theta]
 (thi.ng.geom.types.Triangle3. (mapv #(g/rotate % theta) (:points _))))
g/PRotate3D
(rotate-x
 [_ theta]
 (thi.ng.geom.types.Triangle3. (mapv #(g/rotate-x % theta) (:points _))))
(rotate-y
 [_ theta]
 (thi.ng.geom.types.Triangle3. (mapv #(g/rotate-y % theta) (:points _))))
(rotate-z
 [_ theta]
 (thi.ng.geom.types.Triangle3. (mapv #(g/rotate-z % theta) (:points _))))
(rotate-around-axis
 [_ axis theta]
 (thi.ng.geom.types.Triangle3.
  (mapv #(g/rotate-around-axis % axis theta) (:points _))))
g/PScale
(scale
 ([_ s]
    (thi.ng.geom.types.Triangle3. (mapv #(g/* % s) (:points _))))
 ([_ sx sy]
    (thi.ng.geom.types.Triangle3. (mapv #(g/* % sx sy) (:points _))))
 ([_ sx sy sz]
    (thi.ng.geom.types.Triangle3. (mapv #(g/* % sx sy sz) (:points _)))))
(scale-size
 [_ s] (thi.ng.geom.types.Triangle3. (gu/scale-size s (:points _))))
g/PTranslate
(translate
 [_ t]
 (thi.ng.geom.types.Triangle3. (mapv #(g/+ % t) (:points _))))
g/PTransform
(transform
 [_ m]
 (thi.ng.geom.types.Triangle3. (mapv #(g/transform-vector m %) (:points _))))
g/PVolume
(volume [_] 0.0)
(signed-volume
 [{[a b c] :points}] (/ (g/dot a (g/cross b c)) 6.0))
)
