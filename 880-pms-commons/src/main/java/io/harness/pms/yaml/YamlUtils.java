package io.harness.pms.yaml;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import lombok.experimental.UtilityClass;

@UtilityClass
public class YamlUtils {
  private final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new JavaTimeModule());
  }

  public <T> T read(String yaml, Class<T> cls) throws IOException {
    return mapper.readValue(yaml, cls);
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

  private void injectUuidInObject(JsonNode node) {
    ObjectNode objectNode = (ObjectNode) node;
    objectNode.put(YamlNode.UUID_FIELD_NAME, generateUuid());
    for (Iterator<Entry<String, JsonNode>> it = objectNode.fields(); it.hasNext();) {
      Entry<String, JsonNode> field = it.next();
      injectUuid(field.getValue());
    }
  }

  private void injectUuidInArray(JsonNode node) {
    ArrayNode arrayNode = (ArrayNode) node;
    for (Iterator<JsonNode> it = arrayNode.elements(); it.hasNext();) {
      injectUuid(it.next());
    }
  }
}
