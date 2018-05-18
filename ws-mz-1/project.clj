(defproject ws-mz-1 "0.1.0-SNAPSHOT"
  :description  "FIXME: write description"
  :url          "http://example.com/FIXME"
  :license      {:name "Apache Software License 2.0"
                 :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [org.clojure/data.csv "0.1.3"]
                 [thi.ng/geom "0.0.1158-SNAPSHOT"]
                 [thi.ng/validate "0.1.3"]
                 [reagent "0.6.0-alpha2"]]

  :plugins      [[lein-figwheel "0.5.0-6"]
                 [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id           "dev"
                :source-paths ["src-cljs"]
                :figwheel     true
                :compiler     {:main                 day2.ex03.core
                               :asset-path           "js/compiled/out"
                               :output-to            "resources/public/js/compiled/app.js"
                               :output-dir           "resources/public/js/compiled/out"
                               :source-map-timestamp true}}
               {:id           "min"
                :source-paths ["src-cljs"]
                :compiler     {:output-to     "resources/public/js/compiled/app.js"
                               :optimizations :advanced}}]}

  :figwheel {:css-dirs ["resources/public/css"]})
