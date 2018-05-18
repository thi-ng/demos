(ns ex04.nav
  (:require
   [ex04.state :as state]
   [ex04.router :as router]))

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

