# DOCH Platform Backend


![Java](https://img.shields.io/badge/Java-25-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue)

This repository contains the backend source code for the DOCH Platform, a robust digital ecosystem designed to handle e-commerce, third-party seller management, and ad-hoc logistics. This monolithic application is built with Spring Boot and is designed to be run in a containerized environment.

---

## 1. Project Overview

The DOCH Platform is a multi-platform system divided into five distinct but interconnected components:

*   **DochMarket:** The primary consumer-facing e-commerce marketplace.
*   **Seller Portal:** A dashboard for third-party sellers to manage their products and sales.
*   **DochXender:** A standalone logistics service for ad-hoc parcel delivery.
*   **Admin Dashboard:** An internal tool for managing the entire business operation.
*   **Driver's App:** A mobile-first application for delivery drivers.

This monolithic backend serves all five frontend applications via a unified API.

---

## 2. Technology Stack

*   **Framework:** [Spring Boot](https://spring.io/projects/spring-boot) 4.0.0
*   **Language:** Java 25
*   **Database:** [PostgreSQL](https://www.postgresql.org/) 18
*   **Authentication:** Spring Security with JSON Web Tokens (JWT)
*   **Data Access:** Spring Data JPA / Hibernate
*   **API Documentation:** [SpringDoc OpenAPI](https://springdoc.org/) (Swagger UI)
*   **Build Tool:** [Apache Maven](https://maven.apache.org/)
*   **Containerization:** [Docker](https://www.docker.com/) & Docker Compose

---

## 3. Getting Started: Prerequisites

Before you begin, ensure you have the following installed on your system:

1.  **Git**: For cloning the repository.
2.  **Docker & Docker Compose v2**: Required to run the containerized environment. Ensure you are using `docker compose` (with a space), not the legacy `docker-compose`.
3.  **Java Development Kit (JDK) 25**: Required *only* if you choose the Hybrid (IDE) setup.
4.  **IntelliJ IDEA (or other IDE)**: Required *only* if you choose the Hybrid (IDE) setup.

---

## 4. First-Time Environment Setup

These steps only need to be performed once per machine.

1.  **Clone the repository:**
    ```bash
    git clone <your-repository-url>
    cd DochBackend
    ```

2.  **Create your local environment file:**
    This project uses a `.env` file to manage secrets and local configurations. Copy the template to create your own local file.
    ```bash
    cp .env.example .env
    ```

3.  **Configure your `.env` file:**
    Open the newly created `.env` file with a text editor. Fill in the placeholder values (e.g., `<your-local-db-password>`) in the "Local Development" section. **This file is ignored by Git and should never be committed.**

---

## 5. Running the Project for Development

There are two recommended ways to run the project.

### Option A: All-in-Docker (Recommended for Simplicity & Consistency)

This method runs both the Spring Boot application and the PostgreSQL database as Docker containers.

1.  **Build and Start:**
    From the project root directory, run:
    ```bash
    docker compose up --build
    ```
    *(For subsequent runs without code changes, `docker compose up` is sufficient.)*

2.  **Accessing Services:**
    *   **Backend API:** `http://localhost:8080`
    *   **Swagger UI (API Docs):** `http://localhost:8080/swagger-ui.html`
    *   **Database Admin Tool (Adminer):** `http://localhost:8081`
        *   **Server:** `db`
        *   **Username/Password/Database:** (Use values from your `.env` file)

3.  **Stopping the Environment:**
    Press `Ctrl + C` in the terminal, then run `docker compose down`.

---

### Option B: Hybrid - IDE + Docker (Recommended for Debugging)

This method runs the database in Docker but runs the Spring Boot application directly from your IDE, enabling live debugging and faster restarts.

**Step 1: Start the Database Container**
In a terminal at the project root, run the dedicated database compose file:
```bash
docker compose -f docker-compose.db.yml up
```
*This will start only the PostgreSQL and Adminer containers. Leave this terminal running.*

**Step 2: Configure and Run in IntelliJ (First Time Only)**

1.  **Install EnvFile Plugin:** Go to `File` -> `Settings` -> `Plugins`. Search for `EnvFile` in the Marketplace, install it, and restart the IDE.

2.  **Create the Run Configuration:**
    *   Find `DochApplication.java` in the project tree.
    *   Right-click the green play icon (▶️) next to the class name and select `Run 'DochApplication.main()'`.
    *   **The application will fail to start.** This is expected. This step's only purpose is to make IntelliJ auto-create a run configuration.

3.  **Edit the Run Configuration:**
    *   In the top-right corner of IntelliJ, click the dropdown next to the green play button and select `Edit Configurations...`.
    *   In the window that appears, you will configure three things:

    **a. Add VM Options:**
    *   Click `Modify options` -> `Add VM options`.
    *   In the new **VM options** field, paste this exact text:
        ```
        -Dspring.profiles.active=local
        ```

    **b. Enable EnvFile Plugin:**
    *   Find the **"EnvFile"** panel.
    *   Check the box for **"Enable EnvFile"**.
    *   Click the `+` icon to add a new file.

    **c. Select the `.env` File (and show hidden files):**
    *   In the "Select .env file" window, you may not see your `.env` file because it's ignored by Git.
    *   **Press `Ctrl + H` or click the "Show Hidden Files" icon** in the file picker's toolbar.
    *   Select the `.env` file from your project root and click `OK`.

4.  **Save the Configuration:** Click `Apply`, then `OK`.

**Step 3: Run the Application from IntelliJ**
You are now fully configured.
*   Make sure your database is still running from Step 1.
*   Click the green **Run (▶️)** or **Debug (🐞)** button in the top-right corner of IntelliJ.
*   The application will now start successfully and connect to the Docker database.

**Step 4: Stopping the Hybrid Environment**
1.  Click the red **Stop (⏹️)** button in IntelliJ.
2.  Go to the terminal running the database, press `Ctrl + C`, then run `docker compose -f docker-compose.db.yml down`.

---

## 6. API Documentation

Once the application is running (using either method), you can explore the full API documentation and interact with the endpoints via Swagger UI at:
**[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

A summary of the workflow endpoints and the role/permission matrix is in [docs/API.md](docs/API.md).

---

## 7. Database schema and migrations

The schema is managed by **Flyway** (`src/main/resources/db/migration`); Hibernate only validates it (`ddl-auto=validate`).

| Migration | Content |
| --- | --- |
| `V1__baseline_schema.sql` | schema of the original application |
| `V2__workflow_hardening.sql` | product publication & weight/dimensions, order references & cancellation, payment flow/provider status, vehicle categories & decision matrix, package tracking numbers / OTP attempts / vehicle / schedule / hub, `delivery_event`, `audit_log`, `order_review`, wallet reversal states |
| `V3__backfill_package_vehicle_requirements.sql` | weight/vehicle requirement for packages created before V2 |
| `V4__user_extra_roles.sql` | one account can hold several roles (buyer + seller + driver) |

An empty database receives V1-V4. A database previously created by Hibernate is baselined at V1 and receives V2-V4.
Never edit an applied migration - add a new `V<n>__description.sql`.

No demo data is seeded. The first super-admin is created from `BOOTSTRAP_SUPER_ADMIN_EMAIL` / `BOOTSTRAP_SUPER_ADMIN_PASSWORD`;
all other users register through the portals (customers, sellers) or are invited by an admin (drivers, staff).

---

## 8. Tests

```bash
mvn test                  # unit tests (state machine, vehicle matrix, parsing, references, uploads)
DOCH_IT=true mvn test     # additionally boots the full context against the configured database
```

> On Windows, `mvnw.cmd` fails when the user-profile path contains a space; call the Maven distribution downloaded by the
> wrapper directly (`%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.10\<hash>\bin\mvn.cmd`) or an installed Maven.

End-to-end acceptance (workflows A-H against a running API with Fapshi **sandbox** credentials, real storage and e-mail):

```bash
BASE_URL=http://localhost:8080 ADMIN_PASSWORD='<bootstrap password>' OTP_DB_NAME=doch node acceptance/run-acceptance.mjs
```

It creates its own customer, seller, driver, vehicles, product, orders and payments, prints PASS/FAIL per step and writes
an evidence JSON to `acceptance/results/`. Run it against staging or a scratch database only. Results of the signed-off run
and the requirements checklist are in [docs/ACCEPTANCE.md](docs/ACCEPTANCE.md).

---

## 9. External services

| Service | Variables | Notes |
| --- | --- | --- |
| Fapshi | `FAPSHI_API_USER`, `FAPSHI_API_KEY`, `FAPSHI_BASE_URL`, `FAPSHI_WEBHOOK_SECRET` | Sandbox: `670000000` succeeds, `670000001` fails. Webhook URL `/api/payments/webhook/fapshi` (header `x-wh-secret`). |
| Resend | `RESEND_API_KEY` | requires a verified sending domain for real recipients |
| S3 storage | `MINIO_*` | product, sealed-package and proof-of-delivery photos |
| Geocoding | `GEO_PROVIDER` (`osm` or `google`), `GOOGLE_MAPS_API_KEY` | Google requires billing; `osm` uses Nominatim + OSRM |

Deployment, rollback and operations: [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Connecting a frontend

Run the backend at `http://localhost:8080` and set `NUXT_PUBLIC_API_BASE=http://localhost:8080` for the portals in
`DochMarket-Frondend`. Frontend requests use the `/api/...` routes documented in Swagger. For authenticated routes, send the
access token as `Authorization: Bearer <access_token>`.

Browser origins on `http://localhost:3000`, `http://localhost:5173`, and `http://localhost:5174` are allowed by default. For the other portals (3001, 3003, 3004, 3005) or a deployed frontend, set `CORS_ALLOWED_ORIGINS` in `.env` to a comma-separated list of exact origins, including scheme and port (for example, `https://shop.example.com`). Then restart the backend.
