(ns thi.ng.geom.mesh.polyhedra
  #?(:cljs (:require-macros [thi.ng.math.macros :as mm]))
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v :refer [vec3]]
   [thi.ng.geom.basicmesh :as bm]
   [thi.ng.geom.gmesh :as gm]
   [thi.ng.math.core :as m :refer [PHI PI SQRT2 SQRT3]]
   #?(:clj [thi.ng.math.macros :as mm])))

(defn polyhedron-mesh
 ([f] (polyhedron-mesh f 1))
 ([f scale] (g/into (bm/basic-mesh) (f scale)))
 ([f subdiv scale iter] (nth (iterate subdiv (g/into (gm/gmesh) (f scale))) iter)))
(defn tetrahedron-vertices
  [scale]
  (let [p (/ SQRT3 3.0)
        q (/ p -2.0)
        r (/ (Math/sqrt 6) 6.0), r' (- r)]
    (map #(g/scale % scale)
         [(vec3 p 0 r')
          (vec3 q -0.5 r')
          (vec3 q 0.5 r')
          (vec3 0 0 r)])))

(defn tetrahedron
  [scale]
  (let [[a b c d] (tetrahedron-vertices scale)]
    [[a b c] [a c d] [a d b] [c b d]]))
(defn octahedron-vertices
  [scale]
  (let [p (/ (* 2.0 SQRT2)), p' (- p)
        q 0.5,               q' (- q)]
    (map #(g/normalize % scale)
         [(vec3 p' 0 p)
          (vec3 p 0 p)
          (vec3 p 0 p')
          (vec3 p' 0 p')
          (vec3 0 q 0)
          (vec3 0 q' 0)])))

(defn octahedron
  [scale]
  (let [[a b c d e f] (octahedron-vertices scale)]
    [[d a e] [c d e] [b c e] [a b e]
     [d c f] [a d f] [c b f] [b a f]]))
(defn icosahedron-vertices
  [scale]
  (let [p 0.5,           p' (- p)
        q (/ (* 2 PHI)), q' (- q)]
    (map #(g/normalize % scale)
         [(vec3 0 q p')
          (vec3 q p 0)
          (vec3 q' p 0)
          (vec3 0 q p)
          (vec3 0 q' p)
          (vec3 p' 0 q)
          (vec3 p 0 q)
          (vec3 0 q' p')
          (vec3 p 0 q')
          (vec3 p' 0 q')
          (vec3 q p' 0)
          (vec3 q' p' 0)])))

(defn icosahedron
  [scale]
  (let [[a b c d e f g h i j k l] (icosahedron-vertices scale)]
    [[b a c] [c d b] [e d f] [g d e]
     [h a i] [j a h] [k e l] [l h k]
     [f c j] [j l f] [i b g] [g k i]
     [f d c] [b d g] [c a j] [i a b]
     [j h l] [k h i] [l e f] [g e k]]))
(defn dodecahedron-vertices
  [scale]
  (let [p 0.5,               p' (- p)
        q (/ 0.5 PHI),       q' (- q)
        r (* 0.5 (- 2 PHI)), r' (- r)]
    (map #(g/normalize % scale)
         [(vec3 r 0 p)
          (vec3 r' 0 p)
          (vec3 q' q q)
          (vec3 0 p r)
          (vec3 q q q)
          (vec3 q q' q)
          (vec3 0 p' r)
          (vec3 q' q' q)
          (vec3 r 0 p')
          (vec3 r' 0 p')
          (vec3 q' q' q')
          (vec3 0 p' r')
          (vec3 q q' q')
          (vec3 q q q')
          (vec3 0 p r')
          (vec3 q' q q')
          (vec3 p r 0)
          (vec3 p' r 0)
          (vec3 p' r' 0)
          (vec3 p r' 0)])))

(defn dodecahedron
  [scale]
  (let [[a b c d e f g h i j k l m n o p q r s t] (dodecahedron-vertices scale)]
    [[e d c b a] [h g f a b] [m l k j i] [p o n i j]
     [o d e q n] [d o p r c] [l g h s k] [g l m t f]
     [e a f t q] [m i n q t] [p j k s r] [h b c r s]]))
