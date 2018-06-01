(ns thi.ng.geom.sphere
  #?(:cljs (:require-macros [thi.ng.math.macros :as mm]))
  (:require
   [thi.ng.geom.core :as g :refer [*resolution*]]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.core.vector :as v :refer [vec3]]
   [thi.ng.geom.core.intersect :as isec]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.geom.types :as types]
   [thi.ng.xerror.core :as err]
   [thi.ng.math.core :as m :refer [TWO_PI PI *eps*]]
   #?(:clj [thi.ng.math.macros :as mm]))
  #?(:clj
     (:import
      [thi.ng.geom.types AABB Sphere])))

(defn sphere
  ([] (thi.ng.geom.types.Sphere. (vec3) 1.0))
  ([r] (thi.ng.geom.types.Sphere. (vec3) #?(:clj (double r) :cljs r)))
  ([p r] (thi.ng.geom.types.Sphere. (vec3 p) #?(:clj (double r) :cljs r))))




(extend-type thi.ng.geom.types.Sphere
g/PArea
(area
 [{r :r}] (* 4.0 PI r r))
g/PBoundary
(contains-point?
 [{p :p r :r} q] (<= (g/dist-squared p q) (* r r)))
g/PBounds
(bounds
 [_] (thi.ng.geom.types.AABB. (g/- (:p _) (:r _)) (vec3 (* 2 (:r _)))))
(width  [_] (* 2.0 (:r _)))
(height [_] (* 2.0 (:r _)))
(depth  [_] (* 2.0 (:r _)))
g/PBoundingSphere
(bounding-sphere [_] _)
g/PCenter
(center
 ([_] (thi.ng.geom.types.Sphere. (vec3) (:r _)))
 ([_ p] (thi.ng.geom.types.Sphere. (vec3 p) (:r _))))
(centroid [_] (:p _))
g/PClassify
(classify-point
 [{p :p r :r} q]
 (m/signum (- (* r r) (g/dist-squared p q)) *eps*))


g/PIntersect
(intersect-ray
 [{p :p r :r} ray]
 (let [[rp dir] (if (map? ray) [(:p ray) (:dir ray)] ray)]
   (isec/intersect-ray-sphere? rp dir p r)))
(intersect-shape
 [_ s]
 (cond
  (instance? thi.ng.geom.types.AABB s)
  (isec/intersect-aabb-sphere? s _)
  (instance? thi.ng.geom.types.Sphere s)
  (isec/intersect-sphere-sphere? _ s)
  (instance? thi.ng.geom.types.Plane s)
  (isec/intersect-plane-sphere? (:n s) (:w s) (:p _) (:r _))
  :default (err/type-error! "Sphere" s)))
g/PMeshConvert
(as-mesh
 ([_] (g/as-mesh _ {}))
 ([{[x y z] :p r :r} {:keys [mesh res slices stacks] :or {res *resolution*}}]
    (let [slices (or slices res), stacks (or stacks res)
          range-u (range slices), range-v (range stacks)]
      (->> (for [i range-u, j range-v
                 :let [u (/ i slices)
                       v (/ j stacks)
                       u1 (/ (inc i) slices)
                       v1 (/ (inc j) stacks)
                       verts [[u v]]
                       verts (if (pos? j) (conj verts [u1 v]) verts)
                       verts (if (< j (dec stacks)) (conj verts [u1 v1]) verts)]]
             (conj verts [u v1]))
           ;; TODO transduce
           (map
            (fn [verts]
              (map
               (fn [[u v]]
                 (let [theta (* TWO_PI u) ;; FIXME optimize trig
                       phi (* PI v)
                       st (Math/sin theta) ct (Math/cos theta)
                       sp (Math/sin phi) cp (Math/cos phi)]
                   (vec3
                    (+ (mm/mul ct sp r) x)
                    (mm/madd cp r y)
                    (+ (mm/mul st sp r) z))))
               verts)))
           (g/into (or mesh (bm/basic-mesh)))))))
g/PProximity
(closest-point
 [{p :p r :r} q]
 (g/+! (g/normalize (g/- q p) r) p))
g/PSample
(random-point-inside
 [_]
 (g/+ (:p _) (v/randvec3 (m/random (:r _)))))
(random-point
 [_]
 (g/+ (:p _) (v/randvec3 (:r _))))
g/PTessellate
(tessellate
 [_] (g/tessellate _ {}))
(tessellate
 [_ opts] (g/tessellate (g/as-mesh _ opts)))
g/PRotate
(rotate
 [_ theta] (thi.ng.geom.types.Sphere. (g/rotate-z (:p _) theta) (:r _)))
g/PRotate3D
(rotate-x
 [_ theta] (thi.ng.geom.types.Sphere. (g/rotate-x (:p _) theta) (:r _)))
(rotate-y
 [_ theta] (thi.ng.geom.types.Sphere. (g/rotate-y (:p _) theta) (:r _)))
(rotate-z
 [_ theta] (thi.ng.geom.types.Sphere. (g/rotate-z (:p _) theta) (:r _)))
(rotate-around-axis
 [_ axis theta]
 (thi.ng.geom.types.Sphere.
  (g/rotate-around-axis (:p _) axis theta) (:r _)))

g/PScale
(scale [_ s] (thi.ng.geom.types.Sphere. (g/* (:p _) s) (* (:r _) s)))
(scale-size [_ s] (thi.ng.geom.types.Sphere. (:p _) (* (:r _) s)))

g/PTranslate
(translate [_ t] (thi.ng.geom.types.Sphere. (g/+ (:p _) t) (:r _)))
g/PVolume
(volume [{r :r}] (mm/mul (/ 4.0 3.0) PI r r r))
)
