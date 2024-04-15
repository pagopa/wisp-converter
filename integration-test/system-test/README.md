# System tests for pagoPA WISP Converter

In order to correctly start the tests, first of all execute the command:

```shell
npm install
```

Then, if the test must be executed in DEV environment, execute the command:

```shell
sh run.sh dev TESTCASE SUBKEY
```

Alternatively, if the test must be executed in UAT environment, execute the command:

```shell
sh run.sh uat TESTCASE SUBKEY
```

## Test cases

You can choose one of the following test cases:

| Outcome | Name                       | Primitive    | Command                                 |
|---------|----------------------------|--------------|-----------------------------------------|
| OK      | NodoInviaRPT without Stamp | NodoInviaRPT | `sh run.sh ENV rpt_ok_nostamp SUBKEY`   |
| KO      | NodoInviaRPT with Stamp    | NodoInviaRPT | `sh run.sh ENV rpt_ko_withstamp SUBKEY` |
