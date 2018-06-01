(ns day2.ex03.nav
  (:require
   [day2.ex03.state :as state]
   [day2.ex03.router :as router]))

(defn nav-bar
  [routes]
  [:nav
   (->> routes
        (filter :nav?)
        (map-indexed
         (fn [i {:keys [label] :as route}]
           [:a {:key (str "nav" i)
                :href (router/format-route route {})
                :on-click router/virtual-link}
            label])))])

