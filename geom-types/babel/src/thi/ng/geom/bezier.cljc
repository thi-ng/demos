(ns thi.ng.geom.bezier
  (:require
   [thi.ng.geom.core :as g :refer [*resolution*]]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.types]
   [thi.ng.geom.types.utils :as tu]
   [thi.ng.geom.types.utils.ptf :as ptf]
   [thi.ng.dstruct.core :as d]
   [thi.ng.math.core :as m]))

(defn bernstein
  [t]
  (let [it (- 1.0 t) it2 (* it it) t2 (* t t)]
    [(* it it2) (* 3 (* t it2)) (* 3 (* it t2)) (* t t2)]))
(defn interpolate
  [[a b c d] t]
  (let [it (- 1.0 t), it2 (* it it), t2 (* t t)]
    (->> (g/* a (* it it2))
         (g/madd b (* 3 (* t it2)))
         (g/madd c (* 3 (* it t2)))
         (g/madd d (* t t2)))))

(defn sample-segment
  [seg res]
  (map #(interpolate seg %) (butlast (m/norm-range res))))

(defn sample-with-res
  [res include-last? points]
  (let [ls (->> points
                (d/successive-nth 4)
                (take-nth 3)
                (mapcat #(sample-segment % res)))]
    (if include-last?
      (concat ls [(last points)])
      ls)))
(defn- find-cpoints*
  [ctor tight points]
  (let [np (count points)
        invt (/ 1.0 tight)
        points (vec points)
        c1 (g/subm (points 2) (first points) tight)
        [bi coeff] (reduce
                    (fn [[bi coeff] i]
                      (let [b (/ -1.0 (+ invt (peek bi)))
                            c (peek coeff)
                            p (get points (dec i))
                            q (get points (inc i))]
                        [(conj bi b)
                         (conj coeff (g/* (g/- q p c) (- b)))]))
                    [[0 (- tight)] [(ctor) c1]]
                    (range 2 (dec np)))]
    (reduce
     (fn [delta i]
       (assoc delta i (g/madd (delta (inc i)) (bi i) (coeff i))))
     (vec (repeatedly np ctor))
     (range (- np 2) 0 -1))))

(defn auto-spline*
  [points cpoints]
  (concat
   (->> cpoints
        (d/successive-nth 2)
        (interleave (d/successive-nth 2 points))
        (partition 2)
        (mapcat (fn [[[p q] [dp dq]]] [p (g/+ p dp) (g/- q dq)])))
   [(last points)]))

(defn bezier2
  [points] (thi.ng.geom.types.Bezier2. (mapv vec2 points)))

(defn auto-spline2
  ([points]
   (->> points
        (find-cpoints* vec2 0.25)
        (auto-spline* points)
        (thi.ng.geom.types.Bezier2.)))
  ([points closed?]
   (auto-spline2
    (if closed?
      (conj (vec points) (first points))
      points))))

(defn bezier3
  [points] (thi.ng.geom.types.Bezier3. (mapv vec3 points)))

(defn auto-spline3
  ([points]
   (->> points
        (find-cpoints* vec3 0.25)
        (auto-spline* points)
        (thi.ng.geom.types.Bezier3.)))
  ([points closed?]
   (auto-spline3
    (if closed?
      (conj (vec points) (first points))
      points))))

(extend-type thi.ng.geom.types.Bezier2
g/PVertexAccess
(vertices
 ([_] (g/vertices _ *resolution*))
 ([_ res] (sample-with-res res true (:points _))))
g/PEdgeAccess
(edges
 ([_] (d/successive-nth 2 (g/vertices _ *resolution*)))
 ([_ res] (d/successive-nth 2 (g/vertices _ res))))
g/PGraph
(vertex-neighbors
 [_ v] (d/neighbors v (g/vertices _)))
(vertex-valence
 [_ v]
 (let [points (g/vertices _)]
   (if-let [p (d/neighbors v points)]
     (if (or (m/delta= p (first points)) (m/delta= p (peek points)))
       1 2)
     0)))
g/PMeshConvert
(as-mesh
 [_ {:keys [res dist profile] :as opts}]
 (let [points (if dist
                (g/sample-uniform _ dist true)
                (g/vertices _ (or res *resolution*)))]
   (ptf/sweep-mesh (map vec3 points) profile opts)))
g/PProximity
(closest-point
 [_ p]
 (first (gu/closest-point-on-segments p (g/edges _))))
g/PSample
(point-at
 [_ t] (gu/point-at t (:points _) nil))
(random-point
 [_] (gu/point-at (m/random) (:points _) nil))
(random-point-inside
 [_] (g/random-point _))
(sample-uniform
 [_ udist include-last?]
 (gu/sample-uniform udist include-last? (g/vertices _)))
)

(extend-type thi.ng.geom.types.Bezier3
g/PVertexAccess
(vertices
 ([_] (g/vertices _ *resolution*))
 ([_ res] (sample-with-res res true (:points _))))
g/PEdgeAccess
(edges
 ([_] (d/successive-nth 2 (g/vertices _ *resolution*)))
 ([_ res] (d/successive-nth 2 (g/vertices _ res))))
g/PGraph
(vertex-neighbors
 [_ v] (d/neighbors v (g/vertices _)))
(vertex-valence
 [_ v]
 (let [points (g/vertices _)]
   (if-let [p (d/neighbors v points)]
     (if (or (m/delta= p (first points)) (m/delta= p (peek points)))
       1 2)
     0)))
g/PMeshConvert
(as-mesh
 [_ {:keys [res dist profile] :as opts}]
 (let [points (if dist
                (g/sample-uniform _ dist true)
                (g/vertices _ (or res *resolution*)))]
   (ptf/sweep-mesh points profile opts)))
g/PProximity
(closest-point
 [_ p]
 (first (gu/closest-point-on-segments p (g/edges _))))
g/PSample
(point-at
 [_ t] (gu/point-at t (:points _) nil))
(random-point
 [_] (gu/point-at (m/random) (:points _) nil))
(random-point-inside
 [_] (g/random-point _))
(sample-uniform
 [_ udist include-last?]
 (gu/sample-uniform udist include-last? (g/vertices _)))
)
