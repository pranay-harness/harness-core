package io.harness.pms.yaml;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.serializer.jackson.NGHarnessJacksonModule;
import io.harness.serializer.AnnotationAwareJsonSubtypeResolver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import lombok.experimental.UtilityClass;

@UtilityClass
public class YamlUtils {
  private static List<String> ignorableStringForQualifiedName = Arrays.asList("step", "parallel", "spec");

  private final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSubtypeResolver(AnnotationAwareJsonSubtypeResolver.newInstance(mapper.getSubtypeResolver()));
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new NGHarnessJacksonModule());
  }

  public <T> T read(String yaml, Class<T> cls) throws IOException {
    return mapper.readValue(yaml, cls);
  }

  public String write(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new InvalidRequestException("Couldn't convert object to Yaml");
    }
  }

  public YamlField readTree(String content) throws IOException {
    JsonNode rootJsonNode = mapper.readTree(content);
    YamlNode rootYamlNode = new YamlNode(rootJsonNode, null);
    return new YamlField(rootYamlNode);
  }

  public YamlField toByteString(String content) throws IOException {
    JsonNode rootJsonNode = mapper.readTree(content);
    YamlNode rootYamlNode = new YamlNode(rootJsonNode, null);
    return new YamlField(rootYamlNode);
  }

  public YamlField extractPipelineField(String content) throws IOException {
    YamlField rootYamlField = readTree(content);
    YamlNode rootYamlNode = rootYamlField.getNode();
    return Preconditions.checkNotNull(
        getPipelineField(rootYamlNode), "Invalid pipeline YAML: root of the yaml needs to be an object");
  }

  private YamlField getPipelineField(YamlNode rootYamlNode) {
    return (rootYamlNode == null || !rootYamlNode.isObject()) ? null : rootYamlNode.getField("pipeline");
  }

  public String injectUuid(String content) throws IOException {
    JsonNode rootJsonNode = mapper.readTree(content);
    if (rootJsonNode == null) {
      return content;
    }

    injectUuid(rootJsonNode);
    return rootJsonNode.toString();
  }

  private void injectUuid(JsonNode node) {
    if (node.isObject()) {
      injectUuidInObject(node);
    } else if (node.isArray()) {
      injectUuidInArray(node);
    }
  }

  /**
   * Get Qualified Name till the pipeline level
   * @param yamlNode
   * @return
   */
  public String getFullyQualifiedName(YamlNode yamlNode) {
    return String.join(".", getQualifiedNameList(yamlNode, "pipeline"));
  }

  /**
   *
   * Gives the qualified Name till the given field name.
   *
   * @param yamlNode
   * @param fieldName - the field till which we want the qualified name
   * @return
   */
  public String getQualifiedNameTillGivenField(YamlNode yamlNode, String fieldName) {
    return String.join(".", getQualifiedNameList(yamlNode, fieldName));
  }

  /**
   * Gets qualified Name between from and to. Starting from the given yamlNode
   * @param yamlNode
   * @param from
   * @param to
   * @return
   */
  public String getQNBetweenTwoFields(YamlNode yamlNode, String from, String to) {
    List<String> qualifiedNames = getQualifiedNameList(yamlNode, "pipeline");
    StringBuilder response = new StringBuilder();
    for (String qualifiedName : qualifiedNames) {
      if (qualifiedName.equals(from)) {
        response.append(qualifiedName).append(".");
      }
      if (qualifiedName.equals(to)) {
        response.append(qualifiedName);
        break;
      }
    }
    return response.toString();
  }

  private List<String> getQualifiedNameList(YamlNode yamlNode, String fieldName) {
    if (yamlNode.getParentNode() == null) {
      return Collections.singletonList(getQNForNode(yamlNode, null));
    }
    String qualifiedName = getQNForNode(yamlNode, yamlNode.getParentNode());
    if (EmptyPredicate.isEmpty(qualifiedName)) {
      return getQualifiedNameList(yamlNode.getParentNode(), fieldName);
    }
    if (qualifiedName.equals(fieldName)) {
      List<String> qualifiedNameList = new ArrayList<>();
      qualifiedNameList.add(qualifiedName);
      return qualifiedNameList;
    }
    if (yamlNode.getUuid() != null
        && fieldName.equals(
            getMatchingFieldNameFromParentUsingUUID(yamlNode.getParentNode(), yamlNode.getUuid()).getName())) {
      List<String> qualifiedNameList = new ArrayList<>();
      qualifiedNameList.add(qualifiedName);
      return qualifiedNameList;
    }
    List<String> qualifiedNameList = getQualifiedNameList(yamlNode.getParentNode(), fieldName);
    qualifiedNameList.add(qualifiedName);
    return qualifiedNameList;
  }

  private String getQNForNode(YamlNode yamlNode, YamlNode parentNode) {
    if (parentNode == null) {
      return "";
    }
    if (parentNode.getParentNode() != null && parentNode.getParentNode().isArray()) {
      if (yamlNode.getIdentifier() != null) {
        return yamlNode.getIdentifier();
      } else {
        return "";
      }
    }
    YamlField field;
    // Because UUID won't be there in leaf objects
    if (yamlNode.getUuid() != null) {
      field = getMatchingFieldNameFromParentUsingUUID(parentNode, yamlNode.getUuid());
    } else {
      field = getMatchingFieldNameFromParentUsingValueAsText(parentNode, yamlNode.getCurrJsonNode().asText());
    }

    if (field == null || shouldNotIncludeInQualifiedName(field.getName())) {
      return "";
    }

    return field.getName();
  }

  private boolean shouldNotIncludeInQualifiedName(String fieldName) {
    if (ignorableStringForQualifiedName.contains(fieldName)) {
      return true;
    }
    return fieldName.contains("Definition");
  }

  private void injectUuidInObject(JsonNode node) {
    ObjectNode objectNode = (ObjectNode) node;
    objectNode.put(YamlNode.UUID_FIELD_NAME, generateUuid());
    for (Iterator<Entry<String, JsonNode>> it = objectNode.fields(); it.hasNext();) {
      Entry<String, JsonNode> field = it.next();
      injectUuid(field.getValue());
    }
  }

  public YamlNode getGivenYamlNodeFromParentPath(YamlNode currentNode, String fieldName) {
    if (currentNode == null) {
      return null;
    }
    YamlNode requiredNode;
    if (currentNode.getParentNode() == null) {
      return null;
    }
    if (currentNode.getParentNode().isArray()) {
      requiredNode = checkNodeIfParentIsArray(currentNode.getParentNode(), fieldName);
    } else {
      requiredNode = checkNodeIfParentIsObject(currentNode.getParentNode(), fieldName);
    }
    if (requiredNode == null) {
      return getGivenYamlNodeFromParentPath(currentNode.getParentNode(), fieldName);
    }
    return requiredNode;
  }

  private YamlNode checkNodeIfParentIsObject(YamlNode parentNode, String fieldName) {
    List<YamlField> fields = parentNode.fields();
    for (YamlField currentField : fields) {
      if (currentField.getName().equals(fieldName)) {
        return currentField.getNode();
      }
    }
    return null;
  }

  private YamlNode checkNodeIfParentIsArray(YamlNode parentNode, String fieldName) {
    List<YamlNode> yamlNodes = parentNode.getParentNode().asArray();
    for (YamlNode yamlNode : yamlNodes) {
      List<YamlField> currentNodeFields = yamlNode.fields();
      for (YamlField currentNodeField : currentNodeFields) {
        if (currentNodeField.getName().equals(fieldName)) {
          return currentNodeField.getNode();
        }
      }
    }
    return null;
  }

  private void injectUuidInArray(JsonNode node) {
    ArrayNode arrayNode = (ArrayNode) node;
    for (Iterator<JsonNode> it = arrayNode.elements(); it.hasNext();) {
      injectUuid(it.next());
    }
  }

  private YamlField getMatchingFieldNameFromParentUsingUUID(YamlNode parent, String uuid) {
    for (YamlField field : parent.fields()) {
      if (uuid.equals(parent.getField(field.getName()).getNode().getUuid())) {
        return field;
      }
    }
    return null;
  }

  public YamlField getMatchingFieldNameFromParentUsingValueAsText(YamlNode parent, String value) {
    for (YamlField field : parent.fields()) {
      if (value.equals(parent.getField(field.getName()).getNode().getCurrJsonNode().asText())) {
        return field;
      }
    }
    return null;
  }
}
