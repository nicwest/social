(ns social.handler-test
  (:require [clojure.test :as t]
            [social.handler :as handler]
            [java-time.api :as jt]))

(def now (jt/local-date "2024-03-01"))
(def yesterday (jt/minus now (jt/days 1)))

(t/deftest test-calc-priority
  (t/testing "last seen yesterday, 30 day cadence"
    (t/is (= (handler/calc-priority
               now 30 1 yesterday)
             1/30)))
  (t/testing "last seen 90 days ago, 30 day cadence"
    (t/is (= (handler/calc-priority
               now 30 1 (jt/minus now (jt/days 90)))
             3)))
  (t/testing "last seen 15 days ago, 30 day cadence"
    (t/is (= (handler/calc-priority
               now 30 1 (jt/minus now (jt/days 15)))
             1/2)))
  (t/testing "last seen 1 days ago, 2 times in a 30 day period cadence"
    (t/is (= (handler/calc-priority
               now 30 2 yesterday)
             1/15))))


(t/deftest test-person->json
  (t/testing "json version of the person"
    (t/is (= (handler/person->json {:last-seen now})
             {:last-seen "2024-03-01"}))))

(t/deftest test-json->person
  (t/testing "the person version of json"
    (t/is (= (handler/json->person {"last-seen" "2024-03-01"
                                    "cadence-period-human" "month"})
             {:last-seen (jt/local-date "2024-03-01")
              :cadence-period-human "month"
              :cadence-period 30}))))

(t/deftest test-update-priority
  (t/testing "priority is updated"
    (t/is (= (handler/update-priority
               now
               {:cadence-num 1
                :cadence-period 30
                :last-seen yesterday})
             {:cadence-num 1
              :cadence-period 30
              :last-seen yesterday
              :priority 1/30}))))
