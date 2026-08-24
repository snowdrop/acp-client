# Contributing

Thank you for your interest in contributing to the ACP Java Client.

## Prerequisites

- [JDK 21+](https://openjdk.org/)
- [Apache Maven 3.9+](https://maven.apache.org/)
- [Git](https://git-scm.com/)

## Building locally

Clone the repository and build:

```shell
git clone <repository-url>
cd acp
mvn clean package
```

## Testing locally

Run the client against an ACP-compatible agent:

```shell
# Default: OpenCode with Zen (no API key needed)
mvn exec:exec -pl client

# With a specific agent and prompt
mvn exec:exec -pl client -DacpAgentBinary="claude-agent-acp" -Dprompt="Say Hello"

# With debug logging to see protocol details
mvn exec:exec -pl client -DlogLevel=DEBUG
```

See [README.md](README.md) for all available parameters, providers, and logging options.

## Project structure

| Module   | Artifact ID          | Description |
|----------|----------------------|-------------|
| `schema` | `acp-client-schema`  | ACP JSON Schema, generated Java records/enums, and `JSonSchemaGenerator` |
| `core`   | `acp-client-core`    | ACP client library (sync and async clients, stdio transport) |
| `client` | `acp-client-cli`     | CLI example (`AcpAgentCli`), skills, and sandbox |

## Submitting a pull request

1. Fork the repository and create a branch from `main`:
   ```shell
   git checkout -b my-feature
   ```

2. Make your changes and verify the build compiles:
   ```shell
   mvn clean package
   ```

3. Test your changes against at least one ACP agent:
   ```shell
   mvn exec:exec -pl client -Dprompt="Say Hello"
   ```

4. Commit your changes with a clear message describing what and why:
   ```shell
   git commit -m "Add support for XYZ"
   ```

5. Push your branch and open a pull request against `main`.

## Generating Java classes from the ACP JSON Schema

The `schema` module includes `JSonSchemaGenerator`, a custom code generator that produces Java records and enums from the ACP JSON Schema specification. It replaces the need for external tools like jsonschema2pojo.

### Schema location

The ACP JSON schema is located at:

```
schema/src/main/resources/schema/acp/v1/schema.json
```

### Generated output

By default, generated classes are written to `target/generated-sources` under the `schema` module. This lets you review the generated code before copying or merging it into the main source tree.

The existing (reviewed and manually amended) schema classes live in:

```
schema/src/main/java/io/smallrye/agentclientprotocol/sdk/spec/schema/v1/
```

The package name `io.smallrye.agentclientprotocol.sdk.spec.schema.v1` corresponds to the `v1` schema directory. When a `v2` spec is released, classes would be generated into `.schema.v2`.

### How to regenerate

To regenerate the Java classes after updating the schema file, run `JSonSchemaGenerator` from the `schema` module:

```shell
mvn compile exec:java -Dexec.mainClass=io.smallrye.acp.schema.JSonSchemaGenerator -pl schema
```

Generated files will be written to `schema/target/generated-sources/`. Review them and copy into `schema/src/main/java/` when ready.

To generate directly into the source tree (overwriting existing classes):

```shell
mvn compile exec:java -Dexec.mainClass=io.smallrye.acp.schema.JSonSchemaGenerator -pl schema \
    -DoutputDir=src/main/java
```

The version segment (e.g. `v1`) is automatically derived from the schema path and appended to the base package. For the default path `/schema/acp/v1/schema.json`, it generates classes in the `io.smallrye.agentclientprotocol.sdk.spec.schema.v1` package.

### Generator parameters

| System property  | Description                                                | Default                                                  |
|------------------|------------------------------------------------------------|----------------------------------------------------------|
| `-DschemaPath`   | Classpath resource path to the JSON schema                 | `/schema/acp/v1/schema.json`                             |
| `-DbasePackage`  | Base Java package; version is appended from the schema path| `io.smallrye.agentclientprotocol.sdk.spec.schema`     |
| `-DoutputDir`    | Output root directory for generated files                  | `target/generated-sources`                               |

### What it generates

| Schema construct               | Java output                                          |
|--------------------------------|------------------------------------------------------|
| Object with properties         | Java `record` with `@JsonProperty` annotations       |
| String enum (`"enum"` array)   | Java `enum` with `@JsonValue`                        |
| oneOf with const values        | Java `enum` with `@JsonValue`                        |
| String/integer type aliases    | Skipped (mapped to `String`/`Integer` at call sites) |
| anyOf union types              | Skipped (use `Object` at call sites)                 |

### Updating to a newer ACP spec version

1. Download the latest `schema.json` from the [ACP specification](https://agentclientprotocol.com/specification)
2. Place it under `schema/src/main/resources/schema/acp/<version>/schema.json` (e.g. `v2`)
3. Regenerate the classes by pointing to the new schema path:
   ```shell
   mvn compile exec:java -Dexec.mainClass=io.smallrye.acp.schema.JSonSchemaGenerator -pl schema \
       -DschemaPath=/schema/acp/v2/schema.json
   ```
   This automatically generates classes into `schema/target/generated-sources/` under the `.schema.v2` package.
4. Review the generated classes and copy them into `schema/src/main/java/`
5. Check if any manually amended records (e.g. `SessionConfigOption`, `SelectedPermissionOutcome`) need their custom fields re-applied, as the generator will overwrite them

> **Note:** Some generated records have been manually amended to add fields missing from the schema or to include discriminator properties. After regeneration, check the git diff carefully and re-apply any manual changes.
