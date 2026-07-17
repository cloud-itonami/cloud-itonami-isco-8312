# Operator Guide

## Scope Reminder

This practice coordinates ADMINISTRATIVE / LOGISTICS SCHEDULING only.
It never operates a switch or signal. Any deployment that wires this
actor's output into a system capable of actually throwing a switch or
clearing a signal is out of scope for this repository and must not
rely on this actor's governor as a safety interlock for that action.

## First Deployment

1. Register operators and the sections/switch-signal equipment under
   their coverage.
2. Define consent and purpose categories for logged service records.
3. Run synthetic operating cases across all four allowed ops.
4. Enable human-reviewed sign-off for `:flag-safety-concern` and
   over-ceiling `:coordinate-maintenance-order` proposals.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path (safety concerns always reach a human)
- provenance for all operating records
- human review for over-ceiling maintenance coordination
- audit export for all gated actions
- confirm no downstream integration treats this actor's `:commit`
  disposition as authority to throw a switch or clear a signal

## Certification

Certified operators must prove that the governor gates every
safety-critical coordination action, that safety-critical risks
escalate to humans, and that no coordination action from this actor is
ever wired to directly finalize a switch-throw, signal-clearance or
track-routing decision.
