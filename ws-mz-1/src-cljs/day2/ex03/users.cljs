(ns day2.ex03.users
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require
   [day2.ex03.state :as state]
   [day2.ex03.router :as router]
   [reagent.core :as reagent]))

(defn input-field
  "Text input field component with optional input restriction fn.
  If fn is given, it must accept old input and new input vals as
  arguments (see add-user-form example below)."
  ([val]
   (input-field val (fn [old new] new)))
  ([val edit]
   [:input
    {:type "text"
     :value @val
     :on-change #(swap! val edit (-> % .-target .-value))}]))

(defn add-user-form
  "Sample form component to add new user and demonstrate use of
  above input-field component. ID field is restricted to numeric values.
  When 'add' button is clicked, we call state/add-user to add it to the
  user list stored in the global app state. This component uses
  local state to keep the temporary input values."
  []
  (let [id   (reagent/atom "")
        name (reagent/atom "")]
    (fn []
      [:div
       [:h3 "Add User"]
       [:p
        [:label "ID"]
        ;; restrict input to numbers (using regexp)
        [input-field id (fn [old new] (if (re-find #"^\d*$" new) new old))]]
       [:p
        [:label "Name"]
        [input-field name]]
       [:p
        [:label ""]
        [:button {:on-click #(state/add-user @id @name)} "Add"]]])))

(defn user-list
  "This component displays a table of registered users stored in the
  global app state. Also includes the add-user-form. Since we create
  a reaction for the :users key in the app state, whenever a new user
  is added, this component will automatically update."
  [route]
  (let [users (reaction (:users @state/app))
        rspec (router/route-for-id (:routes @state/app) :user-profile)]
    (fn [route]
      [:div
       [:h1 "Users"]
       [:table
        [:tbody
         [:tr [:th "ID"] [:th "Name"]]
         (map-indexed
          (fn [i {:keys [id name] :as u}]
            [:tr {:key (str "user" i)}
             [:td id]
             [:td [:a {:href     (router/format-route rspec u)
                       :on-click router/virtual-link}
                   name]]])
          (sort-by :name @users))]]
       [add-user-form]])))

(defn profile
  "User profile component, receives user ID as route param, looks up user
  details from app state."
  [route]
  (let [id (get-in route [:params :id])
        user (state/user-for-id id)]
    [:div
     [:h1 "User: " (:name user)]]))
