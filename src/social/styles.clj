(ns social.styles
  (:refer-clojure :exclude [rem])
  (:require [garden.units :refer [rem px vh]]
            [garden.selectors :as selectors]))


(def space (rem 0.5))
(def space-lots (rem 1))

(def screen
  (list 
    [:body :html
     {:font-family "monospace"
      :font-size (px 16)
      :line-height 1.5
      :margin "0 auto"
      :min-height (vh 100)}]

    [:section
     [:p {:max-width (rem 55)}]]

    [:#app {:display "flex"
            :padding (rem 0.5)
            :flex-direction "column"}]

    [:nav {:display "flex"
           :gap (rem 0.8)}
     [:a {:font-weight "bold"
          :color "#000"
          :text-decoration "none"}
      [:&:visited
       {:color "#000"
        :text-decoration "none"}]

      [:&:hover
       {:color "hotpink"
        :text-decoration "underline"}]]]

    [:.login-form {:display "flex"
                   :flex-direction "column"
                   :gap space
                   :padding space-lots}

     [:.field {:display "flex"
               :flex-direction "row"}
      [:label {:min-width (rem 8)
               :padding space}]
      [:input {:padding space
               :border-radius (px 5)
               :border "1px solid #000"
               :font-family "monospace"
               :font-size (rem 1)}]]]

    [:button {:padding space
              :font-family "monospace"
              :font-size (rem 1)}]

    [:.people {:max-width (rem 70)
               :display "flex"
               :flex-direction "column"
               :gap space}
     [:table {:width "100%"}
      [:thead
       [:tr {:background-color "#f0f0f0"}]
       [:th {:text-align "left"
             :padding (rem 0.2)}]]

      [:tbody
       [:tf {:padding (rem 0.2)}]
       [:tr {:background-color "#fafafa"}
        [(selectors/& (selectors/nth-child :odd)) {:background-color "#FFF"}]]]]
     [:.filter {:display "flex"
                :gap space}
      [:label {:padding space}]
      [:input {:padding space
               :border-radius (px 5)
               :border "1px solid #000"
               :font-family "monospace"
               :font-size (rem 1)
               :flex-grow 2
               }]]]

    [:.person {:max-width (rem 70)
               :display "flex"
               :flex-direction "column"
               :gap space
               :min-height (vh 80)}
     [:.name {:font-size (rem 1.5)
              :font-weight "bold"
              :padding 0
              :line-height 1.5
              :margin-top space
              :margin-bottom space}]
     [:input {:border (px 0)
              :font-family "monospace"
              :padding space}]

     [:.field {:display "flex"}]
     [:textarea {:flex-grow 2}]
     [:label {:min-width (rem 8)}]

     [:.notes {:border 0
               :font-family "monospace"
               :font-size (rem 1)}]
     [:select {:border 0
               :font-family "monospace"
               :font-size (rem 1)
               :background "#fff"}]
     [:.num {:max-width (rem 3)}]
     ]

    ))
