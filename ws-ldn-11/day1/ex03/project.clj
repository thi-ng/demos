(defproject ws-ldn-11-ex03 "0.1.0-SNAPSHOT"
  :description  "thi.ng Clojurescript workshop WS-LDN-8"
  :url          "http://workshop.thi.ng"

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.51"]
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]
                 [thi.ng/geom "0.0.1178-SNAPSHOT"]
                 [thi.ng/domus "0.3.0-SNAPSHOT"]]

  :plugins      [[lein-figwheel "0.5.4-3"]
                 [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :compiler {:asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/app.js"
                           :output-dir "resources/public/js/compiled/out"
                           :optimizations :simple
                           :pretty-print true}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/app.js"
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {:css-dirs ["resources/public/css"]})
