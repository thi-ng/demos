(ns ws-ldn-2.components.dropdown)

(defn dropdown
  "Dropdown component. Takes a react component key, currently selected
  value, on-change handler and a map of menu items, where keys are
  used as the <option> items' values. The map's values are expected to
  be maps themselves and need to have at least a :label key. If
  the :label is missing the item's key is used as label."
  [key sel on-change opts]
  [:select.form-control
   {:key key :defaultValue sel :on-change on-change}
   (map
    (fn [[id val]]
      [:option {:key (str key "-" id) :value (name id)} (or (:label val) (name id))])
    opts)])
