package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.yaml.schema.beans.SchemaConstants.ALL_OF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.ARRAY_TYPE_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.NUMBER_TYPE_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.OBJECT_TYPE_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.ONE_OF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.PROPERTIES_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.REF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.STRING_TYPE_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.TYPE_NODE;

import io.harness.exception.InvalidRequestException;
import io.harness.reflection.CodeUtils;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.OneOfMapping;
import io.harness.yaml.schema.beans.SchemaConstants;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.SupportedPossibleFieldTypes;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.schema.beans.YamlSchemaConfiguration;
import io.harness.yaml.utils.YamlConstants;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.models.Model;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class YamlSchemaGenerator {
  JacksonClassHelper jacksonSubtypeHelper;
  SwaggerGenerator swaggerGenerator;

  /**
   * @param yamlSchemaConfiguration Configuration for generation of YamlSchema
   */
  public void generateYamlSchemaFiles(YamlSchemaConfiguration yamlSchemaConfiguration) {
    final Set<Class<?>> rootSchemaClasses = getClassesForYamlSchemaGeneration(yamlSchemaConfiguration);
    SwaggerGenerator swaggerGenerator = new io.harness.yaml.schema.SwaggerGenerator();

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

    final Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap = getSwaggerMetaInfo(rootSchemaClass);
    log.info("Generated metainfo");

    final String entitySwaggerName = YamlSchemaUtils.getSwaggerName(rootSchemaClass);
    final String entityName = YamlSchemaUtils.getEntityName(rootSchemaClass);

    log.info("Generating yaml schema for {}", entityName);
    final String pathForSchemaStorageForEntity =
        getPathToStoreSchema(yamlSchemaConfiguration, rootSchemaClass, entityName);
    final Map<String, JsonNode> stringJsonNodeMap = generateDefinitions(
        stringModelMap, pathForSchemaStorageForEntity, swaggerDefinitionsMetaInfoMap, entitySwaggerName);
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
  String getPathToStoreSchema(
      YamlSchemaConfiguration yamlSchemaConfiguration, Class<?> rootSchemaClass, String entityName) {
    List<String> locationList =
        Arrays.asList(Preconditions.checkNotNull(CodeUtils.location(rootSchemaClass)).split("/"));
    String moduleBasePath = "";
    // Bazel do not have target folder, so tha path is directly coming from .m2.
    // Instead of target we are using 0.0.1-SNAPSHOT to handle bazel build modules.
    if (locationList.contains("0.0.1-SNAPSHOT")) {
      moduleBasePath = locationList.get(locationList.indexOf("0.0.1-SNAPSHOT") - 1) + "/src/main/resources/";
    } else {
      moduleBasePath = locationList.get(locationList.indexOf("target") - 1) + "/src/main/resources/";
    }

    return moduleBasePath + yamlSchemaConfiguration.getGeneratedPathRoot() + File.separator + entityName;
  }

  @VisibleForTesting
  Set<Class<?>> getClassesForYamlSchemaGeneration(YamlSchemaConfiguration yamlSchemaConfiguration) {
    return YamlSchemaUtils.getClasses(yamlSchemaConfiguration.getClassLoader(), YamlSchemaRoot.class);
  }

  /**
   * @param definitions                   swagger generated definitions.
   * @param basePath                      path where schema will be stored.
   * @param swaggerDefinitionsMetaInfoMap extra info which will be added to definitions.
   * @param baseNodeKey                   The root yaml entity name in definition for which schema is being generated.
   * @return map of file path and the the json node of schema.
   */
  @VisibleForTesting
  Map<String, JsonNode> generateDefinitions(Map<String, Model> definitions, String basePath,
      Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap, String baseNodeKey) {
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
        convertSwaggerToJsonSchema(swaggerDefinitionsMetaInfoMap, mapper, name, value);
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

  private void convertSwaggerToJsonSchema(Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap,
      ObjectMapper mapper, String name, ObjectNode value) {
    value.put(SchemaConstants.SCHEMA_NODE, SchemaConstants.JSON_SCHEMA_7);
    if (swaggerDefinitionsMetaInfoMap.containsKey(name)) {
      final SwaggerDefinitionsMetaInfo swaggerDefinitionsMetaInfo = swaggerDefinitionsMetaInfoMap.get(name);
      List<ObjectNode> allOfNodeContents = new ArrayList<>();
      // conditionals
      if (!isEmpty(swaggerDefinitionsMetaInfo.getSubtypeClassMap())) {
        addConditionalAndCleanupFields(swaggerDefinitionsMetaInfoMap, mapper, name, value, allOfNodeContents);
      }
      // oneof mapping
      if (!isEmpty(swaggerDefinitionsMetaInfo.getOneOfMappings())) {
        addExtraRequiredNodes(mapper, swaggerDefinitionsMetaInfo, allOfNodeContents);
      }
      // field multiple property
      if (!isEmpty(swaggerDefinitionsMetaInfo.getFieldPossibleTypes())) {
        addPossibleValuesInFields(mapper, value, swaggerDefinitionsMetaInfo);
      }

      if (isNotEmpty(allOfNodeContents)) {
        if (value.has(SchemaConstants.ALL_OF_NODE)) {
          final ArrayNode allOfNode = (ArrayNode) value.findValue(SchemaConstants.ALL_OF_NODE);
          allOfNode.addAll(allOfNodeContents);
        } else {
          value.putArray(SchemaConstants.ALL_OF_NODE).addAll(allOfNodeContents);
        }
      }
    }

    removeUnwantedNodes(value, "originalRef");
  }

  private void addPossibleValuesInFields(
      ObjectMapper mapper, ObjectNode value, SwaggerDefinitionsMetaInfo swaggerDefinitionsMetaInfo) {
    ObjectNode propertiesNode = getPropertiesNodeFromDefinitionNode(value);
    swaggerDefinitionsMetaInfo.getFieldPossibleTypes().forEach(fieldPossibleTypes -> {
      final String fieldName = fieldPossibleTypes.getFieldName();
      final SupportedPossibleFieldTypes defaultFieldType = fieldPossibleTypes.getDefaultFieldType();
      final ObjectNode fieldNode = (ObjectNode) propertiesNode.get(fieldName);
      List<ObjectNode> fieldOneOfNodes = new ArrayList<>();
      if (fieldNode != null) {
        final ObjectNode currentFieldNodeValue = fieldNode.deepCopy();

        fieldPossibleTypes.getFieldTypes().forEach(type -> {
          final ObjectNode nodeFromType = getNodeFromType(type, mapper);
          if (nodeFromType == null) {
            return;
          }
          if (type.equals(defaultFieldType)) {
            fieldOneOfNodes.add(0, nodeFromType);
          } else {
            fieldOneOfNodes.add(nodeFromType);
          }
        });

        if (defaultFieldType == SupportedPossibleFieldTypes.none) {
          fieldOneOfNodes.add(0, currentFieldNodeValue);
        } else {
          fieldOneOfNodes.add(currentFieldNodeValue);
        }
        fieldNode.removeAll();
        fieldNode.putArray(ONE_OF_NODE).addAll(fieldOneOfNodes);
      }
    });
  }

  private ObjectNode getPropertiesNodeFromDefinitionNode(ObjectNode value) {
    // In case of child classes they contain parent node information in 0 index of all of.
    // assuming index 1 to have properties node.
    // later if we find a corner case we will have to iterate over all the nodes to find properties node and see if it
    // works.
    ObjectNode propertiesNode;
    if (value.get(ALL_OF_NODE) != null) {
      propertiesNode = (ObjectNode) value.get(ALL_OF_NODE).get(1).get(PROPERTIES_NODE);
    } else {
      propertiesNode = (ObjectNode) value.get(PROPERTIES_NODE);
    }
    return propertiesNode;
  }

  private ObjectNode getNodeFromType(SupportedPossibleFieldTypes type, ObjectMapper mapper) {
    ObjectNode objectNode = mapper.createObjectNode();
    switch (type) {
      case string:
        objectNode.put(TYPE_NODE, STRING_TYPE_NODE);
        return objectNode;
      case number:
        objectNode.put(TYPE_NODE, NUMBER_TYPE_NODE);
        return objectNode;
      case map:
        objectNode.put(TYPE_NODE, OBJECT_TYPE_NODE);
        objectNode.putObject(SchemaConstants.ADDITIONAL_PROPERTIES_NODE).put(TYPE_NODE, STRING_TYPE_NODE);
        return objectNode;
      case list:
        objectNode.put(TYPE_NODE, ARRAY_TYPE_NODE);
        return objectNode;
      case none:
        return null;
      default:
        throw new InvalidRequestException("Unknown type for generating object node for a type");
    }
  }

  private void addExtraRequiredNodes(
      ObjectMapper mapper, SwaggerDefinitionsMetaInfo swaggerDefinitionsMetaInfo, List<ObjectNode> allOfNodeContents) {
    final Set<OneOfMapping> oneOfMappings = swaggerDefinitionsMetaInfo.getOneOfMappings();
    final List<ObjectNode> allOfNodeRequiredContent =
        oneOfMappings.stream()
            .map(mapping -> {
              final List<ObjectNode> requiredNodes =
                  mapping.getOneOfFieldNames()
                      .stream()
                      .map(field -> {
                        ObjectNode objectNode = mapper.createObjectNode();
                        objectNode.putArray(SchemaConstants.REQUIRED_NODE).add(field);
                        return objectNode;
                      })
                      .collect(Collectors.toList());

              if (mapping.isNullable()) {
                // not handled.
              }
              return requiredNodes;
            })
            .map(oneOfMapContent -> {
              ObjectNode oneOfNode = mapper.createObjectNode();
              oneOfNode.putArray(SchemaConstants.ONE_OF_NODE).addAll(oneOfMapContent);
              return oneOfNode;
            })
            .collect(Collectors.toList());
    allOfNodeContents.addAll(allOfNodeRequiredContent);
  }

  private void addConditionalAndCleanupFields(Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap,
      ObjectMapper mapper, String name, ObjectNode value, List<ObjectNode> allOfNodeContents) {
    final Set<FieldSubtypeData> fieldSubtypeDatas = swaggerDefinitionsMetaInfoMap.get(name).getSubtypeClassMap();
    for (FieldSubtypeData fieldSubtypeData : fieldSubtypeDatas) {
      if (fieldSubtypeData.getDiscriminatorType() == JsonTypeInfo.As.EXTERNAL_PROPERTY) {
        addConditionalBlock(mapper, allOfNodeContents, fieldSubtypeData);
        removeFieldWithRefFromSchema(value, fieldSubtypeData);
      } else {
        addInternalConditionalBlock(value, mapper, fieldSubtypeData);
      }
    }
  }

  // todo(abhinav): solve it to handle conditional better.
  private void addInternalConditionalBlock(ObjectNode value, ObjectMapper mapper, FieldSubtypeData fieldSubtypeData) {
    ObjectNode propertiesNodeFromDefinitionNode = getPropertiesNodeFromDefinitionNode(value);
    final String fieldName = fieldSubtypeData.getFieldName();
    final ObjectNode fieldNode = (ObjectNode) propertiesNodeFromDefinitionNode.get(fieldName);
    if (fieldNode.get(ONE_OF_NODE) != null) {
      throw new InvalidRequestException("Both Subtype and one of not handled for a single field");
    }
    List<ObjectNode> possibleNodes =
        fieldSubtypeData.getSubtypesMapping()
            .stream()
            .map(fieldData
                -> mapper.createObjectNode().put(
                    REF_NODE, SchemaConstants.DEFINITIONS_STRING_PREFIX + fieldData.getSubTypeDefinitionKey()))
            .collect(Collectors.toList());
    fieldNode.removeAll();
    fieldNode.putArray(ONE_OF_NODE).addAll(possibleNodes);
  }

  private void removeFieldWithRefFromSchema(ObjectNode value, FieldSubtypeData fieldSubtypeData) {
    final String fieldName = fieldSubtypeData.getFieldName();
    ObjectNode propertiesNode = (ObjectNode) value.findValue(PROPERTIES_NODE);
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
   * @param mapper            object mapper.
   * @param allOfNodeContents the conditional block in which new condition is to be added.
   * @param fieldSubtypeData  the subtype mapping for fields which will be added to conditionals.
   *                          Adds conditional block to the single definition.
   *                          Currently only handled for {@link JsonTypeInfo.As#EXTERNAL_PROPERTY}
   */
  private void addConditionalBlock(
      ObjectMapper mapper, List<ObjectNode> allOfNodeContents, FieldSubtypeData fieldSubtypeData) {
    for (SubtypeClassMap subtypeClassMap : fieldSubtypeData.getSubtypesMapping()) {
      ObjectNode ifElseBlock = mapper.createObjectNode();
      ifElseBlock.with(SchemaConstants.IF_NODE)
          .with(PROPERTIES_NODE)
          .with(fieldSubtypeData.getDiscriminatorName())
          .put(SchemaConstants.CONST_NODE, subtypeClassMap.getSubtypeEnum());
      ifElseBlock.with(SchemaConstants.THEN_NODE)
          .with(PROPERTIES_NODE)
          .with(fieldSubtypeData.getFieldName())
          .put(SchemaConstants.REF_NODE,
              SchemaConstants.DEFINITIONS_STRING_PREFIX + subtypeClassMap.getSubTypeDefinitionKey());
      allOfNodeContents.add(ifElseBlock);
    }
  }

  @VisibleForTesting
  Map<String, SwaggerDefinitionsMetaInfo> getSwaggerMetaInfo(Class<?> clazz) {
    Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap = new HashMap<>();
    jacksonSubtypeHelper.getRequiredMappings(clazz, swaggerDefinitionsMetaInfoMap);
    return swaggerDefinitionsMetaInfoMap;
  }
}
