(ns thi.ng.geom.svg.adapter
  (:require
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.types])
  #?(:clj
     (:import
      [thi.ng.geom.types Circle2 Line2 LineStrip2 Polygon2 Rect2 Triangle2])))

(extend-protocol svg/PSVGConvert

  thi.ng.geom.types.Line2
  (as-svg
    [{p :points} {:keys [__start __end] :as opts}]
    (if (or __start __end)
      (svg/line-decorated (p 0) (p 1) __start __end opts)
      (svg/line (p 0) (p 1) opts)))

  thi.ng.geom.types.Circle2
  (as-svg
    [_ opts] (svg/circle (:p _) (:r _) opts))

  thi.ng.geom.types.LineStrip2
  (as-svg
    [{:keys [points]} {:keys [__start __segment __end] :as opts}]
    (if (or __start __segment __end)
      (svg/line-strip-decorated points __start __segment __end opts)
      (svg/line-strip points opts)))

  thi.ng.geom.types.Polygon2
  (as-svg
    [_ opts] (svg/polygon (:points _) opts))

  thi.ng.geom.types.Rect2
  (as-svg
    [{:keys [p size]} opts] (svg/rect p (size 0) (size 1) opts))

  thi.ng.geom.types.Triangle2
  (as-svg
    [_ opts] (svg/polygon (:points _) opts)))

;; CLJS walk differs to clojure's impl and doesn't work for defrecords
;; hence we provide a custom version here...

(defn walk
  [inner outer form]
  (cond
    (seq? form)    (outer (doall (map inner form)))
    (vector? form) (outer (mapv inner form))
    :else          (outer form)))

(defn postwalk
  [f form] (walk (partial postwalk f) f form))

(defn all-as-svg
  [form]
  (postwalk
   (fn [x] (if (satisfies? svg/PSVGConvert x) (svg/as-svg x (meta x)) x))
   form))
(defn key-attrib-injector
  "To be used with inject-element-attribs, generates an unique :key
  attrib for each SVG element."
  [el attribs] (assoc attribs :key (name (gensym))))

(defn inject-element-attribs
  "Walks SVG DOM tree with postwalk and applies given function to each
  element node. The fn takes 2 args: the element itself and its
  attribute map. The fn's return value will be used as the new
  attribute map."
  ([root]
   (inject-element-attribs key-attrib-injector root))
  ([f root]
   (postwalk
    (fn [x]
      (if (vector? x)
        (let [y (nth x 1)]
          (if (or (nil? y) (map? y))
            (assoc x 1 (f x y))
            x))
        x))
    root)))
