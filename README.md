# pagoPA WISP Converter

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-wisp-converter&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-wisp-converter)
[![Integration Tests](https://github.com/pagopa/pagopa-wisp-converter/actions/workflows/05_integration_test.yml/badge.svg?branch=main)](https://github.com/pagopa/pagopa-wisp-converter/actions/workflows/integration_test.yml)

A service that permits to handle nodoInviaRPT and nodoInviaCarrelloRPT request from WISP, interfacing
them with GPD system

---

## Api Documentation 📖

See
the [OpenApi 3 here.](https://editor.swagger.io/?url=https://raw.githubusercontent.com/pagopa/pagopa-wisp-converter/main/openapi/openapi.json)

---

## Technology Stack

- Java 17
- Spring Boot
- Hibernate
- Azure CosmosDB
- Redis Cache

---

## Develop Locally 💻

### Prerequisites

- Maven
- JDK17

### Before the first run

In order to get the latest updated sources and to correctly execute the application, it is necessary to
generate the source classes from WSDL and XSD definition. For doing so, move in `scripts` folder and
execute the following command:

`sh update-specs.sh`

After the execution, the newly generated classes are added in `target/generated-sources/jaxb` folder.
The application now can be run and all the class references are correctly resolved.

### Run the project

Start the Spring Boot application with this command:

`mvn spring-boot:run -Dspring-boot.run.profiles=local`

### Testing 🧪

#### Unit testing

To run the **Junit** tests:

`mvn clean verify`

#### Integration testing

For automatic execution of simple tests for checking the correct workflow between the various components, execute the
following commands from `./integration-test/system-test`:

`sh run.sh dev TESTCASE SUBKEY`

A more wide information about this test can be found in Readme file in the same folder.  
For classical integration tests with Cucumber, execute the following commands from `./integration-test/cucumber/src`:

1. `yarn install`
2. `yarn test`

#### Performance testing

install [k6](https://k6.io/) and then from `./performance-test/src`

1. `k6 run --env VARS=local.environment.json --env TEST_TYPE=./test-types/load.json main_scenario.js`

---

## Contributors 👥

Made with ❤️ by PagoPa S.p.A.

### Mainteiners

See `CODEOWNERS` file
