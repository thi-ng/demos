(ns thi.ng.geom.svg.test.core
  #?(:cljs
     (:require-macros
      [cemerick.cljs.test :refer (is deftest with-test run-tests testing)]))
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :refer [vec2 vec3]]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.math.core :as m]
   #?(:clj
      [clojure.test :refer :all]
      :cljs
      [cemerick.cljs.test])))




(deftest test-main
  (is true "is true"))
