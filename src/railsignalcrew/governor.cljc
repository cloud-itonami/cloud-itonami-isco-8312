(ns railsignalcrew.governor
  "RailSignalCrewGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  coordination action an advisor may propose for a railway brake,
  signal and switch operator. The governor never dispatches hardware
  itself and never allows a proposal that directly finalizes a
  switch-throw, signal-clearance or track-routing decision — this
  actor coordinates ADMINISTRATIVE / LOGISTICS SCHEDULING only, at
  every maturity level. Modeled on cloud-itonami-isco-3313's
  accountingsupport.governor.

  Task twist: unlike a simple numeric ceiling, the domain-scope
  invariant (#5 below) is a text-pattern check against a dedicated
  `:action-text` field that must describe a coordination/logging/
  scheduling action, never a finalization action. The pattern list is
  phrased as finalization ACTION PHRASES ('throw the track switch',
  'clear the signal', 'set the route') and never as bare NOUNS
  ('switch', 'signal', 'track') — a bare-noun list would false-trip on
  ordinary section names and rationale text that legitimately mention
  switch/signal equipment by name (e.g. a section named 'Switch 12
  Junction', or advisor rationale 'flagged a signal anomaly on section
  S-4') without proposing to actuate it. See
  `railsignalcrew.governor-test`'s `default-mock-advisor-proposals-
  never-self-trip` test, which asserts every default mock-advisor
  proposal across all four allowed ops passes this check even when
  section names/ids contain the words 'switch' or 'signal'.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. operator provenance     — the operator must be registered.
    2. no-actuation             — proposal :effect must be :propose
                                (the governor never dispatches
                                hardware itself; it only gates what
                                the advisor may coordinate).
    3. section basis             — a proposal citing a section must
                                cite a REGISTERED section belonging to
                                this operator.
    4. closed op-allowlist       — :op must be one of the four
                                permitted coordination ops; anything
                                else is refused outright.
    5. no switch/signal/routing finalization — the proposal's
                                :action-text must never describe
                                throwing a track switch, clearing a
                                signal, or finalizing a track-routing
                                decision, REGARDLESS of the declared
                                :op. This is a hard, PERMANENT block —
                                there is no maturity level or
                                confidence at which this actor may
                                auto-commit or human-approve its way
                                past this rule; it is not an
                                escalation, it is an outright refusal.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :flag-safety-concern (any surfaced switch-malfunction or
                                signal-anomaly concern always requires
                                human sign-off before any coordinated
                                response).
    7. :op :coordinate-maintenance-order whose :maintenance-cost
                                exceeds the section's registered
                                `:max-maintenance-cost` (procurement
                                coordination above the registered
                                ceiling always requires human
                                sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [railsignalcrew.store :as store]))

(def confidence-floor 0.6)

(def ^:private allowed-ops
  #{:log-service-record :schedule-crew-operation
    :flag-safety-concern :coordinate-maintenance-order})

;; Phrased as finalization ACTION PHRASES (verb + object), never bare
;; nouns — see the namespace docstring above for why a bare-noun list
;; would self-trip on ordinary section names/rationale text.
(def ^:private finalization-patterns
  [#"(?i)throw(?:ing|s|n)? (the )?(track )?switch"
   #"(?i)clear(?:ing|s|ed)? (the )?signal"
   #"(?i)set(?:ting)? (the )?(track )?rout(?:e|ing)"
   #"(?i)rout(?:e|ing)? (the )?train"
   #"(?i)finaliz(?:e|ing|ed) (the )?(track )?rout(?:e|ing)"
   #"(?i)activat(?:e|ing|ed) (the )?signal"
   #"(?i)chang(?:e|ing|ed) (the )?signal (aspect|indication)"
   #"(?i)actuat(?:e|ing|ed) (the )?(track )?switch"])

(defn- finalizes-switch-or-signal? [text]
  (boolean (and text (some #(re-find % text) finalization-patterns))))

(defn- hard-violations [{:keys [request proposal]} operator-record s]
  (let [{:keys [op section-id action-text]} proposal]
    (cond-> []
      (nil? operator-record)
      (conj {:rule :no-operator :detail "未登録 operator"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は自らハードウェアを直接作動させない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :op-not-allowed :detail "許可されていない op（closed allowlist 外）"})

      (and section-id (nil? s))
      (conj {:rule :unknown-section :detail "未登録 section への提案は不可"})

      (and section-id s (not= (:operator-id s) (:operator-id request)))
      (conj {:rule :section-wrong-operator :detail "section が別 operator のもの"})

      (finalizes-switch-or-signal? action-text)
      (conj {:rule :finalizes-switch-or-signal
             :detail "スイッチ転換・信号現示・進路確定を直接実行する提案は常に恒久的に拒否（この actor は事務/ロジスティクス調整のみを担う）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `railsignalcrew.store/Store`. Pure — never
  mutates the store, never dispatches hardware, never allows a
  switch-throw/signal-clearance/track-routing finalization."
  [request context proposal store]
  (let [operator-record (store/operator store (:operator-id request))
        s (some->> (:section-id proposal) (store/section store))
        hard (hard-violations {:request request :proposal proposal}
                              operator-record s)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        safety-flag? (= :flag-safety-concern (:op proposal))
        over-cost? (and (= :coordinate-maintenance-order (:op proposal))
                        s
                        (number? (:maintenance-cost proposal))
                        (> (:maintenance-cost proposal) (:max-maintenance-cost s)))
        always-risky? (or safety-flag? over-cost?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
