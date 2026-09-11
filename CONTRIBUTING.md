# Contributing

`cloud-itonami-isco-8312` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:test
kbb -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real operator, section or operating documents.
- Keep production writes and disclosures behind Rail Signal Crew Governor.
- Never relax the switch/signal/track-routing finalization hard-block —
  this actor coordinates ADMINISTRATIVE / LOGISTICS SCHEDULING only.
  Any change touching `railsignalcrew.governor`'s `finalization-patterns`
  or `hard-violations` needs an ADR, not just tests.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
