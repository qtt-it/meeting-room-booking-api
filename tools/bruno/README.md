# API collection

Open this folder as a Bruno collection and select the Local environment.
Start the application with Docker Compose first.

Run requests in numeric order against a disposable development database.
The collection creates one employee, one room and two bookings.
It changes the employee password and records cancellation and rejection.
Do not run it against a production database.
Re-running uses unique names but still consumes one active room slot.

Login captures tokens in runtime variables with `bru.setVar`.
Tokens stay in memory and are not written to environment files.
Local credentials are demonstration values only.
Override credentials in Bruno when using a different development instance.
Never commit real credentials or exported tokens.

The collection generates future Monday-Saturday meeting times.
With Node.js 22 installed, run:

```bash
cd tools/bruno
npx --yes @usebruno/cli@4.3.0 run --env Local --bail --reporter-skip-all-headers --reporter-skip-body
```

Each request checks its status and response envelope.
The automated PostgreSQL suite also checks negative and concurrent cases.

Script API: [Bruno JavaScript reference](https://docs.usebruno.com/testing/script/javascript-reference).
