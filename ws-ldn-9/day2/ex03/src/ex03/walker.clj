(ns ex03.walker
  (:require
   [clojure.xml :as xml]
   [clojure.walk :as walk :refer [prewalk]]))

(xml/parse "<foo id=\"23\"><bar>42</bar></foo>")
(spit "foo.xml" "<foo id=\"23\"><bar>42</bar></foo>")
(xml/parse "foo.xml")
(use 'clojure.pprint)
(pprint (xml/parse "foo.xml"))
(xml-seq (xml/parse "foo.xml"))
(spit "foo.xml" "<foo id=\"23\"><bar>42</bar><bar>66</bar></foo>")
(xml-seq (xml/parse "foo.xml"))
(filter #(= :bar (:tag %)) (xml-seq (xml/parse "foo.xml")))

;;; walk

(def data
  [:div [:p [:img {:src "foo.jpg" :alt "guess"}] [:caption "caption"]]])

(postwalk-demo data)
(prewalk-demo data)

(prewalk (fn [x] (if (string? x) (.toUpperCase x) x)) data)

(prewalk (fn [x] (if (map? x) (dissoc x :alt) x)) data)
(prewalk (fn [x] (if (map? x) (assoc x :alt "lookee") x)) data)

(let [count (volatile! 0)]
  (prewalk (fn [x] (vswap! count inc) x) data)
  @count)
