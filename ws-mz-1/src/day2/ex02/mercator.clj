(ns day2.ex02.mercator
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :refer [vec2]]
   [thi.ng.math.core :as m]))

;; https://en.wikipedia.org/wiki/Mercator_projection

(defn lat-log
  [lat] (Math/log (Math/tan (+ (/ (m/radians lat) 2) m/QUARTER_PI))))

(defn mercator-in-rect
  [[lon lat] [left right top bottom] w h]
  (let [lon              (m/radians lon)
        left             (m/radians left)
        [lat top bottom] (map lat-log [lat top bottom])]
    (vec2
     (* w (/ (- lon left) (- (m/radians right) left)))
     (* h (/ (- lat top) (- bottom top))))))
