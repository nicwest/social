(ns social.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [re-frame.core :as re-frame]
   [social.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [cljs-http.client :as http]
   [cljs-time.core :as t]
   [cljs.core.async :refer [<!]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-fx
 ::set-route
 (fn-traced 
   [{:keys [db]} [_ route]]
   (let [db (assoc db :route route)]
   (case (:handler route)
     :calendar {:db db
                :dispatch [::get-calendar-keys]}
     :people {:db db
              :dispatch [::get-people]}
     :person {:db db
              :dispatch [::get-person (get-in route [:route-params :id])]}
     :logout {:db db
              :dispatch [::logout]}
     {:db db}))))

(re-frame/reg-fx
  :http
  (fn [{:keys [method url on-success on-failure opts]}]
    (go (let [opts (if opts opts {})
              resp (<! (method url opts))
              handler (if (:success resp) on-success on-failure)]
          (re-frame/dispatch (into [] (conj handler resp)))))))


(re-frame/reg-event-fx
  ::api-error
  (fn-traced [{:keys [db]} [_ loading-key resp]]
             (println "API PROBLEMO!" resp)
             {:db (assoc-in db [:loading loading-key] false)}))

(re-frame/reg-event-fx
  ::get-calendar-keys
  (fn-traced [{:keys [db]} [_]]
   {:db (-> db
            (assoc-in [:loading :calendar-keys] true))
    :http {:method http/get
           :url (str "http://localhost:3000/calendar/keys")
           :on-success [::get-calendar-keys-success]
           :on-failure [::api-error :calendar-keys]}}))


(re-frame/reg-event-fx
  ::get-calendar-keys-success
  (fn-traced [{:keys [db]} [_ resp]]
             {:db (-> db
                      (assoc-in [:loading :calendar-keys] false)
                      (assoc :calendar-keys (:keys (:body resp))))}))

(re-frame/reg-event-fx
  ::get-people
  (fn-traced [{:keys [db]} [_]]
   {:db (-> db
            (assoc-in [:loading :people] true))
    :http {:method http/get
           :url (str "http://localhost:3000/people")
           :on-success [::get-people-success]
           :on-failure [::api-error :people]
           :opts {:query-params {:q (:people-filter db)
                                 :s (:people-sort db)}}}}))

(re-frame/reg-event-fx
  ::get-people-success
  (fn-traced [{:keys [db]} [_ resp]]
             {:db (-> db
                      (assoc-in [:loading :people] false)
                      (assoc :people (:people (:body resp))))}))

(re-frame/reg-event-fx
  ::get-person
  (fn-traced [{:keys [db]} [_ id]]
   {:db (-> db
            (assoc-in [:loading :person] true)
            (assoc :person nil))
    :http {:method http/get
           :url (str "http://localhost:3000/people/" id)
           :on-success [::get-person-success]
           :on-failure [::api-error :person]}}))


(re-frame/reg-event-fx
  ::update-person
  (fn-traced 
    [{:keys [db]} [_ k v]]
    (let [i (inc (:person-debouncer db))
          db (-> db
                 (assoc-in [:person k] v)
                 (assoc :person-debouncer i))]
    {:db db
     :dispatch-later {:ms 2000
                      :dispatch [::update-person-debounced i]}})))

(re-frame/reg-event-fx
  ::update-person-debounced
  (fn-traced
    [{:keys [db]} [_ i]]
    (when (= (:person-debouncer db) i)
      {:db (assoc-in db [:loading :person-update] true)
       :http {:method http/patch
              :url (str "http://localhost:3000/people/" (get-in db [:person :id]))
              :on-success [::update-person-success i]
              :on-failure [::api-error :person-update]
              :opts {:json-params {:person (:person db)}}}})))

(re-frame/reg-event-fx
  ::update-person-success
  (fn-traced
    [{:keys [db]} [_ i _]]
    {:db (-> db
             (assoc-in [:loading :person-update] false)
             (assoc :person-last-update i))}))

(re-frame/reg-event-fx
  ::create-person
  (fn-traced 
    [{:keys [db]} [_]]
    {:db (-> db
             (assoc-in [:loading :person] true)
             (assoc :person nil))
     :http {:method http/post
            :url "http://localhost:3000/people"
            :on-success [::create-person-success]
            :on-failure [::api-error :person]}}))

(re-frame/reg-event-fx
  ::create-person-success
  (fn-traced
    [{:keys [db]} [_ resp]]
    (let [p (get-in resp [:body :person])]
    {:db (-> db
             (assoc-in [:loading :person] false)
             (assoc :person p))
     :navigate [:person :id (:id p)]})))

(re-frame/reg-event-fx
  ::get-person-success
  (fn-traced [{:keys [db]} [_ resp]]
             {:db (-> db
                      (assoc-in [:loading :person] false)
                      (assoc :person (:body resp)))}))

(re-frame/reg-event-fx
  ::login
  (fn [{:keys [db]} [_ {:keys [email password] :as values}]]
  {:db (assoc-in db [:loading :login] true)
   :http {:method http/post
          :url "http://localhost:3000/login"
          :on-success [::login-success]
          :on-failure [::login-failure]
          :opts {:json-params 
                 {:email email
                  :password password}}}}))

(re-frame/reg-event-fx
  ::login-success
  (fn-traced [{:keys [db]} [_ resp]]
    (let [{:keys [token name id]} (:body resp)]
      {:navigate [:dashboard]
       :db (-> db 
               (assoc-in [:loading :login] false)
               (assoc :user {:id id
                             :name name
                             :token token}))})))

(re-frame/reg-event-fx
  ::login-failure
  (fn [{:keys [db]} [_ resp]]
    {:db (assoc-in db [:loading :login] false)}))

(re-frame/reg-event-fx
  ::logout
  (fn [{:keys [db]} [_ _]]
    {:db (assoc db :user nil :people nil :calendar-keys nil)
     :navigate [:login]}))

(re-frame/reg-event-fx
  ::update-people-filter
  (fn [{:keys [db]} [_ text]]
    {:db (assoc db :people-filter text)
     :dispatch-later {:ms 600 :dispatch [::get-people-debounced text]}}))

(re-frame/reg-event-fx
  ::update-people-sort
  (fn [{:keys [db]} [_ v]]
    {:db (assoc db :people-sort (name v))
     :dispatch [::get-people]}))

(re-frame/reg-event-fx
  ::get-people-debounced
  (fn [{:keys [db]} [_ text]]
    (when (= (:people-filter db) text)
      {:dispatch [::get-people]})))
