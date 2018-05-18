(ns day2.ex03.state
  (:require-macros
   [reagent.ratom :refer [reaction]]
   [cljs-log.core :refer [debug info warn]])
  (:require
   [thi.ng.strf.core :as f]
   [reagent.core :as reagent]))

(defonce app (reagent/atom {}))

(defn set-state!
  "Helper fn to reset app state for given key. Key can be single
  keyword or path vector."
  [key val]
  (debug key val)
  (swap! app (if (sequential? key) assoc-in assoc) key val))

(defn update-state!
  "Helper fn to update app state value for given key. Key can be single
  keyword or path vector. Behaves like clojure.core/update or update-in"
  [key f & args]
  (debug key args)
  (swap! app #(apply (if (sequential? key) update-in update) % key f args)))

(defn subscribe
  "Helper fn to create a reagent reaction for given app state key/path."
  [key]
  (if (sequential? key)
    (reaction (get-in @app key))
    (reaction (@app key))))

(defn nav-change
  [route]
  (set-state! :curr-route route))

(defn add-user
  [id name]
  (update-state! :users conj {:id (f/parse-int id 10) :name name}))

(defn user-for-id
  [id]
  (some #(if (= id (:id %)) %) (:users @app)))

(defn init-app
  [routes]
  (swap! app merge
         {:inited true
          :routes routes
          :users  [{:id 128 :name "Gary"}
                   {:id 123 :name "Adrian"}
                   {:id 512 :name "Michael"}
                   {:id 456 :name "Cameron"}
                   {:id 384 :name "Karsten"}]}))
