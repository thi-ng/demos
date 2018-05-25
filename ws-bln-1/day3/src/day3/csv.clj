(ns day3.csv
  (:require
    [clojure.java.io :as io]
    [clojure.data.csv :as csv]
    [clojure.string :as str]
    [thi.ng.strf.core :as f]
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.vector :refer [vec2]]
    [thi.ng.geom.viz.core :as viz]
    [thi.ng.geom.svg.core :as svg]
    ))

(defn sanitize
  "Takes a string and replaces non-word chars with dashes."
  [s]
  (str/lower-case (str/replace s #"[^\w]+" "-")))

(defn load-csv-resource
  "Takes CSV source (path, URI or stream) and returns parsed CSV as vector of rows"
  [path]
  (let [data (-> path
                 (io/reader)
                 (csv/read-csv :separator \,))]
    (println (count data) "rows loaded")
    data))

(defn build-column-index
  "Takes a set of columns to be indexed and first row of CSV (column headers).
  Returns map of {id col-name, ...} for all matched columns."
  [wanted-columns csv-columns]
  (->> csv-columns
       (map (fn [i x] [i x]) (range))
       (filter #(wanted-columns (second %)))
       (into {})))

(defn transform-csv-row
  "Generic CSV row transformer. Takes a column index map as returned
  by build-column-index and single CSV row vector, returns map of
  desired columns with empty cols removed. Column names are also
  turned into keywords."
  [col-idx keep-cols row]
  (->> row
       (map-indexed (fn [i x] [i x]))    ;; add col # to each value
       (filter (fn [[i]] (keep-cols i))) ;; only keep indexed columns
       (map (fn [[i x]] [(keyword (sanitize (col-idx i))) x])) ;; form pairs of [:col-name val]
       (remove (fn [x] (empty? (second x)))) ;; remove all cols w/ empty vals
       (into {})))                           ;; turn into hash-map

(defn coerce-row-fields
  "Takes a row map and set of column ids, returns updated map with
   column values coerced to floats."
  [cols row]
  (reduce
    (fn [acc id] (update acc id f/parse-float))
    row cols))

(defn load-data
  [path cols coerce-cols]
  (let [rows      (load-csv-resource path)
        col-idx   (build-column-index cols (first rows))
        keep-cols (set (keys col-idx))
        xf        (comp
                    (map #(transform-csv-row col-idx keep-cols %))
                    (map (partial coerce-row-fields coerce-cols)))
        results   (into [] xf (rest rows))]
    results))

(defn column-value-range
  [rows id]
  {:pre [(pos? (count rows)) (keyword? id)]
   ; :post [(> (:avg %) 0.1)]
   }
  {:min (transduce (map id) min Double/MAX_VALUE rows)
   :max (transduce (map id) max (- Double/MAX_VALUE) rows)
   :avg (/ (transduce (map id) + 0 rows) (count rows))})

(defn calc-stats
  [rows cols]
  (reduce
     (fn [acc id] (assoc acc id (column-value-range rows id)))
     {} cols))

(defn load-temperatures
  []
  (let [rows (load-data
               (io/resource "data/global-temp-annual.csv")
               #{"Year" "Land" "Land and Ocean"}
               #{:year :land :land-and-ocean})
        stats (calc-stats rows #{:year :land :land-and-ocean})]
    {:rows rows
     :stats stats}))

(defn make-viz-spec
  [{:keys [rows stats]} colid]
  {:x-axis (viz/linear-axis
            {:domain [(get-in stats [:year :min]) (get-in stats [:year :max])]
             :range  [50 580]
             :major  10
             :minor  5
             :pos    250
             :label  (viz/default-svg-label int)})
   :y-axis (viz/linear-axis
            {:domain     [(get-in stats [colid :min]) (get-in stats [colid :max])]
             :range      [250 20]
             :major      0.1
             :minor      0.05
             :pos        50
             :label-dist 15
             :label-style {:text-anchor "end"}})
   :grid   {:attribs {:stroke "#ccc"}}
   :data   [{:values  (map (juxt :year :land) rows)
             :attribs {:fill "none" :stroke "#0af"}
             :layout  viz/svg-bar-plot}]})

(comment
  (->> (viz/svg-plot2d-cartesian (make-viz-spec temps :land))
    (svg/svg {:width 600 :height 320})
    (svg/serialize)
    (spit "land-temps.svg"))
  )