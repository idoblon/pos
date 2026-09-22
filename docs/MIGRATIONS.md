# Database Migrations

## Current state

- **Schema management:** `spring.jpa.hibernate.ddl-auto=update` (Hibernate auto-updates
  the schema on startup). This is convenient for development but is **not** a
  substitute for versioned migrations — it cannot rename columns, cannot narrow
  types safely, and gives no review trail.
- **Flyway:** `flyway-core` is included as a dependency but **disabled**:
  `spring.flyway.enabled=false` in `application.properties`.

## What Hibernate will apply automatically (next startup)

The money fields on `orders` / `order_items` were converted from `DOUBLE` to
`DECIMAL(19,2)` (`BigDecimal`). With `ddl-auto=update`, Hibernate issues the
corresponding `ALTER TABLE` statements automatically. Existing rows are
converted by MySQL; values stored with binary-float noise (e.g. `12.340000000000001`)
will be rounded to two decimal places during conversion.

## Enabling Flyway (planned follow-up)

1. **Baseline first.** The database already exists and is populated, so Flyway
   must start from a baseline instead of trying to create tables that exist:

   ```properties
   spring.flyway.baseline-on-migrate=true
   spring.flyway.baseline-version=1
   ```

2. Put versioned scripts in `src/main/resources/db/migration`, named
   `V1__init.sql`, `V2__....sql`, etc.
3. Generate the initial script from the current schema (e.g. with
   `mysqldump --no-data`), review it, and commit it.
4. Set `spring.flyway.enabled=true`.
5. **Turn off** `ddl-auto` (set it to `validate`) once Flyway owns the schema —
   letting both tools write the schema will cause conflicts.

## Conventions

- Never edit a migration that has been applied anywhere; add a new version.
- Migrations must be backward compatible with the previous release (expand →
  migrate → contract) so the app can run against old and new schema during deploys.
