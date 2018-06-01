(ns ex03.core
  (:gen-class)
  (:require
   [org.httpkit.server :as http]
   [compojure.core :refer :all]
   [compojure.route :as route]
   [hiccup.core :refer [html]]
   [hiccup.page :refer [html5 include-js include-css]]
   [hiccup.element :as el]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace]]
   [ring.util.anti-forgery :refer [anti-forgery-field]]
   [ring.util.response :as resp]
   [ring.util.mime-type :refer [default-mime-types]]
   [environ.core :refer [env]]
   [thi.ng.strf.core :as f]
   [thi.ng.validate.core :as v]
   [clojure.data.json :as json]))

(defonce state (atom {}))

(def html-head
  (html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
    [:meta {:name "viewport" :content "width=device-width,initial-scale=1.0"}]
    [:meta {:name "author" :content ""}]
    [:meta {:name "description" :content ""}]
    [:meta {:name "keywords" :content ""}]
    [:title "ws-ldn-9"]
    (include-css "//fonts.googleapis.com/css?family=Roboto" "/css/main.css")]))

(defn page
  [& body]
  (html5
   {:lang "en"}
   html-head
   [:body body]))

(def coercions-types
  {:int #(f/parse-int % 10 -1)
   :float #(f/parse-float % -1)})

;; (coerce-params (:params req) {:id :int :age :int})
;; [[:id :int] [:age :int]]

(defn coerce-params
  [params coercions]
  (reduce
   (fn [acc [param type]]
     (if (coercions-types type)
       (update acc param (coercions-types type))
       acc))
   params
   coercions))

(defroutes app-routes
  (GET "/" [:as req]
       (page [:h1 "Hello world!!!"]))
  (GET ["/people/:id" :id #"\d+"] [id]
       (page [:h1 id]))
  (GET "/users/:id" [:as request]
       (let [[params err] (-> request
                              :params
                              (coerce-params {:id :int})
                              (v/validate {:id [(v/number) (v/pos)]}))]
         (if err
           {:status 400
            :body (first (:id err))}
           (-> {:status 200
                :body (json/write-str {:answer #{(:id params) "some other things"}})}
               (resp/content-type (default-mime-types "json"))))))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      (wrap-stacktrace)
      (wrap-reload)))

(defn start!
  []
  (let [port (f/parse-int (env :port) 10 3000)]
    (swap! state assoc :server (http/run-server #'app {:port port}))
    (println "server running @" port)))

(defn stop!
  []
  (when-let [server (:server @state)]
    (server :timeout 100)
    (swap! state dissoc :server))
  (Thread/sleep 1100))

(defn restart!
  []
  (stop!)
  (start!))

(defn -main [& args] (start!))
