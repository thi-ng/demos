(defproject ex03 "0.1.0-SNAPSHOT"
  :description  "FIXME: write description"
  :url          "http://example.com/FIXME"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [http-kit "2.1.16"]
                 [compojure "1.5.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring/ring-devel "1.5.0-RC1"]
                 [hiccup "1.0.5"]
                 [thi.ng/strf "0.2.2"]
                 [thi.ng/validate "0.1.3"]
                 [environ "1.0.3"]]

  :ring         {:handler ex03.core/app}
  :main         ex03.core

  :profiles     {:dev
                 {:global-vars   {*warn-on-reflection* true}
                  :jvm-opts      ^:replace []
                  :repl-options  {:init-ns ex03.core}}

                 :uberjar
                 {:aot :all}})
