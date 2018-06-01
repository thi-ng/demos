(defproject thi.ng/geom-types "0.0.921"
  :description  "thi.ng geometry kit - types module"
  :url          "https://github.com/thi-ng/geom"
  :license      {:name "Apache Software License"
                 :url "http://www.apache.org/licenses/LICENSE-2.0"
                 :distribution :repo}
  :scm          {:name "git"
                 :url  "https://github.com/thi-ng/geom"}

  :min-lein-version "2.4.0"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [thi.ng/geom-core "0.0.921"]
                 [thi.ng/xerror "0.1.0"]]

  :profiles     {:dev {:dependencies [[criterium "0.4.3"]]
                       :plugins      [[lein-cljsbuild "1.1.1"]
                                      [com.cemerick/clojurescript.test "0.3.3"]]
                       :global-vars {*warn-on-reflection* true}
                       :jvm-opts ^:replace []
                       :aliases {"cleantest" ["do" "clean," "test," "cljsbuild" "test"]}}}

  :cljsbuild    {:builds [{:source-paths ["src" "test"]
                           :id "simple"
                           :compiler {:output-to "target/geom-types-0.0.921.js"
                                      :optimizations :whitespace
                                      :pretty-print true}}]
                 :test-commands {"unit-tests" ["phantomjs" :runner "target/geom-types-0.0.921.js"]}}

  :pom-addition [:developers [:developer
                              [:name "Karsten Schmidt"]
                              [:url "http://postspectacular.com"]
                              [:timezone "0"]]])
