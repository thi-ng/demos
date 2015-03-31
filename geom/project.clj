(defproject geom-demos "0.1.0-SNAPSHOT"
  :description  "thi.ng/geom demos"
  :url          "https://github.com/thi-ng/demos"
  :license      {:name "Apache Software License"
                 :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [org.clojure/clojurescript "0.0-3117"]
                 [thi.ng/geom "0.0.777"]
                 [thi.ng/domus "0.1.0"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :clean-targets ^{:protect false} ["resources/public/js"]

  :cljsbuild    {:builds [{:id "dev"
                           :source-paths ["src"]
                           :compiler {:output-to "resources/public/js/app.js"
                                      :optimizations :whitespace
                                      :pretty-print true}}
                          {:id "prod"
                           :source-paths ["src"]
                           :compiler {:output-to "resources/public/js/app.js"
                                      :optimizations :advanced
                                      ;;:pseudo-names true
                                      ;;:pretty-print true
                                      :pretty-print false
                                      }}]
                 :test-commands {"unit-tests" ["phantomjs" :runner "resources/public/js/app.js"]}})
