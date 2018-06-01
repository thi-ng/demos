(ns thi.ng.geom.quad
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.line :as l]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.geom.triangle :as t]
   [thi.ng.geom.types :as types]
   [thi.ng.geom.types.utils :as tu]
   [thi.ng.dstruct.core :as d]
   [thi.ng.xerror.core :as err]
   [thi.ng.math.core :as m :refer [*eps*]])
  #?(:clj
     (:import
      [thi.ng.geom.types Quad3 Triangle3 Line3])))

(defn quad3
  ([] (quad3 1.0))
  ([w] (cond
        (and (sequential? w) (= 4 (count w))) (thi.ng.geom.types.Quad3. (mapv vec3 w))
        (number? w) (thi.ng.geom.types.Quad3. [(vec3) (vec3 w 0.0 0.0) (vec3 w w 0.0) (vec3 0.0 w 0.0)])
        :default (err/illegal-arg! w)))
  ([a b c d] (thi.ng.geom.types.Quad3. [(vec3 a) (vec3 b) (vec3 c) (vec3 d)])))



(defn inset-corner
  "Takes the end points of two lines and an inset vector for each line.
  Computes the shortest line segment between the two lines and returns
  the end point lying on AB (the first line given). Returns nil if there
  is no intersection (i.e. the given lines are parallel or zero length)."
  [a b c d n1 n2]
  (:a (gu/closest-line-between (g/+ a n1) (g/+ b n1) (g/+ c n2) (g/+ d n2))))

(defn inset-quad
  "Takes a vector of four points defining a convex quad and an inset
  distance (use negative value for offsetting). Returns vector of edge
  intersections (the new corner points)."
  [[a b c d] inset]
  (let [nu (g/normalize (g/- (g/mix b c) (g/mix a d)) inset)
        nv (g/normalize (g/- (g/mix d c) (g/mix a b)) inset)
        iu (g/- nu)
        iv (g/- nv)]
    [(inset-corner a d a b nu nv)
     (inset-corner b c a b iu nv)
     (inset-corner b c c d iu iv)
     (inset-corner a d c d nu iv)]))

(extend-type thi.ng.geom.types.Quad3
g/PArea
(area
 [{[a b c d] :points}]
 (+ (gu/tri-area3 a b c) (gu/tri-area3 a c d)))
g/PBoundary
(contains-point?
 [_ p] )
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
 ([_] (thi.ng.geom.types.Quad3. (gu/center (vec3) (:points _))))
 ([_ o] (thi.ng.geom.types.Quad3. (gu/center (g/centroid _) (vec3 o) (:points _)))))
(centroid [_] (gu/centroid (:points _)))
g/PCircumference
(circumference
 [{p :points}] (gu/arc-length (conj p (first p))))
