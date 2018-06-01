(ns thi.ng.geom.core.subdiv
  #?(:cljs
     (:require-macros
      [thi.ng.math.macros :as mm]))
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   #?(:clj [thi.ng.math.macros :as mm])))

(defn subdiv-kernel3
  [u v [a b c]]
  [(->> (g/* c (u 2)) (g/madd b (u 1)) (g/madd a (u 0)))
   (->> (g/* c (v 2)) (g/madd b (v 1)) (g/madd a (v 0)))])

(defn subdiv-kernel5
  [u v [a b c d e]]
  [(->> (g/* e (u 4)) (g/madd d (u 3)) (g/madd c (u 2)) (g/madd b (u 1)) (g/madd a (u 0)))
   (->> (g/* e (v 4)) (g/madd d (v 3)) (g/madd c (v 2)) (g/madd b (v 1)) (g/madd a (v 0)))])

(defn subdivide-closed
  ([scheme points]
     (subdivide-closed (:fn scheme) (:coeff scheme) points))
  ([f [u v] points]
     (let [n  (count u)
           n2 (int (/ n 2))]
       (->> (concat (take-last n2 points) points (take n2 points))
            (partition n 1)
            (mapcat #(f u v %))))))

(def schemes
  {:chaikin      {:fn subdiv-kernel3 :coeff [[0.25 0.75 0] [0 0.75 0.25]]}
   :cubic-bezier {:fn subdiv-kernel3 :coeff [[0.125 0.75 0.125] [0 0.5 0.5]]}})
