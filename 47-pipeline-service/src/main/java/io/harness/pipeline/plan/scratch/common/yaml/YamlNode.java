package io.harness.pipeline.plan.scratch.common.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@Value
public class YamlNode {
  public static final String UUID_FIELD_NAME = "uuid";
  public static final String IDENTIFIER_FIELD_NAME = "identifier";
  public static final String TYPE_FIELD_NAME = "type";
  public static final String NAME_FIELD_NAME = "name";

  @NotNull JsonNode currNode;
  YamlNode parentNode;
  @NotNull JsonNode rootNode;

  public YamlNode(JsonNode rootNode) {
    this.currNode = rootNode;
    this.parentNode = null;
    this.rootNode = rootNode;
  }

  public YamlNode(JsonNode currNode, YamlNode parentNode) {
    this.currNode = currNode;
    this.parentNode = parentNode;
    this.rootNode = parentNode.rootNode;
  }

  @Override
  public String toString() {
    return currNode.toString();
  }

  public boolean isObject() {
    return currNode.isObject();
  }

  public boolean isArray() {
    return currNode.isArray();
  }

  public String asText() {
    return currNode.asText();
  }

  public List<YamlNode> asArray() {
    List<YamlNode> entries = new ArrayList<>();
    currNode.elements().forEachRemaining(field -> entries.add(new YamlNode(field, this)));
    return entries;
  }

  public List<YamlField> fields() {
    List<YamlField> entries = new ArrayList<>();
    currNode.fields().forEachRemaining(field -> entries.add(new YamlField(field, this)));
    return entries;
  }

  public String getUuid() {
    return getStringValue(UUID_FIELD_NAME);
  }

  public String getIdentifier() {
    return getStringValue(IDENTIFIER_FIELD_NAME);
  }

  public String getType() {
    return getStringValue(TYPE_FIELD_NAME);
  }

  public String getName() {
    return getStringValue(NAME_FIELD_NAME);
  }

  public String getNameOrIdentifier() {
    return Optional.ofNullable(getName()).orElse(getIdentifier());
  }

  public String getStringValue(String name) {
    JsonNode value = getValueInternal(name);
    return (value == null || !value.isTextual()) ? null : value.asText();
  }

  public YamlField getField(String name) {
    JsonNode value = getValueInternal(name);
    return value == null ? null : new YamlField(name, new YamlNode(currNode.findValue(name), this));
  }

  private JsonNode getValueInternal(String key) {
    JsonNode value = !currNode.isObject() ? null : currNode.get(key);
    if (value != null && value.isNull()) {
      value = null;
    }
    return value;
  }
}
