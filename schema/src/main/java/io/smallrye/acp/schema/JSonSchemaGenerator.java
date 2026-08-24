package io.smallrye.acp.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Code generator that produces Java records and enums from the ACP JSON Schema.
 *
 * <p>Reads the schema definition from a classpath resource (default: {@code /schema/acp/v1/schema.json})
 * and generates one {@code .java} file per type. The version segment (e.g. {@code v1}) is
 * derived from the schema path and appended to the base package name.
 *
 * <p>Supported schema constructs:
 * <ul>
 *   <li><b>Objects with properties</b> &rarr; Java {@code record} with Jackson annotations</li>
 *   <li><b>String enums</b> ({@code "enum"} array) &rarr; Java {@code enum} with {@code @JsonValue}</li>
 *   <li><b>oneOf with const values</b> &rarr; Java {@code enum}</li>
 *   <li><b>String/integer type aliases</b> &rarr; skipped (mapped to {@code String}/{@code Integer})</li>
 *   <li><b>anyOf union types</b> &rarr; skipped (use {@code Object} at call sites)</li>
 * </ul>
 *
 * <p>System properties:
 * <ul>
 *   <li>{@code -DschemaPath=/schema/acp/v1/schema.json} &mdash; classpath resource path (default: {@code /schema/acp/v1/schema.json})</li>
 *   <li>{@code -DbasePackage=com.example.schema} &mdash; base package; version is appended automatically (default: {@code io.smallrye.agentclientprotocol.sdk.spec.schema})</li>
 *   <li>{@code -DoutputDir=target/generated-sources} &mdash; output root directory for generated files (default: {@code target/generated-sources})</li>
 * </ul>
 *
 * <p>The version is extracted from the schema path: for {@code /schema/acp/v1/schema.json}
 * the version is {@code v1}, producing package {@code io.smallrye.agentclientprotocol.sdk.spec.schema.v1}.
 *
 * <p>Generated files are written to {@code target/generated-sources} by default, so the user
 * can review them before copying or merging into the main source tree.
 *
 * <p>Run as a standalone program (from the {@code schema} module):
 * <pre>{@code
 * mvn compile exec:java -Dexec.mainClass=io.smallrye.acp.schema.JSonSchemaGenerator -pl schema
 * mvn compile exec:java -Dexec.mainClass=io.smallrye.acp.schema.JSonSchemaGenerator -pl schema \
 *     -DschemaPath=/schema/acp/v2/schema.json
 * mvn compile exec:java -Dexec.mainClass=io.smallrye.acp.schema.JSonSchemaGenerator -pl schema \
 *     -DschemaPath=/schema/acp/v2/schema.json -DbasePackage=com.example.acp.schema
 * mvn compile exec:java -Dexec.mainClass=io.smallrye.acp.schema.JSonSchemaGenerator -pl schema \
 *     -DoutputDir=src/main/java
 * }</pre>
 */
public class JSonSchemaGenerator {

    private static final String DEFAULT_SCHEMA_PATH = "/schema/acp/v1/schema.json";
    private static final String DEFAULT_BASE_PACKAGE = "io.smallrye.agentclientprotocol.sdk.spec.schema";
    private static final String DEFAULT_OUTPUT_DIR = "target/generated-sources";

    private static String targetPackage;
    private static JsonNode allDefs;

