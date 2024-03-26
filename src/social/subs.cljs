(ns social.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::route
 (fn [db _]
   (:route db)))

(re-frame/reg-sub
  ::loading
  (fn [db]
    (:loading db)))

(re-frame/reg-sub
  ::calendar-keys
  (fn [db]
    (:calendar-keys db)))

(re-frame/reg-sub
  ::user
  (fn [db]
    (:user db)))

(re-frame/reg-sub
  ::logged-in?
  (fn [db]
    (not (not (get-in db [:user :id])))))

(re-frame/reg-sub
  ::people
  (fn [db]
    (:people db)))

(re-frame/reg-sub
  ::person
  (fn [db]
    (:person db)))

(re-frame/reg-sub
  ::people-filter
  (fn [db]
    (:people-filter db)))


(re-frame/reg-sub
  ::person-save-status
  (fn [db]
    (cond
      (get-in db [:loading :person-update]) "saving..."
      (not= (:person-last-update db) (:person-debouncer db)) "unsaved changes"
      :else "saved!")))
