# Security Policy

This project handles railway brake, signal and switch operator coordination
workflows. Treat vulnerabilities as potentially high impact even when the demo
data is synthetic — this domain is rail-safety adjacent, even though the
actor itself never operates a switch or signal.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real operator or section data exposure
- authorization bypass
- Rail Signal Crew Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path by which this actor's output could be wired to directly
  finalize a switch-throw, signal-clearance or track-routing decision

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
gftdcojp organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on operator data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real operator/section data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
- Never integrate this actor's `:commit` disposition as an authority
  signal for physically throwing a switch or clearing a signal.
