(ns railsignalcrew.store
  "SSoT for the ISCO-08 8312 railway brake, signal and switch operator
  coordination practice actor (itonami actor pattern, ADR-2607121000 /
  CLAUDE.md Actors section; README's 'Robotics premise' — a rail
  operations scheduling and logging robot performs service-record
  logging, crew-roster coordination and maintenance-order drafting
  under this advisor/governor pair, which never dispatches hardware
  itself and never throws a track switch, clears a signal, or
  finalizes a routing decision). Modeled on
  cloud-itonami-isco-3313's accountingsupport.store.

  This actor is an ADMINISTRATIVE / LOGISTICS SCHEDULING coordination
  layer only. It never operates a switch or signal — the physical
  switch-throw / signal-clearance / track-routing decision stays
  outside this actor's authority at every maturity level.

  Domain:

    operator — a registered railway brake, signal and switch operator
               (:operator-id, :name)
    section  — a registered track/signal section under an operator's
               coverage {:section-id :operator-id :name
               :max-maintenance-cost number}. `:max-maintenance-cost`
               is the registered ceiling a proposed maintenance-order
               cost should stay within — a proposal above it does not
               finalize anything by itself, but always requires human
               sign-off before any procurement coordination proceeds.
    record   — a committed operating record (a posted service-record
               log, crew-schedule proposal, safety flag or maintenance
               coordination order) — written ONLY via commit-record!.
    ledger   — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (operator [s operator-id])
  (section [s section-id])
  (records-of [s operator-id])
  (ledger [s])
  (register-operator! [s operator])
  (register-section! [s sec])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (operator [_ operator-id] (get-in @a [:operators operator-id]))
  (section [_ section-id] (get-in @a [:sections section-id]))
  (records-of [_ operator-id] (filter #(= operator-id (:operator-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-operator! [s op]
    (swap! a assoc-in [:operators (:operator-id op)] op) s)
  (register-section! [s sec]
    (swap! a assoc-in [:sections (:section-id sec)] sec) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:operators {} :sections {} :records [] :ledger []}
                                   seed)))))
