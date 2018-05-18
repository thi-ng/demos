(defproject ws-ldn-11-ex05 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [thi.ng/strf "0.2.2"]
                 [reagent "0.5.1"]]

  :plugins      [[lein-figwheel "0.5.4-3"]
                 [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :clean-targets ^{:protect false} ["resources/public/js" "target"]

  :cljsbuild {:builds
              [{:source-paths ["src"]
                :id "dev"
                :compiler {:optimizations :simple
                           :pretty-print  true
                           :output-to     "resources/public/js/main.js"
                           :modules       {:cljs-base {:output-to "resources/public/js/base.js"}
                                           :app       {:output-to "resources/public/js/app.js"
                                                       :entries #{"ex05.core"}}
                                           :worker    {:output-to "resources/public/js/worker.js"
                                                       :entries #{"worker"}
                                                       ;;:depends-on #{}
                                                       }}}}
               {:source-paths ["src"]
                :id "prod"
                :compiler {:optimizations :advanced
                           :output-to     "resources/public/js/main.js"
                           :modules       {:cljs-base {:output-to "resources/public/js/base.js"}
                                           :app       {:output-to "resources/public/js/app.js"
                                                       :entries #{"ex05.core"}}
                                           :worker    {:output-to "resources/public/js/worker.js"
                                                       :entries #{"worker"}}}}}
               ]})
