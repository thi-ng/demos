(ns thi.ng.geom.path
  (:require
   [thi.ng.geom.core :as g :refer [*resolution*]]
   [thi.ng.geom.core.vector :as v :refer [vec2]]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.bezier :as b]
   [thi.ng.xerror.core :as err]
   #?(:clj [clojure.xml :as xml]))
  #?(:clj (:import [thi.ng.geom.types Line2 Path2])))

(defn path2
  ([segments]
     (thi.ng.geom.types.Path2. (vec segments)))
  ([s & segments]
     (thi.ng.geom.types.Path2. (vec (cons s segments)))))

(defmulti sample-segment (fn [s res last?] (:type s)))

(defmethod sample-segment :line
  [{[a b] :points} res last?]
  (gu/sample-segment-with-res a b res last?))

(defmethod sample-segment :close
  [{[a b] :points} res last?]
  (gu/sample-segment-with-res a b res last?))

(defmethod sample-segment :bezier
  [{points :points} res last?]
  (b/sample-with-res res last? points))

(defn sample-segments*
  [res segments]
  (let [last (last segments)
        [paths curr] (reduce
                      (fn [[paths curr] seg]
                        (let [curr (concat curr (sample-segment seg res (= seg last)))]
                          (if (= :close (:type seg))
                            [(conj paths curr) []]
                            [paths curr])))
                      [[] []] segments)]
    (if (seq curr)
      (conj paths curr)
      paths)))
(defn parse-svg-coords
  [coords]
  (->> coords
       (re-seq #"[0-9\.\-\+]+")
       #?(:clj (map #(Double/parseDouble %)) :cljs (map js/parseFloat))
       (partition 2)
       (mapv vec2)))

(defn parse-svg-path
  ([svg]
   (parse-svg-path
    (->> svg
         (re-seq #"([MLCZz])\s*(((([0-9\.\-]+)\,?){2}\s*){0,3})")
         (map (fn [[_ t c]]
                [t (parse-svg-coords c)])))
    [0 0] [0 0]))
  ([[[type points :as seg] & more] p0 pc]
   (when seg
     (cond
       (= "M" type)
       (let [p (first points)] (recur more p p))

       (= "L" type)
       (let [p (first points)]
         (lazy-seq (cons {:type :line :points [pc p]}
                         (parse-svg-path more p0 p))))

       (= "C" type)
       (let [p (last points)]
         (lazy-seq (cons {:type :bezier :points (cons pc points)}
                         (parse-svg-path more p0 p))))

       (or (= "Z" type) (= "z" type))
       (lazy-seq (cons {:type :close :points [pc p0]}
                       (parse-svg-path more p0 p0)))

       :default
       (err/unsupported! (str "Unsupported path segment type" type))))))
#?(:clj
   (defn parse-svg
     [src res udist]
     (->> src
          (xml/parse)
          (xml-seq)
          (filter #(= :path (:tag %)))
          (mapv #(parse-svg-path (get-in % [:attrs :d])))
          (map path2))))

(extend-type thi.ng.geom.types.Path2
g/PArea
(area [_])
g/PClassify
(classify-point [_ p])
g/PProximity
(closest-point [_ p])
g/PBoundary
(contains-point? [_ p])
g/PBounds
(bounds [_])
g/PBoundingCircle
(bounding-circle [_] nil)
g/PCenter
(center
 ([_] nil)
 ([_ o] nil))
(centroid [_])
g/PCircumference
(circumference [_] nil)
g/PVertexAccess
(vertices
 [_ res]
 (first (sample-segments* res (:segments _))))
g/PEdgeAccess
(edges [_])
g/PPolygonConvert
(as-polygon
 ([_] nil)
 ([_ res] nil))
g/PSample
(point-at [_ t])
(random-point [_])
(random-point-inside [_])
(sample-uniform
 [_ udist include-last?]
 (->> _
      :segments
      (sample-segments* 8)
      (map #(gu/sample-uniform udist include-last? %))
      (first))) ;; TODO why first?
)
