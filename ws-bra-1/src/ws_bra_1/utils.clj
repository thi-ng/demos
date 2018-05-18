(ns ws-bra-1.utils
  (:require
   [thi.ng.math.core :as m]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat]
   [thi.ng.geom.circle :refer [circle]]
   [thi.ng.geom.polygon :refer [polygon2]]
   [thi.ng.geom.line :as l]
   [thi.ng.geom.bezier :as b]
   [thi.ng.geom.path :as path]
   [thi.ng.dstruct.core :as d]))

(defn smooth-polygon
  ([poly base amount]
   (let [c      (g/centroid poly)
         points (g/vertices poly)
         points (d/wrap-seq points [(last points)] [(first points)])]
     (->> points
          (partition 3 1)
          (map
           (fn [[p q r]]
             (-> (m/- p q)
                 (m/+ (m/- r q))
                 (m/+ (m/* (m/- q c) base))
                 (m/* amount)
                 (m/+ q))))
          (polygon2))))
  ([poly base amount iter]
   (d/iterate-n iter #(smooth-polygon % base amount) poly)))

(defn poly-as-linestrip
  [poly]
  (let [verts (g/vertices poly)]
    (l/linestrip2 (conj verts (first verts)))))

(defn shape-vertices-as-circles
  [shape r] (map (fn [v] (circle v r)) (g/vertices shape)))
