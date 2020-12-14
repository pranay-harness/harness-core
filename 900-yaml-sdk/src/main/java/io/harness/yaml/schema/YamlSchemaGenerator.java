package io.harness.yaml.schema;

import io.harness.exception.InvalidRequestException;
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
import java.util.Iterator;
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
    SwaggerGenerator swaggerGenerator = new SwaggerGenerator();

    for (Class<?> rootSchemaClass : rootSchemaClasses) {
      generateJsonSchemaForRootClass(yamlSchemaConfiguration, swaggerGenerator, rootSchemaClass);
    }
  }

  @VisibleForTesting
  void generateJsonSchemaForRootClass(
      YamlSchemaConfiguration yamlSchemaConfiguration, SwaggerGenerator swaggerGenerator, Class<?> rootSchemaClass) {
    log.info("Generating swagger for {}", rootSchemaClass.toString());
    final Map<String, Model> stringModelMap = swaggerGenerator.generateDefinitions(rootSchemaClass);
    log.info("Generated swagger");

    final Map<String, Set<FieldSubtypeData>> subTypeMap = getSubTypeMap(rootSchemaClass);
    log.info("Generated subtype Map");

    final String entitySwaggerName = YamlSchemaUtils.getSwaggerName(rootSchemaClass);
    final String entityName = YamlSchemaUtils.getEntityName(rootSchemaClass);
    log.info("Generating yaml schema for {}", entityName);
    final Map<String, JsonNode> stringJsonNodeMap = generateDefinitions(stringModelMap,
        yamlSchemaConfiguration.getGeneratedPathRoot() + File.separator + entityName, subTypeMap, entitySwaggerName);
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    DefaultPrettyPrinter defaultPrettyPrinter = new SchemaGeneratorUtils.SchemaPrinter();
    ObjectWriter jsonWriter = mapper.writer(defaultPrettyPrinter);
    stringJsonNodeMap.forEach((filePath, content) -> {
      try {
        writeContentsToFile(filePath, content, jsonWriter);
      } catch (IOException e) {
        throw new InvalidRequestException("Cannot save file", e);
      }
    });
    log.info("Saved files");
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
   * @return map of file path and the the json node of schema.
   */
  @VisibleForTesting
  Map<String, JsonNode> generateDefinitions(Map<String, Model> definitions, String basePath,
      Map<String, Set<FieldSubtypeData>> subTypeMap, String baseNodeKey) {
    Map<String, JsonNode> filePathContentMap = new HashMap<>();
    ObjectMapper mapper = SchemaGeneratorUtils.getObjectMapperForSchemaGeneration();
    try {
      final String definitionsJson = mapper.writeValueAsString(definitions.entrySet());

      ArrayNode definitionsArrayNode = (ArrayNode) mapper.readTree(definitionsJson);
      ObjectNode modifiedNode = mapper.createObjectNode();

      for (JsonNode node : definitionsArrayNode) {
        // Assuming only single node in next
        final String name = node.fieldNames().next();
        ObjectNode value = (ObjectNode) node.get(name);
        convertSwaggerToJsonSchema(subTypeMap, mapper, name, value);
        filePathContentMap.put(basePath + File.separator + name + YamlConstants.JSON_EXTENSION, value);
        modifiedNode.with(name).setAll(value);
      }

      ObjectNode outputNode = mapper.createObjectNode();
      generateCompleteSchema(baseNodeKey, modifiedNode, outputNode);
      filePathContentMap.put(basePath + File.separator + YamlConstants.SCHEMA_FILE_NAME, outputNode);
    } catch (IOException e) {
      log.error("Error in generating Yaml Schema.", e);
    }
    return filePathContentMap;
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
        removeFieldWithRefFromSchema(value, fieldSubtypeData);
      }
      value.putArray(SchemaConstants.ALL_OF_NODE).addAll(allConditionals);
    }
    removeUnwantedNodes(value, "originalRef");
  }

  private void removeFieldWithRefFromSchema(ObjectNode value, FieldSubtypeData fieldSubtypeData) {
    final String fieldName = fieldSubtypeData.getFieldName();
    ObjectNode propertiesNode = (ObjectNode) value.findValue(SchemaConstants.PROPERTIES_NODE);
    propertiesNode.remove(fieldName);
  }

  private void removeUnwantedNodes(JsonNode objectNode, String unwantedNode) {
    if (objectNode.isArray()) {
      final Iterator<JsonNode> elements = objectNode.elements();
      while (elements.hasNext()) {
        final JsonNode node = elements.next();
        if (node.isArray()) {
          ArrayNode arrayNode = (ArrayNode) node;
          final Iterator<JsonNode> nodeIterator = arrayNode.elements();
          while (nodeIterator.hasNext()) {
            JsonNode nextNode = nodeIterator.next();
            removeUnwantedNodes(nextNode, unwantedNode);
          }
        } else if (node.isObject()) {
          removeUnwantedNodes(node, unwantedNode);
        }
      }
    } else if (objectNode.isObject()) {
      ObjectNode node = (ObjectNode) objectNode;
      node.remove(unwantedNode);
      final Iterator<JsonNode> elements = node.elements();
      while (elements.hasNext()) {
        removeUnwantedNodes(elements.next(), unwantedNode);
      }
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

  @VisibleForTesting
  Map<String, Set<FieldSubtypeData>> getSubTypeMap(Class<?> clazz) {
    Map<String, Set<FieldSubtypeData>> classSubtypesMap = new HashMap<>();
    JacksonSubtypeHelper jacksonSubtypeHelper = new JacksonSubtypeHelper();
    jacksonSubtypeHelper.getSubtypeMapping(clazz, classSubtypesMap);
    return classSubtypesMap;
  }
}
