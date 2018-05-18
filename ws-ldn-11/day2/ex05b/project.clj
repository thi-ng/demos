(defproject ws-ldn-11-ex05b "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [thi.ng/geom "0.0.1178-SNAPSHOT"]
                 [reagent "0.5.1"]]

  :plugins      [[lein-figwheel "0.5.4-3"]
                 [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled/" "target"]

  :cljsbuild {:builds
              [{:id           "fig-gl"
                :source-paths ["src"]
                :figwheel     true
                :compiler     {:main                 ex05b.core
                               :asset-path           "js/compiled/out"
                               :output-to            "resources/public/js/compiled/app.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :source-map-timestamp true}}
               {:source-paths ["src"]
                :id           "dev"
                :compiler     {:optimizations :simple
                               :pretty-print  true
                               :output-to     "resources/public/js/dummy.js"
                               :modules       {:cljs-base  {:output-to "resources/public/js/base.js"}
                                               :app        {:output-to "resources/public/js/compiled/app.js"
                                                            :entries   #{"ex05b.core"}}
                                               :meshworker {:output-to  "resources/public/js/meshworker.js"
                                                            :entries    #{"meshworker"}}}}}
               {:source-paths ["src"]
                :id           "min"
                :compiler     {:optimizations :advanced
                               :pretty-print  false
                               :output-to     "resources/public/js/dummy.js"
                               :modules       {:cljs-base  {:output-to "resources/public/js/base.js"}
                                               :app        {:output-to "resources/public/js/compiled/app.js"
                                                            :entries   #{"ex05b.core"}}
                                               :meshworker {:output-to  "resources/public/js/meshworker.js"
                                                            :entries    #{"meshworker"}}}}}]})
