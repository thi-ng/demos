(ns thi.ng.geom.types.utils.ptf
  #?(:cljs (:require-macros [thi.ng.math.macros :as mm]))
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :refer [vec2 vec3 V3]]
   [thi.ng.geom.core.matrix :refer [matrix44 M44]]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.dstruct.core :as d]
   [thi.ng.math.core :as m :refer [*eps* TWO_PI]]
   #?(:clj [thi.ng.math.macros :as mm])))

(defn compute-tangents
  [points]
  (let [t (mapv (fn [[p q]] (g/normalize (g/- q p))) (d/successive-nth 2 points))]
    (conj t (peek t))))

(defn compute-frame
  [tangents norms bnorms i]
  (let [ii (dec i)
        p  (tangents ii)
        q  (tangents i)
        a  (g/cross p q)
        n  (if-not (m/delta= 0.0 (g/mag-squared a))
             (let [theta (Math/acos (m/clamp-normalized (g/dot p q)))]
               (g/transform-vector (g/rotate-around-axis M44 (g/normalize a) theta) (norms ii)))
             (norms ii))]
    [n (g/cross q n)]))

(defn compute-first-frame
  [t]
  (let [t' (g/abs t)
        i  (if (< (t' 0) (t' 1)) 0 1)
        i  (if (< (t' 2) (t' i)) 2 i)
        n  (g/cross t (g/normalize (g/cross t (assoc V3 i 1.0))))]
    [n (g/cross t n)]))

(defn compute-frames
  [points]
  (let [tangents (compute-tangents points)
        [n b]    (compute-first-frame (first tangents))
        num      (count tangents)]
    (loop [norms [n], bnorms [b], i 1]
      (if (< i num)
        (let [[n b] (compute-frame tangents norms bnorms i)]
          (recur (conj norms n) (conj bnorms b) (inc i)))
        [points tangents norms bnorms]))))

(defn align-frames
  [[points tangents norms bnorms]]
  (let [num   (count tangents)
        a     (first norms)
        b     (peek norms)
        theta (-> (g/dot a b) (m/clamp-normalized) (Math/acos) (/ (dec num)))
        theta (if (> (g/dot (first tangents) (g/cross a b)) 0.0) (- theta) theta)]
    (loop [norms norms, bnorms bnorms, i 1]
      (if (< i num)
        (let [t (tangents i)
              n (-> M44
                    (g/rotate-around-axis t (* theta i))
                    (g/transform-vector (norms i)))
              b (g/cross t n)]
          (recur (assoc norms i n) (assoc bnorms i b) (inc i)))
        [points tangents norms bnorms]))))

(defn sweep-point
  "Takes a path point, a PTF normal & binormal and a profile point.
  Returns profile point projected on path (point)."
  [p n b [qx qy]]
  (vec3
   (mm/madd qx (n 0) qy (b 0) (p 0))
   (mm/madd qx (n 1) qy (b 1) (p 1))
   (mm/madd qx (n 2) qy (b 2) (p 2))))

(defn sweep-profile
  [profile [points _ norms bnorms]]
  (let [frames (map vector points norms bnorms)
        tx     (fn [[p n b]] (mapv #(sweep-point p n b %) profile))
        frame0 (tx (first frames))]
    (->> (next frames) ;; TODO transducer
         (reduce
          (fn [[faces prev] frame]
            (let [curr  (tx frame)
                  curr  (conj curr (first curr))
                  faces (->> (mapcat
                              (fn [a b] [(vector (a 0) (a 1) (b 1) (b 0))])
                              (d/successive-nth 2 prev)
                              (d/successive-nth 2 curr))
                             (concat faces))]
              [faces curr]))
          [nil (conj frame0 (first frame0))])
         (first))))

(defn sweep-mesh
  [points profile & [{:keys [mesh align?]}]]
  (let [frames (compute-frames points)
        frames (if align? (align-frames frames) frames)]
    (->> frames
         (sweep-profile profile)
         (g/into (or mesh (bm/basic-mesh))))))

(defn sweep-strand
  [[p _ n b] r theta delta profile]
  (-> (mapv
       #(->> (vec2 r (mm/madd % delta theta))
             (g/as-cartesian)
             (sweep-point (p %) (n %) (b %)))
       (range (count p)))
      (sweep-mesh profile {:align? true})))

(defn sweep-strands
  [base r strands twists profile]
  (let [delta (/ (* twists TWO_PI) (dec (count (first base))))]
    (->> (m/norm-range strands)
         (butlast)
         (#?(:clj pmap :cljs map) #(sweep-strand base r (* % TWO_PI) delta profile)))))

(defn sweep-strand-mesh
  [base r strands twists profile & [{:as opts}]]
  (->> (sweep-strands base r strands twists profile)
       (reduce g/into (or (:mesh opts) (bm/basic-mesh)))))
