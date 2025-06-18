# Security Policy

The following security policies are applicable to the **javamon** source code, documentation and releases. Please, follow these rules if you wish to report a vulnerability, request an enhancement or otherwise contribute to the project.

## Good practices to follow

:warning: **Never store credentials information into source code or config file in a GitHub repository**
- Block sensitive data being pushed to GitHub by git-secrets or its likes as a git pre-commit hook
- Audit for slipped secrets with dedicated tools
- Use environment variables for secrets in CI/CD (e.g. GitHub Secrets) and secret managers in production
- Don't include links to irrelevant external websites or files

## Reporting a Vulnerability

Please, use the [Security Advisories](https://github.com/vkamenar/javamon/security/advisories) to report vulnerabilities or any other security concerns.

## Security Update policy

Vulnerabilities will be communicated via GitHub Advisories and a description of the issue will be included in the release notes.
