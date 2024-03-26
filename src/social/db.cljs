(ns social.db)

(def default-db
  {:route {:handler :dashboard}
   :loading {}
   :user nil
   :calendar-keys nil
   :people nil
   :people-filter ""
   :people-sort "priority"
   :person nil
   :person-debouncer 0
   :person-last-update 0})
