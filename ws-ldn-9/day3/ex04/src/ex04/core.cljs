(ns ex04.core
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn severe]])
  (:require
   [ex04.state :as state]
   [ex04.router :as router]
   [ex04.nav :as nav]
   [ex04.home :as home]
   [ex04.users :as users]
   [ex04.canvas :as canvas]
   [reagent.core :as reagent]
   [thi.ng.strf.core :as f]
   [thi.ng.validate.core :as v]))

(enable-console-print!)

(def routes
  "Basic SPA route configuration. See router ns for further options."
  [{:id        :home
    :match     ["home"]
    :component #'home/home
    :label     "Home"
    :nav?      true}
   {:id        :user-list
    :match     ["users"]
    :component #'users/user-list
    :label     "Users"
    :nav?      true}
   {:id        :user-profile
    :match     ["users" :id]
    :validate  {:id {:coerce   #(f/parse-int % 10)
                     :validate [(v/optional (v/number))]}}
    :component #'users/profile
    :label     "User profile"}
   {:id        :canvas
    :match     ["canvas"]
    :component #'canvas/canvas
    :label     "Epileptic drawing tool"
    :nav?      true}])

(defn view-wrapper
  "Shared component wrapper for all routes, includes navbar."
  [route]
  (let [route @route]
    [:div
     [nav/nav-bar routes route]
     [(:component route) route]]))

(defn app-component
  "Application main component."
  []
  (let [route (reaction (:curr-route @state/app))]
    (fn []
      (if @route
        [view-wrapper route]
        [:div "initializing..."]))))

(defn start-router
  "Starts SPA router, called from main fn."
  []
  (router/start!
   routes
   nil
   (router/route-for-id routes :home)
   state/nav-change
   (constantly nil)))

(defn main
  "Application main entry point, initializes app state and kicks off
  React component lifecycle."
  []
  (when-not (:inited @state/app)
    (state/init-app routes))
  (start-router)
  (reagent/render-component
   [app-component]
   (.getElementById js/document "app")))

;; document.getElementById("app")
;; document.body
;;

(defn on-js-reload
  "Called each time fighweel has reloaded code."
  [] (debug :reloaded))

(main)