g/PClassify
(classify-point
 [_ p]
 (transduce (map #(g/classify-point (thi.ng.geom.types.Line3. %) p)) min (g/edges _)))
g/PExtrude
(extrude
 [_ {:keys [mesh depth scale offset flags]
     :or {depth 1.0 scale 1.0 flags "nsewfb"}}]
 (let [[a b c d :as v] (g/vertices _)
       norm (gu/ortho-normal a b c)
       offset (or offset (g/* norm depth))
       dp (g/dot norm (g/normalize offset))
       [a2 b2 c2 d2] (if (== 1.0 scale)
                       (mapv #(g/+ offset %) v)
                       (->> (g/scale-size _ scale)
                            (g/vertices)
                            (mapv #(g/+ offset %))))
       [n s e w f b'] (d/demunge-flags-seq flags "nsewfb")]
   (->> [(if n [d2 c2 c d])
         (if s [b2 a2 a b])
         (if e [c2 b2 b c])
         (if w [a2 d2 d a])
         (if b' [a2 b2 c2 d2])
         (if f [d c b a])]
        (sequence ;; FIXME eduction
         (if (neg? dp)
           (comp
            (filter identity)
            (map rseq))
           (filter identity)))
        (g/into (or mesh (bm/basic-mesh))))))
(extrude-shell
 [_ opts] nil)
g/PFlip
(flip
 [_] (thi.ng.geom.types.Quad3. (reverse (:points _))))
g/PVertexAccess
(vertices
 [_] (:points _))
g/PEdgeAccess
(edges
 [{[a b c d] :points}] [[a b] [b c] [c d] [d a]])
g/PGraph
(vertex-neighbors
 [_ v] (d/neighbors (vec3 v) (:points _)))
(vertex-valence
 [_ v] (if ((set (:points _)) v) 2 0))
g/PInset
(inset
 [_ inset] (thi.ng.geom.types.Quad3. (inset-quad (:points _) inset)))
g/PIntersect
(intersect-line
 [_ l])
(intersect-ray
 [_ r])
(intersect-shape
 [_ s])
g/PMeshConvert
(as-mesh
 ([_] (g/as-mesh _ {}))
 ([_ opts] (g/add-face (or (:mesh opts) (bm/basic-mesh)) (:points _))))
g/PPointMap
(map-point
  [{[a b c d] :points} p]
  (let [u1 (gu/closest-point-coeff p a b)
        u2 (gu/closest-point-coeff p d c)
        v1 (gu/closest-point-coeff p a d)
        v2 (gu/closest-point-coeff p b c)
        dab (g/dist p (g/mix a b u1))
        ddc (g/dist p (g/mix d c u2))
        dad (g/dist p (g/mix a d v1))
        dbc (g/dist p (g/mix b c v2))
        u* (+ (* u1 (/ dab (+ dab ddc)))
              (* u2 (/ ddc (+ dab ddc))))
        v* (+ (* v1 (/ dad (+ dad dbc)))
              (* v2 (/ dbc (+ dad dbc))))]
    (vec2 u* v*)))
(unmap-point
  [_ p] (gu/map-bilinear (:points _) p))
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
 [_] (gu/from-barycentric (:points _) (m/normdist-weights 4)))
(sample-uniform
 [{p :points} udist include-last?]
 (gu/sample-uniform udist include-last? (conj p (first p))))
(random-point-inside
 [_] (gu/map-bilinear (:points _) (vec2 (m/random) (m/random))))
g/PSubdivide
(subdivide
 ([_] (g/subdivide _ {}))
 ([{:keys [points]} {:keys [num cols rows] :or {num 2}}]
    (let [ru (d/successive-nth 2 (m/norm-range (or cols num)))
          rv (d/successive-nth 2 (m/norm-range (or rows num)))
          map-p (fn [p] (->> p (gu/map-bilinear points) (mapv #(m/roundto % *eps*)) vec3))]
      (for [[v1 v2] rv, [u1 u2] ru]
        (thi.ng.geom.types.Quad3.
         [(map-p [u1 v1]) (map-p [u2 v1]) (map-p [u2 v2]) (map-p [u1 v2])])))))
g/PTessellate
(tessellate
 ([{[a b c d] :points}]
  [(thi.ng.geom.types.Triangle3. [a b c])
   (thi.ng.geom.types.Triangle3. [a c d])])
 ([_ {tess-fn :fn :or {tess-fn gu/tessellate-3} :as opts}]
  (->> (g/subdivide _ opts)
       (sequence
        (comp
         (mapcat #(tess-fn (:points %)))
         (map #(thi.ng.geom.types.Triangle3. %)))))))
g/PRotate
(rotate
 [_ theta]
 (thi.ng.geom.types.Quad3. (mapv #(g/rotate % theta) (:points _))))
g/PRotate3D
(rotate-x
 [_ theta]
 (thi.ng.geom.types.Quad3. (mapv #(g/rotate-x % theta) (:points _))))
(rotate-y
 [_ theta]
 (thi.ng.geom.types.Quad3. (mapv #(g/rotate-y % theta) (:points _))))
(rotate-z
 [_ theta]
 (thi.ng.geom.types.Quad3. (mapv #(g/rotate-z % theta) (:points _))))
(rotate-around-axis
 [_ axis theta]
 (thi.ng.geom.types.Quad3.
  (mapv #(g/rotate-around-axis % axis theta) (:points _))))
g/PScale
(scale
 ([_ s]
    (thi.ng.geom.types.Quad3. (mapv #(g/* % s) (:points _))))
 ([_ sx sy]
    (thi.ng.geom.types.Quad3. (mapv #(g/* % sx sy) (:points _))))
 ([_ sx sy sz]
    (thi.ng.geom.types.Quad3. (mapv #(g/* % sx sy sz) (:points _)))))
(scale-size
 [_ s] (thi.ng.geom.types.Quad3. (gu/scale-size s (:points _))))
g/PTranslate
(translate
 [_ t]
 (thi.ng.geom.types.Quad3. (mapv #(g/+ % t) (:points _))))
g/PTransform
(transform
 [_ m]
 (thi.ng.geom.types.Quad3. (mapv #(g/transform-vector m %) (:points _))))
g/PVolume
(volume [_] 0.0)
)
