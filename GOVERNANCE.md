# Governance

`cloud-itonami-isco-8312` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions or disclose records.
- Rail Signal Crew Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- no proposal may ever finalize a switch-throw, signal-clearance or
  track-routing decision — this actor coordinates ADMINISTRATIVE /
  LOGISTICS SCHEDULING only, and that boundary may not be relaxed by
  a maintainer PR without a superseding ADR.
- every commit, hold and approval path is auditable.
- real operator/section data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification, license, or
the switch/signal-finalization scope boundary should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling operator/section data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- wiring this actor's output to directly finalize a switch-throw,
  signal-clearance or track-routing decision
