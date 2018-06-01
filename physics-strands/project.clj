(defproject physics-strands "0.1.0-SNAPSHOT"
  :description  "physics-strands demo"
  :url          "https://github.com/thi-ng/demos"
  :license      {:name "Apache Software License"
                 :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [thi.ng/geom "1.0.0-RC3"]
                 [thi.ng/domus "0.1.0"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :clean-targets ^{:protect false} ["out/js"]

  :cljsbuild    {:builds [{:id "dev"
                           :source-paths ["src"]
                           :compiler {:output-to "out/js/app.js"
                                      :optimizations :whitespace
                                      :pretty-print true}}
                          {:id "prod"
                           :source-paths ["src"]
                           :compiler {:output-to "out/js/app.js"
                                      :optimizations :advanced
                                      :pretty-print false}}]})
