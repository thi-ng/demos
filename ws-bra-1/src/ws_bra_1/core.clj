(ns ws-bra-1.core
  (:require
   [ws-bra-1.utils :as utils]
   [thi.ng.math.core :as m]
   [thi.ng.math.noise :as n]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat]
   [thi.ng.geom.circle :refer [circle]]
   [thi.ng.geom.polygon :refer [polygon2]]
   [thi.ng.geom.basicmesh :refer [basic-mesh]]
   [thi.ng.geom.ptf :as ptf]
   [thi.ng.geom.line :as l]
   [thi.ng.geom.bezier :as b]
   [thi.ng.geom.path :as path]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.geom.svg.adapter :as svgadapt]
   [thi.ng.dstruct.core :as d]
   [clojure.java.io :as io]
   [thi.ng.geom.mesh.io :as mio]))

(def inner-radius 30)
(def inner-offset (vec2 0 0))
(def inset-depth 15)
(def material-thick 2.95)
(def ring-distance 250)
(def rod-length 350)
(def rod-width 15)


(defn save-as-svg
  [path {:keys [width height body]}]
  (->> body
       (svgadapt/all-as-svg)
       (svg/svg {:width (or width 600) :height (or height 400)})
       (svg/serialize)
       (spit path)))

#_(save-as-svg
   "foo.svg"
   {:body (-> (circle [300 200] 100)
              (g/as-polygon 5)
              (utils/poly-as-linestrip)
              (g/sample-uniform 20000 false)
              (polygon2)
              (utils/smooth-polygon 0.125 0.25 8)
              (utils/shape-vertices-as-circles 2))})


(def curve-points
  [[50 10] [40 20] [20 25] [35 35]
   [60 30] [65 20] [80 15] [55 23]
   [45 40] [50 35]])

(defn make-rings-along-curve
  "Takes sequence of numbers and converts it into
  sequence of 3d ring meshes."
  []
  (map-indexed
   (fn [z [x radius]]
     (let [c (circle [x 0] radius)
           e (g/extrude c {:res 80 :depth 1 :mesh (basic-mesh)})
           e (g/translate e [0 0 (* z 10)])]
       e))
   curve-points))

;; meshlab.sf.net

#_(with-open [out (io/output-stream "rings.stl")]
    (mio/write-stl
     (mio/wrapped-output-stream out)
     (g/tessellate (reduce g/into (make-rings-along-curve)))))


(defn make-agent
  []
  {:pos    [0 0]
   :dir    (g/as-cartesian
            (v/vec2 1 (m/radians (m/random -45 45))))
   :speed  (m/random 1 5)
   :smooth (m/random 0.05 0.25) ;; percentage
   })


(defn make-inset
  [radius theta]
  (let [p (g/as-cartesian (vec2 radius (m/radians theta)))
        n (m/normalize (g/normal p) material-thick)
        m (m/normalize p inset-depth)
        a (m/- p n)
        d (m/+ p n)
        b (m/+ a m)
        c (m/+ d m)]
    [a b c d]))

(def inner-circle
  (polygon2 (mapcat #(make-inset inner-radius %) [0 120 240])))

(defn ring
  [i r]
  [:g {:fill      "none"
       :stroke    "black"
       :transform (str "translate(" (* i ring-distance) "," r ")")}
   (circle r)
   (g/translate inner-circle inner-offset)])

(defn ring-cluster
  [path rings]
  (save-as-svg
   path
   {:width 1000
    :body (map-indexed ring rings)}))

(defn make-rod
  [num-rings]
  (let [[a b c d] (make-inset rod-width 0)
        inset  [b a d c]
        delta  (/ rod-length (inc num-rings))
        insets (mapcat
                (fn [i]
                  (let [offset (* (inc i) delta)]
                    (map #(m/+ % 0 offset) inset)))
                (range num-rings))]
    (-> (into [[0 0] [(+ rod-width inset-depth) 0]] insets)
        (conj [(+ rod-width inset-depth) rod-length] [0 rod-length])
        (polygon2))))

;; (require 'ws-bra-1.core :reload)
;; (ring-cluster "rings.svg" [100 200 75 80])
