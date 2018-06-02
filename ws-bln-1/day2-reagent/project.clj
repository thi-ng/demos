(defproject thi.ng/ws-bln-1 "0.1.0-SNAPSHOT"
  :description   "thi.ng x Studio NAND Berlin workshop #1"
  :url           "http://thi.ng/ws-bln-1"
  :license       {:name "Apache Software License 2.0"
                  :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies  [[org.clojure/clojure "1.9.0"]
                  [org.clojure/clojurescript "1.10.238"]
                  ; [org.clojure/core.async "0.4.474"]
                  [thi.ng/geom "1.0.0-RC3"]
                  [reagent "0.5.1"]
                  [cljs-log "0.2.2"]]

  :plugins       [[lein-figwheel "0.5.16"]
                  [lein-cljsbuild "1.1.7"]
                  [lein-environ "1.0.0"]]

  :clean-targets ^{:protect false} ["target" "resources/public/js"]

  :profiles      {:prod {:env {:log-level 4}}}

  :cljsbuild      {
  :builds [ { :id "dev"
              :source-paths ["src"]
              :figwheel true
              :compiler {  :main "day2.core"
                           :asset-path "js/out"
                           :output-to "resources/public/js/app.js"
                           :output-dir "resources/public/js/out" } } ]
}

  :figwheel      {:http-server-root "public"         ;; default and assumes "resources"
                  :server-port 3449                  ;; default
                  :css-dirs ["resources/public/css"] ;; watch and update CSS
                  })