    /**
     * Reads the ACP JSON Schema and generates Java source files.
     *
     * <p>The schema path and base package can be configured via system properties:
     * <ul>
     *   <li>{@code -DschemaPath} &mdash; classpath path to the JSON schema</li>
     *   <li>{@code -DbasePackage} &mdash; base Java package (version is appended from the schema path)</li>
     *   <li>{@code -DoutputDir} &mdash; output root directory (default: {@code target/generated-sources})</li>
     * </ul>
     *
     * @param args unused
     * @throws IOException if the schema cannot be read or files cannot be written
     */
    public static void main(String[] args) throws IOException {
        String schemaPath = System.getProperty("schemaPath", DEFAULT_SCHEMA_PATH);
        String basePackage = System.getProperty("basePackage", DEFAULT_BASE_PACKAGE);
        Path outputDir = Path.of(System.getProperty("outputDir", DEFAULT_OUTPUT_DIR));

        // Derive version from the schema path: /schema/acp/v1/schema.json -> v1
        String version = extractVersion(schemaPath);
        targetPackage = basePackage + "." + version;

        System.out.println("Schema path:    " + schemaPath);
        System.out.println("Target package: " + targetPackage);
        System.out.println("Output dir:     " + outputDir);

        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = JSonSchemaGenerator.class.getResourceAsStream(schemaPath)) {
            if (is == null) {
                System.err.println("ERROR: Schema not found on classpath: " + schemaPath);
                System.exit(1);
            }
            JsonNode root = mapper.readTree(is);
            allDefs = root.get("$defs");

            Path packageDir = outputDir.resolve(targetPackage.replace('.', '/'));
            Files.createDirectories(packageDir);

            int count = 0;
            Iterator<Map.Entry<String, JsonNode>> fields = allDefs.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String name = entry.getKey();
                JsonNode schema = entry.getValue();

                String javaCode = generateType(name, schema);
                if (javaCode != null) {
                    Files.writeString(packageDir.resolve(name + ".java"), javaCode);
                    System.out.println("Generated: " + name);
                    count++;
                }
            }
            System.out.println("Done! Generated " + count + " files.");
        }
    }

    /**
     * Extracts the version segment from a schema classpath path.
     * For example: {@code /schema/acp/v1/schema.json} &rarr; {@code v1}
     *
     * @param schemaPath the classpath resource path
     * @return the version segment
     */
    private static String extractVersion(String schemaPath) {
        // Split path segments and find the parent directory of schema.json
        // /schema/acp/v1/schema.json -> ["", "schema", "acp", "v1", "schema.json"]
        String[] segments = schemaPath.split("/");
        if (segments.length >= 2) {
            return segments[segments.length - 2]; // "v1"
        }
        return "v1";
    }

    /**
     * Generates the Java source for a single schema definition, or {@code null} if the
     * type should be skipped (e.g. string aliases, union types).
     *
     * @param name   the schema definition name (becomes the Java type name)
     * @param schema the JSON Schema node
     * @return the generated Java source, or {@code null}
     */
    private static String generateType(String name, JsonNode schema) {
        // String type aliases -> skip (just use String)
        if (isStringAlias(schema)) {
            return null;
        }
        // Integer type aliases -> skip (just use Integer)
        if (isIntegerAlias(schema)) {
            return null;
        }
        // String enum (has "enum" array)
        if (schema.has("enum") && "string".equals(getSimpleType(schema))) {
            return generateStringEnum(name, schema);
        }
        // oneOf with const values -> string enum
        if (schema.has("oneOf") && isConstEnum(schema.get("oneOf"))) {
            return generateOneOfEnum(name, schema);
        }
        // Object type -> record
        if ("object".equals(getSimpleType(schema)) && schema.has("properties")) {
            return generateRecord(name, schema);
        }
        // anyOf union types -> skip (use Object or sealed interface in future)
        return null;
    }

    /**
     * Generates a Java enum from a schema with an {@code "enum"} array of string values.
     */
    private static String generateStringEnum(String name, JsonNode schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(targetPackage).append(";\n\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonValue;\n\n");
        appendJavadoc(sb, schema, "");
        sb.append("public enum ").append(name).append(" {\n");

        JsonNode values = schema.get("enum");
        for (int i = 0; i < values.size(); i++) {
            String val = values.get(i).asText();
            String enumConst = toEnumConstant(val);
            sb.append("    ").append(enumConst).append("(\"").append(val).append("\")");
            sb.append(i < values.size() - 1 ? ",\n" : ";\n");
        }

        sb.append("\n    private final String value;\n\n");
        sb.append("    ").append(name).append("(String value) {\n");
        sb.append("        this.value = value;\n");
        sb.append("    }\n\n");
        sb.append("    @JsonValue\n");
        sb.append("    public String getValue() {\n");
        sb.append("        return value;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Generates a Java enum from a {@code oneOf} schema where each option has a {@code const} value
     * (e.g. {@code StopReason}, {@code ToolCallStatus}).
     */
    private static String generateOneOfEnum(String name, JsonNode schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(targetPackage).append(";\n\n");
        sb.append("import com.fasterxml.jackson.annotation.JsonValue;\n\n");
        appendJavadoc(sb, schema, "");
        sb.append("public enum ").append(name).append(" {\n");

        JsonNode oneOf = schema.get("oneOf");
        List<String> entries = new ArrayList<>();
        for (JsonNode option : oneOf) {
            if (option.has("const")) {
                String val = option.get("const").asText();
                String enumConst = toEnumConstant(val);
                entries.add("    " + enumConst + "(\"" + val + "\")");
            }
        }
        sb.append(String.join(",\n", entries)).append(";\n");

        sb.append("\n    private final String value;\n\n");
        sb.append("    ").append(name).append("(String value) {\n");
        sb.append("        this.value = value;\n");
        sb.append("    }\n\n");
        sb.append("    @JsonValue\n");
        sb.append("    public String getValue() {\n");
        sb.append("        return value;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Generates a Java {@code record} from an object schema with properties.
     * Produces a canonical constructor with all fields and, when optional fields exist,
     * a convenience constructor with only the required fields.
     */
    private static String generateRecord(String name, JsonNode schema) {
        JsonNode properties = schema.get("properties");
        Set<String> required = new LinkedHashSet<>();
        if (schema.has("required")) {
            for (JsonNode r : schema.get("required")) {
                required.add(r.asText());
            }
        }

        // Collect all fields
        List<FieldInfo> allFields = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> props = properties.fields();
        while (props.hasNext()) {
            Map.Entry<String, JsonNode> prop = props.next();
            String propName = prop.getKey();
            JsonNode propSchema = prop.getValue();
            String javaType = resolveJavaType(propSchema);
            String fieldName = toJavaFieldName(propName);
            boolean isRequired = required.contains(propName);
            allFields.add(new FieldInfo(propName, fieldName, javaType, isRequired));
        }

        // Separate required and optional fields
        List<FieldInfo> requiredFields = allFields.stream().filter(f -> f.required).toList();
        List<FieldInfo> optionalFields = allFields.stream().filter(f -> !f.required).toList();

        // Collect imports
        Set<String> imports = new TreeSet<>();
        imports.add("com.fasterxml.jackson.annotation.JsonProperty");
        for (FieldInfo f : allFields) {
            if (f.javaType.startsWith("List<")) {
                imports.add("java.util.List");
            }
            if (f.javaType.startsWith("Map<") || f.javaType.equals("Map<String, Object>")) {
                imports.add("java.util.Map");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(targetPackage).append(";\n\n");
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n");

        appendJavadoc(sb, schema, "");
        sb.append("public record ").append(name).append("(\n");

        // All fields as record components
        for (int i = 0; i < allFields.size(); i++) {
            FieldInfo f = allFields.get(i);
            sb.append("        @JsonProperty(\"").append(f.jsonName).append("\") ").append(f.javaType).append(" ").append(f.fieldName);
            sb.append(i < allFields.size() - 1 ? ",\n" : "") ;
        }
        sb.append(") {\n");

        // Convenience constructor with only required fields (if there are optional fields)
        if (!optionalFields.isEmpty() && !requiredFields.isEmpty()) {
            sb.append("    public ").append(name).append("(");
            for (int i = 0; i < requiredFields.size(); i++) {
                FieldInfo f = requiredFields.get(i);
                sb.append(f.javaType).append(" ").append(f.fieldName);
                if (i < requiredFields.size() - 1) sb.append(", ");
            }
            sb.append(") {\n");
            sb.append("        this(");
            for (int i = 0; i < allFields.size(); i++) {
                FieldInfo f = allFields.get(i);
                sb.append(f.required ? f.fieldName : "null");
                if (i < allFields.size() - 1) sb.append(", ");
            }
            sb.append(");\n");
            sb.append("    }\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Resolves a JSON Schema property definition to a Java type string.
     * Handles {@code $ref}, {@code allOf}, {@code anyOf}, arrays, and primitive types.
     *
     * @param propSchema the property schema node
     * @return the Java type name (e.g. {@code "String"}, {@code "List<ToolCallLocation>"})
     */
    private static String resolveJavaType(JsonNode propSchema) {
        // $ref
        if (propSchema.has("$ref")) {
            return refToClassName(propSchema.get("$ref").asText());
        }

        // allOf with single $ref
        if (propSchema.has("allOf")) {
            JsonNode allOf = propSchema.get("allOf");
            if (allOf.size() == 1 && allOf.get(0).has("$ref")) {
                String refType = refToClassName(allOf.get(0).get("$ref").asText());
                return resolveTypeAlias(refType);
            }
        }

        // anyOf with $ref + null -> nullable reference
        if (propSchema.has("anyOf")) {
            JsonNode anyOf = propSchema.get("anyOf");
            for (JsonNode option : anyOf) {
                if (option.has("$ref")) {
                    String refType = refToClassName(option.get("$ref").asText());
                    return resolveTypeAlias(refType);
                }
            }
        }

        // array
        if (propSchema.has("type") && isTypeOrContains(propSchema, "array")) {
            if (propSchema.has("items")) {
                String itemType = resolveJavaType(propSchema.get("items"));
                return "List<" + boxType(itemType) + ">";
            }
            return "List<Object>";
        }

        // simple types
        String type = getSimpleType(propSchema);
        if (type != null) {
            return switch (type) {
                case "string" -> "String";
                case "integer" -> "Integer";
                case "number" -> "Double";
                case "boolean" -> "Boolean";
                case "object" -> {
                    if (propSchema.has("additionalProperties")) {
                        yield "Map<String, Object>";
                    }
                    yield "Object";
                }
                default -> "Object";
            };
        }

        return "Object";
    }

    // Resolve type aliases (string/integer typedefs) to their Java primitive wrapper
    private static String resolveTypeAlias(String typeName) {
        if (allDefs.has(typeName)) {
            JsonNode def = allDefs.get(typeName);
            if (isStringAlias(def)) return "String";
            if (isIntegerAlias(def)) return "Integer";
        }
        return typeName;
    }

    private static String refToClassName(String ref) {
        // "#/$defs/Foo" -> "Foo"
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    private static String boxType(String type) {
        return switch (type) {
            case "int" -> "Integer";
            case "double" -> "Double";
            case "boolean" -> "Boolean";
            default -> type;
        };
    }

    // --- Helpers ---
    private static boolean isStringAlias(JsonNode schema) {
        return "string".equals(getSimpleType(schema)) && !schema.has("enum");
    }

    private static boolean isIntegerAlias(JsonNode schema) {
        return "integer".equals(getSimpleType(schema)) && !schema.has("enum");
    }

    private static boolean isConstEnum(JsonNode options) {
        for (JsonNode option : options) {
            if (option.has("const") && "string".equals(getSimpleType(option))) {
                return true;
            }
        }
        return false;
    }

    private static String getSimpleType(JsonNode schema) {
        if (!schema.has("type")) return null;
        JsonNode typeNode = schema.get("type");
        if (typeNode.isTextual()) return typeNode.asText();
        if (typeNode.isArray()) {
            for (JsonNode t : typeNode) {
                if (!"null".equals(t.asText())) return t.asText();
            }
        }
        return null;
    }

    private static boolean isTypeOrContains(JsonNode schema, String type) {
        JsonNode typeNode = schema.get("type");
        if (typeNode.isTextual()) return type.equals(typeNode.asText());
        if (typeNode.isArray()) {
            for (JsonNode t : typeNode) {
                if (type.equals(t.asText())) return true;
            }
        }
        return false;
    }

    private static String toJavaFieldName(String jsonName) {
        if (jsonName.startsWith("_")) {
            return jsonName.substring(1);
        }
        return jsonName;
    }

    private static String toEnumConstant(String value) {
        // e.g., "end_turn" -> "END_TURN", "assistant" -> "ASSISTANT"
        return value.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase().replace("-", "_");
    }

    private static void appendJavadoc(StringBuilder sb, JsonNode schema, String indent) {
        if (schema.has("description")) {
            String desc = schema.get("description").asText();
            sb.append(indent).append("/**\n");
            for (String line : desc.split("\n")) {
                sb.append(indent).append(" * ").append(line).append("\n");
            }
            sb.append(indent).append(" */\n");
        }
    }

    private record FieldInfo(String jsonName, String fieldName, String javaType, boolean required) {}
}
