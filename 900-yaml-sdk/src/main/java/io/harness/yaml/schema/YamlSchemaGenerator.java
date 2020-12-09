package io.harness.yaml.schema;

import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.SchemaConstants;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.YamlSchemaConfiguration;
import io.harness.yaml.utils.YamlConstants;
import io.harness.yaml.utils.YamlSchemaUtils;
import io.harness.yamlSchema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.models.Model;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class YamlSchemaGenerator {
  /**
   * @param yamlSchemaConfiguration Configuration for generation of YamlSchema
   */
  public void generateYamlSchemaFiles(YamlSchemaConfiguration yamlSchemaConfiguration) {
    final Set<Class<?>> rootSchemaClasses = getClassesForYamlSchemaGeneration(yamlSchemaConfiguration);

    for (Class<?> rootSchemaClass : rootSchemaClasses) {
      SwaggerGenerator swaggerGenerator = new SwaggerGenerator();
      log.info("Generating swagger for {}", rootSchemaClass.toString());
      final Map<String, Model> stringModelMap = swaggerGenerator.generateDefinitions(rootSchemaClass);
      log.info("Generated swagger");

      final Map<String, Set<FieldSubtypeData>> subTypeMap = getSubTypeMap(rootSchemaClass);
      log.info("Generated subtype Map");

      final String entitySwaggerName = YamlSchemaUtils.getSwaggerName(rootSchemaClass);
      final String entityName = YamlSchemaUtils.getEntityName(rootSchemaClass);
      log.info("Generating yaml schema for {}", entityName);
      generateDefinitions(stringModelMap, yamlSchemaConfiguration.getGeneratedPathRoot() + File.separator + entityName,
          subTypeMap, entitySwaggerName);
      log.info("Saved files");
    }
  }

  @VisibleForTesting
  Set<Class<?>> getClassesForYamlSchemaGeneration(YamlSchemaConfiguration yamlSchemaConfiguration) {
    return YamlSchemaUtils.getClasses(yamlSchemaConfiguration.getClassLoader(), YamlSchemaRoot.class);
  }

  /**
   * @param definitions swagger generated definitions.
   * @param basePath    path where schema will be stored.
   * @param subTypeMap  extra subtype info which will be added to definitions.
   * @param baseNodeKey The root yaml entity name in definition for which schema is being generated.
   */
  private void generateDefinitions(Map<String, Model> definitions, String basePath,
      Map<String, Set<FieldSubtypeData>> subTypeMap, String baseNodeKey) {
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    DefaultPrettyPrinter defaultPrettyPrinter = new SchemaGeneratorUtils.SchemaPrinter();
    ObjectWriter jsonWriter = mapper.writer(defaultPrettyPrinter);

    try {
      final String definitionsJson = jsonWriter.writeValueAsString(definitions.entrySet());

      ArrayNode definitionsArrayNode = (ArrayNode) mapper.readTree(definitionsJson);
      ObjectNode modifiedNode = mapper.createObjectNode();

      for (JsonNode node : definitionsArrayNode) {
        // Assuming only single node in next
        final String name = node.fieldNames().next();
        ObjectNode value = (ObjectNode) node.get(name);

        convertSwaggerToJsonSchema(subTypeMap, mapper, name, value);
        writeContentsToFile(basePath + File.separator + name + YamlConstants.JSON_EXTENSION, value, jsonWriter);
        modifiedNode.with(name).setAll(value);
      }

      ObjectNode outputNode = mapper.createObjectNode();
      generateCompleteSchema(baseNodeKey, modifiedNode, outputNode);
      writeContentsToFile(basePath + File.separator + YamlConstants.SCHEMA_FILE_NAME, outputNode, jsonWriter);
    } catch (IOException e) {
      log.error("Error in generating Yaml Schema.", e);
    }
  }

  @VisibleForTesting
  void writeContentsToFile(@NotNull String fileName, @NotNull Object content, @NotNull ObjectWriter jsonWriter)
      throws IOException {
    Files.createDirectories(Paths.get(fileName).getParent());
    FileUtils.write(new File(fileName), jsonWriter.writeValueAsString(content), StandardCharsets.UTF_8);
    log.info("Saved Json Schema at: {}", fileName);
  }

  /**
   * @param baseNodeKey  Node Key which will act as root for complete schema.
   * @param modifiedNode The modified swagger spec which contains all json schema fields and also conditionals.
   * @param outputNode   Node in which final output will be dumped.
   */
  private void generateCompleteSchema(String baseNodeKey, ObjectNode modifiedNode, ObjectNode outputNode) {
    final JsonNode value = modifiedNode.findValue(baseNodeKey);
    if (value.isArray()) {
      outputNode.setAll((ObjectNode) value.get(0));
    } else {
      outputNode.setAll((ObjectNode) value);
    }
    outputNode.with(SchemaConstants.DEFINITIONS_NODE).setAll(modifiedNode);
  }

  private void convertSwaggerToJsonSchema(
      Map<String, Set<FieldSubtypeData>> subTypeMap, ObjectMapper mapper, String name, ObjectNode value) {
    value.put(SchemaConstants.SCHEMA_NODE, SchemaConstants.JSON_SCHEMA_7);
    if (subTypeMap.containsKey(name) && !subTypeMap.get(name).isEmpty()) {
      List<ObjectNode> allConditionals = new ArrayList<>();
      final Set<FieldSubtypeData> fieldSubtypeDatas = subTypeMap.get(name);
      for (FieldSubtypeData fieldSubtypeData : fieldSubtypeDatas) {
        addConditionalBlock(mapper, allConditionals, fieldSubtypeData);
      }
      value.putArray(SchemaConstants.ALL_OF_NODE).addAll(allConditionals);
    }
  }

  /**
   * @param mapper           object mapper.
   * @param allConditionals  the conditional block in which new condition is to be added.
   * @param fieldSubtypeData the subtype mapping for fields which will be added to conditionals.
   *                         Adds conditional block to the single definition.
   *                         Currently only handled for {@link JsonTypeInfo.As#EXTERNAL_PROPERTY}
   */
  private void addConditionalBlock(
      ObjectMapper mapper, List<ObjectNode> allConditionals, FieldSubtypeData fieldSubtypeData) {
    for (SubtypeClassMap subtypeClassMap : fieldSubtypeData.getSubtypesMapping()) {
      ObjectNode ifElseBlock = mapper.createObjectNode();
      ifElseBlock.with(SchemaConstants.IF_NODE)
          .with(SchemaConstants.PROPERTIES_NODE)
          .with(fieldSubtypeData.getDiscriminatorName())
          .put(SchemaConstants.CONST_NODE, subtypeClassMap.getSubtypeEnum());
      ifElseBlock.with(SchemaConstants.THEN_NODE)
          .with(SchemaConstants.PROPERTIES_NODE)
          .with(fieldSubtypeData.getFieldName())
          .put(SchemaConstants.REF_NODE,
              SchemaConstants.DEFINITIONS_STRING_PREFIX + subtypeClassMap.getSubTypeDefinitionKey());
      allConditionals.add(ifElseBlock);
    }
  }

  private Map<String, Set<FieldSubtypeData>> getSubTypeMap(Class<?> clazz) {
    Map<String, Set<FieldSubtypeData>> classSubtypesMap = new HashMap<>();
    JacksonSubtypeHelper jacksonSubtypeHelper = new JacksonSubtypeHelper();
    jacksonSubtypeHelper.getSubtypeMapping(clazz, classSubtypesMap);
    return classSubtypesMap;
  }
}
