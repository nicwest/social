(ns social.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [social.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
