(ns day1.core
  (:require
    [hiccup.core :refer [html]]
    [hiccup.page :refer [html5]]
    [thi.ng.geom.svg.core :as svg]))

(defn export-svg
  [& body]
  (println (count body) "elements")
  (->> body
    (svg/svg {:width 200 :height 200})
    (svg/serialize)
    (spit "berlin.svg")))

(export-svg
  (svg/circle [100 100] 100 {:fill "red"})
  (svg/circle [100 100] 50 {:fill "yellow"}))