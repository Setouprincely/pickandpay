# one-duch

Doch is organized as a monorepo containing the backend API and frontend applications:

- `DochBackend/` — Spring Boot API and Docker Compose configuration. See its `README.md` for setup instructions.
- `DochMarket-Frondend/` — Nuxt frontend applications. See its `README.md` for frontend setup.

Local environment files, dependency directories, and build outputs are excluded from this repository.

## Workflow validation & staging acceptance

- Requirements checklist, workflow diagrams, test results and known limitations: [`DochBackend/docs/ACCEPTANCE.md`](DochBackend/docs/ACCEPTANCE.md)
- Endpoints and role/permission matrix: [`DochBackend/docs/API.md`](DochBackend/docs/API.md)
- Staging deployment and rollback plan: [`DochBackend/docs/DEPLOYMENT.md`](DochBackend/docs/DEPLOYMENT.md)
- Automated end-to-end acceptance runner: [`DochBackend/acceptance/run-acceptance.mjs`](DochBackend/acceptance/run-acceptance.mjs)