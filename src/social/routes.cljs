(ns social.routes
  (:require
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [re-frame.core :as re-frame]
   [social.events :as events]))

(def routes
  (atom 
    ["/" {""      :dashboard
          "people" {"" :people
                    ["/" :id] :person}
          "me" :me
          "calendar" :calendar
          "messages" :messages
          "logout" :logout
          "login" :login}]))


(defonce history
  (pushy/pushy #(re-frame/dispatch [::events/set-route %])
               #(bidi/match-route @routes %)))

(defn start!
  []
  (pushy/start! history))

(re-frame/reg-fx
  :navigate
  (fn [args]
    (pushy/set-token! history (apply bidi/path-for (into [@routes] args)))))
