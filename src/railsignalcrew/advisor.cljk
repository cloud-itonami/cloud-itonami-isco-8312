(ns railsignalcrew.advisor
  "Rail Signal Crew Advisor — the advisor named in this repository's
  README, proposing a rail-operations coordination action (log a
  service record, schedule a crew operation, flag a safety concern,
  coordinate a maintenance order) from an operator's request.
  Swappable mock/llm; the advisor ONLY proposes —
  `railsignalcrew.governor` checks operator/section provenance and
  scope independently, and always escalates safety concerns and
  over-threshold maintenance orders. Modeled on
  cloud-itonami-isco-3313's advisor.

  This advisor NEVER proposes to directly finalize a switch-throw,
  signal-clearance or track-routing decision — its :action-text
  describes coordination/logging/scheduling only, never a finalization
  action. `railsignalcrew.governor` independently hard-blocks any
  proposal whose :action-text describes such a finalization action,
  regardless of the declared :op.

  A proposal: {:op :log-service-record|:schedule-crew-operation|
               :flag-safety-concern|:coordinate-maintenance-order
               :effect :propose :section-id str
               :maintenance-cost number|nil :action-text str
               :stake kw :confidence n :rationale str}"
  )

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(def ^:private default-action-text
  {:log-service-record
   "record switch/signal inspection and status data only; no switch or signal actuation"
   :schedule-crew-operation
   "propose an operator roster/shift assignment only; no switch or signal actuation"
   :flag-safety-concern
   "surface a switch-malfunction or signal-anomaly concern for human review; no switch or signal actuation"
   :coordinate-maintenance-order
   "propose signal/switch equipment maintenance procurement coordination only; no switch or signal actuation"})

(defn- infer [_store {:keys [op stake operator-id section-id maintenance-cost action-text]
                      :as request}]
  {:op op
   :effect :propose
   :section-id section-id
   :maintenance-cost maintenance-cost
   :action-text (or action-text (get default-action-text op "administrative coordination only"))
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for operator " operator-id
                   (when section-id (str " on section " section-id)))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a rail-operations scheduling advisor for railway brake,
   signal and switch operator coordination. Given a request, propose
   an :op, the :section-id, a :maintenance-cost when relevant, an
   :action-text describing the proposed coordination/logging action,
   an honest :confidence and a :stake. You coordinate ADMINISTRATIVE
   and LOGISTICS SCHEDULING only — you may never propose to throw a
   track switch, clear a signal, or finalize a routing decision; your
   :action-text must never describe such a finalization action, only
   logging, scheduling, flagging or procurement coordination. Safety
   concerns always require human sign-off regardless of confidence.
   Maintenance orders above the section's registered cost ceiling
   always require human sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
