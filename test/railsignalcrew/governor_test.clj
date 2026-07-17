(ns railsignalcrew.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [railsignalcrew.store :as store]
            [railsignalcrew.advisor :as advisor]
            [railsignalcrew.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-operator! st {:operator-id "operator-1" :name "Kobo Rail Ops"})
    (store/register-section! st {:section-id "S-1" :operator-id "operator-1"
                                 :name "Switch 12 Junction"
                                 :max-maintenance-cost 5000})
    st))

(def ^:private req {:operator-id "operator-1"})

(defn- proposal [op overrides]
  (merge {:op op :effect :propose :section-id "S-1"
         :action-text "log-only, no switch/signal actuation"
         :confidence 0.9 :stake :low}
        overrides))

(deftest ok-log-service-record
  (let [st (fresh-store)
        v (governor/check req {} (proposal :log-service-record {}) st)]
    (is (:ok? v))))

(deftest ok-schedule-crew-operation
  (let [st (fresh-store)
        v (governor/check req {} (proposal :schedule-crew-operation {}) st)]
    (is (:ok? v))))

(deftest ok-coordinate-maintenance-order-within-ceiling
  (testing "the maintenance-cost ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (proposal :coordinate-maintenance-order
                                             {:maintenance-cost 5000}) st)]
      (is (:ok? v)))))

(deftest hard-on-unregistered-operator
  (let [st (fresh-store)
        v (governor/check {:operator-id "nobody"} {} (proposal :log-service-record {}) st)]
    (is (:hard? v))
    (is (some #(= :no-operator (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (proposal :log-service-record {:effect :direct-write}) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-not-in-closed-allowlist
  (testing "an op outside the closed allowlist is refused outright, never auto-commit-eligible"
    (let [st (fresh-store)
          v (governor/check req {} (proposal :throw-switch {:confidence 0.99}) st)]
      (is (:hard? v))
      (is (some #(= :op-not-allowed (:rule %)) (:violations v))))))

(deftest hard-on-unknown-section
  (let [st (fresh-store)
        v (governor/check req {} (proposal :log-service-record {:section-id "S-ghost"}) st)]
    (is (:hard? v))
    (is (some #(= :unknown-section (:rule %)) (:violations v)))))

(deftest hard-on-foreign-section
  (let [st (fresh-store)]
    (store/register-operator! st {:operator-id "operator-2" :name "Other"})
    (let [v (governor/check {:operator-id "operator-2"} {} (proposal :log-service-record {}) st)]
      (is (:hard? v))
      (is (some #(= :section-wrong-operator (:rule %)) (:violations v))))))

(deftest hard-and-permanent-on-switch-throw-finalization-text
  (testing "a proposal to directly throw the track switch is a hard, permanent block regardless of declared op or confidence"
    (let [st (fresh-store)
          v (governor/check req {}
                            (proposal :log-service-record
                                      {:action-text "throw the track switch on section S-1"
                                       :confidence 0.99})
                            st)]
      (is (:hard? v))
      (is (not (:escalate? v)))
      (is (some #(= :finalizes-switch-or-signal (:rule %)) (:violations v))))))

(deftest hard-and-permanent-on-signal-clearance-finalization-text
  (testing "a proposal to directly clear the signal is a hard, permanent block regardless of declared op or confidence"
    (let [st (fresh-store)
          v (governor/check req {}
                            (proposal :schedule-crew-operation
                                      {:action-text "clear the signal for approach"
                                       :confidence 0.99})
                            st)]
      (is (:hard? v))
      (is (not (:escalate? v)))
      (is (some #(= :finalizes-switch-or-signal (:rule %)) (:violations v))))))

(deftest hard-and-permanent-on-track-routing-finalization-text
  (testing "a proposal to set the track route is a hard, permanent block regardless of declared op or confidence"
    (let [st (fresh-store)
          v (governor/check req {}
                            (proposal :coordinate-maintenance-order
                                      {:action-text "set the route for the incoming train"
                                       :maintenance-cost 100
                                       :confidence 0.99})
                            st)]
      (is (:hard? v))
      (is (not (:escalate? v)))
      (is (some #(= :finalizes-switch-or-signal (:rule %)) (:violations v))))))

(deftest always-escalates-flag-safety-concern-even-at-high-confidence
  (testing "any surfaced switch-malfunction or signal-anomaly concern always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (proposal :flag-safety-concern {:confidence 0.99}) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-over-ceiling-maintenance-order-even-at-high-confidence
  (testing "procurement coordination above the registered ceiling always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (proposal :coordinate-maintenance-order
                                             {:maintenance-cost 50000 :confidence 0.99}) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (proposal :log-service-record {:confidence 0.3}) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

;; -----------------------------------------------------------------
;; Dedicated regression test for the known self-tripping bug pattern:
;; a naive bare-noun scope-exclusion list (e.g. checking for the
;; substrings "switch"/"signal") would false-trip on the advisor's OWN
;; default rationale/action-text whenever a section name legitimately
;; contains those words (rail sections are routinely named things like
;; "Switch 12 Junction" or "Signal Box 4" — that is equipment
;; identification, not a proposal to actuate it). This test asserts
;; the default mock-advisor proposal for every allowed op, against a
;; section whose NAME contains both "switch" and "signal" as bare
;; nouns, never hard-blocks (never self-trips) — the governor's
;; `finalizes-switch-or-signal?` check must only match finalization
;; ACTION PHRASES ("throw the switch", "clear the signal"), never bare
;; nouns.
;; -----------------------------------------------------------------

(defn- self-trip-store []
  (let [st (store/mem-store)]
    (store/register-operator! st {:operator-id "operator-1" :name "Kobo Rail Ops"})
    (store/register-section! st {:section-id "S-signal-switch" :operator-id "operator-1"
                                 :name "Switch and Signal Box 4"
                                 :max-maintenance-cost 5000})
    st))

(deftest default-mock-advisor-proposals-never-self-trip
  (testing "default mock-advisor proposals across all four allowed ops, against a
           section literally named with the words 'switch' and 'signal', never
           hard-trip the finalization-scope rule"
    (let [st (self-trip-store)
          advisor (advisor/mock-advisor)
          ops [:log-service-record :schedule-crew-operation
               :flag-safety-concern :coordinate-maintenance-order]]
      (doseq [op ops]
        (let [request {:operator-id "operator-1" :op op :stake :high
                       :section-id "S-signal-switch"
                       :maintenance-cost (when (= op :coordinate-maintenance-order) 100)}
              p (advisor/-advise advisor st request)
              v (governor/check request {} p st)]
          (testing (str "op " op)
            (is (not (:hard? v))
                (str "self-tripped on default proposal for " op ": " (:violations v)))))))))
