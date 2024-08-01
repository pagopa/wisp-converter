# Integration Tests

👀 Integration tests are configured as:

- `config` folder contains configuration files;
- `features` folder contains feature description files;
- `stesp` folder contains code files;

See there for more information.

---

### ☁️ Pipeline execution

Launch the `integration-test` GitHub Action to execute remotely.
The reports will be published here:

🔗

---

### 💻 Local execution

If you want to test your local branch, you should change configuration.

``` shell
sh run_test.sh
```

ℹ️ Set in your environment:

- `PSP_SUBSCRIPTION_KEY` to set the subscription-key for `fdr-psp`;
- `ORG_SUBSCRIPTION_KEY` to set the subscription-key for `fdr-org`;
- `TAGS` to customize test you want execute. Default `runnable`;
- `JUNIT=true` to execute _behave_ with the junit option;

#### Things to know

In the feature, you can use the following keyword to annotate a specific variable in the text of a step:

- `#` is used for global variables
- `$` is used for local variables
  These variables will assume the related values at runtime.

