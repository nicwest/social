(ns social.handler
  (:require [compojure.core :refer [defroutes GET POST PATCH]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults] ]
            [jumblerg.middleware.cors :refer [wrap-cors]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [nano-id.core :refer [nano-id]]
            [java-time.api :as jt]
            [faker.name :as names]
            [clojure.string :refer [includes? lower-case]]
            ))

(defn login
  [request]
  (let [data (:body request)
        email (get data "email")
        password (get data "password")]
    (if (and (= email "bob@example.com")
             (= password "pass"))
      {:status 201
       :body {:token (nano-id)
              :name "Bob"
              :id "bob"}}
      {:status 403
       :body {:error "invalid credentials"}})))

(def calendar-keys-data
  [{:title "default"
    :id (nano-id)
    :created-at (jt/format :iso-offset-date-time (jt/offset-date-time))
    :permission ["read" "create" "update" "delete"]}
   {:title "Drinking buddies"
    :id (nano-id)
    :created-at (jt/format :iso-offset-date-time (jt/offset-date-time))
    :permissions ["read"]}
   {:title "Partner and kids"
    :id (nano-id)
    :created-at (jt/format :iso-offset-date-time (jt/offset-date-time))
    :permissions ["read" "delete"]}])

(defn calendar-keys
  [request]
  {:status 200
   :body {:keys calendar-keys-data}})

(def tags
  ["family" "friends" "work-friends" "uni" "school" "cars" "bikes" "knitting"
   "dancing" "yoga" "art" "music" "gigs" "punk" "metal" "jazz" "blues" "chess"
   "london" "birmingham" "scotland" "france" "wales" "america" "new york"
   "cheese" "farming" "ex"])

(def cadence-periods
  {;["day" (* 60 60 24)]
   ;["week" (* 60 60 24 7)]
   ;["fortnight" (* 60 60 24 14)]
   "month" 30
   "quarter" 90
   "year" 365
   "decade" 3650})


(def iso-formatter
  (jt/formatter "YYYY-MM-dd'T'HH:mm:ss.SSSZZ"))


(defn calc-priority 
  [now period n last-seen]
  (/ (jt/time-between last-seen now :days)
     (/ period n)))

(def people-data
  (atom
    (into 
      {} 
      (let [now (jt/local-date)]
        (map 
          (fn [_] 
            (let [offset (jt/days (rand-int 30))
                  last-seen (jt/minus now offset)
                  cadence-num (inc (rand-int 2))
                  [cadence-period-human cadence-period] (rand-nth (into [] cadence-periods))
                  priority (calc-priority (jt/local-date) cadence-period cadence-num last-seen)
                  id (nano-id)]
              [id 
               {:name (str (names/first-name) " " (names/last-name))
                :id id
                :tags (into [] (random-sample 0.1 tags))
                :last-seen last-seen
                :cadence-num cadence-num
                :cadence-period cadence-period
                :cadence-period-human cadence-period-human
                :priority (float priority)
                :notes "foo bar"}]))
          (range 100))))))


(defn update-priority
  [now {:keys [cadence-num
               cadence-period
               last-seen]
    :as person}]
    (assoc person :priority (calc-priority now cadence-period cadence-num last-seen)))

(defn person->json
  [p]
  (assoc p :last-seen (jt/format :iso-date (:last-seen p))))

(defn json->person
  [p]
  (->> (assoc p
         "last-seen" (jt/local-date (get p "last-seen"))
         "cadence-period" (get cadence-periods (get p "cadence-period-human")))
      (map (fn [[k v]] [(keyword k) v]))
      (into {})))

(defn people
  [request]
  (let [q (get-in request [:query-params "q"])
        q (if q (lower-case q) q)
        s (get-in request [:query-params "s"])
        s (if s (keyword (lower-case s)) :priority)]
  {:status 200
   :body
   {:people
    (->> @people-data
         (map val)
         (map #(update-priority (jt/local-date) %))
         (filter #(or (includes? (lower-case (:name %)) q)
                      (includes? (:tags %) q)))
         (sort #(> (s %1) (s %2)))
         (take 20)
         (map person->json))}}))

(defn person
  [request]
  (let [id (get-in request [:params :id])
        p (get @people-data id)]
    {:status 200
     :body (->> p
               (update-priority (jt/local-date))
               (person->json))}))

(defn create-person
  [_]
  (let [id (nano-id)
        p {:name ""
           :id id
           :tags ""
           :last-seen nil
           :cadence-date nil
           :cadence-num 1
           :cadence-period "month"
           :priority 0
           :notes "foo bar"}]
    (swap! people-data assoc id p)
    {:status 201 :body {:person p}}))

(defn update-person
  [request]
  (let [id (get-in request [:params :id])
        orig (get @people-data id)
        data (json->person (get-in request [:body "person"]))
        p (merge orig data)
        p (update-priority (jt/local-date) p)]
    (swap! people-data assoc id p)
    {:status 202 :body {:person (person->json p)}}))

(defroutes app-routes
  (GET "/" [] {:status 200 :body {:pew "pewnpwpw"}})
  (GET "/calendar/keys" [id] calendar-keys )
  (GET "/people/:id" [id] person)
  (PATCH "/people/:id" [id] update-person)
  (GET "/people" [] people)
  (POST "/people" [] create-person)
  (POST "/login" [] login)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-cors #".*")
      (wrap-json-response)
      (wrap-json-body)
      (wrap-defaults api-defaults)))
