(ns social.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [social.events :as events]
   [social.routes :as routes]
   [social.subs :as subs]
   [cljs-time.core :as t]
   [cljs-time.format :as tf]
   ))

(defn login-field
  [{:keys [id label values typ]}]
  [:div {:class "field"}
   [:label {:for id} label]
   [:input {:id id
            :type typ
            :value (id @values)
            :on-change #(swap! values assoc id (.. % -target -value))}]])

(def login-state (r/atom {:email "bob@example.com" :password "pass"}))

(defn login-panel []
  [:div {:class "login-form"}
   (login-field {:id :email
                 :label "Email:"
                 :typ "email"
                 :values login-state})
   (login-field {:id :password
                 :label "Password: "
                 :typ "password"
                 :values login-state})
   [:button {:on-click #(re-frame/dispatch [::events/login @login-state])}
    "login"]])


(defn menu-link
  [href text]
  [:div
   [:a {:href href} text]])

(defn menu
  []
  [:nav 
   (menu-link "/" "home")
   (menu-link "/people" "people")
   (menu-link "/me" "me")
   (menu-link "/messages" "messages" )
   (menu-link "/calendar" "calendar")
   (menu-link "/logout" "logout")])

(defn dashboard-panel []
  (let [user @(re-frame/subscribe [::subs/user])]
    [:section
     [:h1 (str "Hey " (:name user) ". Here's what's going on!")]]))

(defn shitty-moment
  [d]
  (if (< (t/today) d)
    "--"
    (let [interval (t/interval d (t/today))]
      (condp < (t/in-days interval)
        365 (str (t/in-years interval) " years ago")
        30 (str (t/in-months interval) " months ago")
        (str (t/in-days interval) " days ago")))))

(def iso-fmt
  (tf/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))



(defn people-panel []
  (let [loading @(re-frame/subscribe [::subs/loading])
        people @(re-frame/subscribe [::subs/people])
        people-filter (re-frame/subscribe [::subs/people-filter])]
  [:section {:class "people"}
    [:h1 "People!"]
    [:div {:class "filter"}
     [:label {:for "people-filter"} "filter" ]
     [:input {:id "people-filter" :type "text"
              :value @people-filter
              :on-change (fn [this]
                           (re-frame/dispatch-sync
                             [::events/update-people-filter
                              (.. this -target -value)]))}]
     [:button {:on-click #(re-frame/dispatch [::events/create-person])} "+ new person"]]


    [:table
     [:thead
      [:tr
       [:th {:on-click #(re-frame/dispatch [::events/update-people-sort :name])} "name" ]
       [:th "tags"]
       [:th {:on-click #(re-frame/dispatch [::events/update-people-sort :last-seen])} "last-seen"]
       [:th {:on-click #(re-frame/dispatch [::events/update-people-sort :priority])} "priority"]
       [:th {:on-click #(re-frame/dispatch [::events/update-people-sort :cadence])} "cadence-date"]]]
     [:tbody
      (if people
        (for [person people]
          [:tr {:key (str "person-" (:id person))}
           [:td [:a {:href (str "/people/" (:id person)) }
                 (:name person)]]
           [:td (clojure.string/join ", " (:tags person))]
           [:td (shitty-moment (tf/parse (:last-seen person)))]
           [:td (condp < (:priority person)
                 1.0 [:span {:style {:color "red"}} "!!!"]
                 0.9 [:span {:style {:color "hotpink"}} "!!"]
                 0.8 [:span {:style {:color "salmon"}} "!"]
                 0.7 [:span {:style {:color "rebeccapurple"}} "??"]
                 0.6 [:span {:style {:color "darkblue"}} "?"]
                 "")]
           [:td (str (:cadence-num person) "/" (:cadence-period-human person))]])
        [:tr
         [:td {:colSpan 5} "no people yet, add some!"]])
      ]]]))

(defn person-panel
  []
  (let [loading @(re-frame/subscribe [::subs/loading])
        save-status (re-frame/subscribe [::subs/person-save-status])
        person @(re-frame/subscribe [::subs/person])]
    [:section {:class "person"}
     [:input {:class "name"
              :value (:name person)
              :placeholder "Name"
              :on-change #(re-frame/dispatch-sync
                            [::events/update-person :name (.. % -target -value)])}]
     [:div  {:class "field"}
      [:label "last seen:"]
      [:input {:value (:last-seen person)
               :type "date"
               :placeholder "last seen"
               :on-change #(re-frame/dispatch-sync
                             [::events/update-person :last-seen (.. % -target -value)])}]]
     [:div { :class "field" }
      [:label "cadence:"]
      [:input {:class "num"
               :type "number"
               :value (:cadence-num person)
               :on-change #(re-frame/dispatch-sync
                             [::events/update-person :cadence-num (int (.. % -target -value))])}]
      "/"
      [:select {:class "period"

                :value (:cadence-period-human person)
                :on-change #(re-frame/dispatch-sync
                              [::events/update-person :cadence-period-human (.. % -target -value)])}
       [:option {:value "month"} "month"]
       [:option {:value "quarter"} "quarter"]
       [:option {:value "year"} "year"]
       [:option {:value "decade"} "decade"]]]
     [:textarea {:class "notes"
                 :value (:notes person)
                 :on-change #(re-frame/dispatch-sync
                               [::events/update-person :notes (.. % -target -value)])}]
     
     [:div {:class "updating"} @save-status]
     ]))

(defn me-panel []
  [:section
    [:h1 "Me!"]])

(defn messages-panel []
  [:section
    [:h1 "Messages!"]])


(defn calendar-panel []
  (let [loading @(re-frame/subscribe [::subs/loading])
        calendar-keys @(re-frame/subscribe [::subs/calendar-keys])]
    [:section
     [:h1 "Calendar!"]
     [:p "it can be useful to see events that are coming up in your calendar
      software of choice, or to share your schedule with a friend/family"]
     [:p "use the following url to add your events to your calendar"]
     [:p
      [:strong [:a {:href "https://example.com/bob/calendar.ical"}
               "https://example.com/bob/calendar.ical"]]]
     [:h3 "keys"]
     [:p "Everyone that wants to access your calendar needs a password. You can
      create and destroy passwords below!"]
     (cond
       (:calendar-keys loading) [:div "loading...!"]
       (not calendar-keys) [:div "no keys"]
       :else 
       (for [calendar-key calendar-keys]
         [:div {:class "secret" :key (str "cal-key" (:id  calendar-key))}
          [:div {:class "details"}
           [:div {:class "title"} (:title calendar-key)]
           [:div {:class "username"} (:id calendar-key)]
           [:div {:class "date"}  "created: " (:created-at calendar-key)]
           [:div {:class "permissions"} (clojure.string/join ", " (:permissions calendar-key))]]
          [:div {:class "delete"} [:button "delete"]]]))]))

(defn main-panel []
  (let [route (re-frame/subscribe [::subs/route])
        logged-in? @(re-frame/subscribe [::subs/logged-in?])]
    (if logged-in?
      [:<>
       (menu)
       (case (:handler @route)
         :dashboard (dashboard-panel)
         :people (people-panel)
         :person (person-panel)
         [:h1 "404"])]
       (case (:handler @route)
         :login (login-panel)
         [:h1 "404"]))))

