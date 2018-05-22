(defproject thi.ng/geom-core "0.0.921"
  :description  "thi.ng geometry kit - core module"
  :url          "https://github.com/thi-ng/geom"
  :license      {:name "Apache Software License"
                 :url  "http://www.apache.org/licenses/LICENSE-2.0"
                 :distribution :repo}
  :scm          {:name "git"
                 :url  "https://github.com/thi-ng/geom"}

  :min-lein-version "2.4.0"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [thi.ng/xerror "0.1.0"]
                 [thi.ng/math "0.1.4"]
                 [thi.ng/dstruct "0.1.2"]]

  :perforate {:environments [{:namespaces [thi.ng.geom.bench.core.vector]}]}

  :profiles     {:dev {:dependencies [[perforate-x "0.1.0"]]
                       :plugins      [[lein-cljsbuild "1.1.1"]
                                      [com.cemerick/clojurescript.test "0.3.3"]
                                      [perforate "0.3.4"]
                                      [lein-npm "0.5.0"]]
                       :node-dependencies [[benchmark "1.0.0"]]
                       :global-vars {*warn-on-reflection* true}
                       :jvm-opts ^:replace []
                       :aliases {"cleantest" ["do" "clean," "test," "cljsbuild" "test"]}}}

  :cljsbuild    {:builds [{:id "simple"
                           :source-paths ["src" "test"]
                           :compiler {:output-to "target/geom-core-0.0.921.js"
                                      :optimizations :whitespace
                                      :pretty-print true}}
                          {:id "bench"
                           :source-paths ["src" "test" "benchmarks"]
                           :notify-command ["node" "target/cljs/benchmark.js"]
                           :compiler {:target :nodejs
                                      :output-to "target/cljs/benchmark.js"
                                      :optimizations :simple
                                      :pretty-print true}}]
                 :test-commands {"unit-tests" ["phantomjs" :runner "target/geom-core-0.0.921.js"]}}

  :pom-addition [:developers [:developer
                              [:name "Karsten Schmidt"]
                              [:url "http://postspectacular.com"]
                              [:timezone "0"]]])
