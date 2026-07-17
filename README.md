# cloud-itonami-isco-8312

Open Occupation Blueprint for **ISCO-08 8312**: Railway Brake, Signal and Switch Operators.

This repository designs a forkable OSS business for a railway brake, signal and switch operator coordination practice: a rail operations scheduling and logging robot manages service-record logging, crew-roster coordination and maintenance-order drafting under a governor-gated actor, so the practice keeps its own operating records instead of renting a closed rail-ops SaaS.

**This actor coordinates ADMINISTRATIVE and LOGISTICS SCHEDULING only.**
It never operates a switch or signal, at any maturity level. The closed
op-allowlist below excludes any op that could directly finalize a
switch-throw, signal-clearance or track-routing decision, and the
governor independently, permanently hard-blocks any proposal whose
action text describes such a finalization action — this is not an
escalation path a human can approve through; it is an outright refusal.

**Maturity: `:implemented`.** `src/railsignalcrew/` implements the
`RailSignalCrewActor` as a `langgraph.graph/state-graph`
(`railsignalcrew.actor`) wired to a `Rail Signal Crew Advisor`
(`railsignalcrew.advisor`) and an independent `RailSignalCrewGovernor`
(`railsignalcrew.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 21 tests / 49 assertions green (`clojure -M:test`).

HARD invariants (always hold, never overridable): operator provenance,
no-actuation (`:effect` must be `:propose`), a registered section
basis for any section-citing proposal, a closed op-allowlist (only
`:log-service-record`, `:schedule-crew-operation`,
`:flag-safety-concern` and `:coordinate-maintenance-order` are
permitted), and — the domain-critical rule — no proposal may ever
describe directly throwing a track switch, clearing a signal, or
finalizing a track-routing decision, regardless of the declared `:op`
or confidence. That last rule is a hard, PERMANENT block: there is no
`:request-approval` escape hatch for it.

Always-escalate cases (human sign-off regardless of confidence,
mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (any surfaced switch-malfunction/signal-anomaly
concern) and `:coordinate-maintenance-order` proposals whose
maintenance cost exceeds the section's registered ceiling.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical/administrative domain work**. Here a rail operations scheduling
and logging robot performs service-record logging, crew-roster coordination
and maintenance-order drafting under an actor that proposes actions and an
independent **Rail Signal Crew Governor** that gates them. The governor never
dispatches hardware itself and never allows a switch-throw, signal-clearance
or track-routing finalization; `:high`/`:safety-critical` actions (such as a
surfaced safety concern or an over-ceiling maintenance order) require human
sign-off.

## Core Contract

```text
operator roster + section registry + service-record intake
        |
        v
Rail Signal Crew Advisor -> Rail Signal Crew Governor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated, administrative only) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
suppress an operating record, disclose sensitive data without governor
approval and audit evidence, or — regardless of any approval path —
finalize a switch-throw, signal-clearance or track-routing decision.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8312`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
