package io.harness.pms.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class YamlNode {
  public static final String UUID_FIELD_NAME = "uuid";
  public static final String IDENTIFIER_FIELD_NAME = "identifier";
  public static final String TYPE_FIELD_NAME = "type";
  public static final String NAME_FIELD_NAME = "name";

  @NotNull JsonNode currNode;

  public YamlNode(JsonNode currNode) {
    this.currNode = currNode;
  }

  public YamlNode(JsonNode currNode, YamlNode parentNode) {
    this.currNode = currNode;
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
    currNode.elements().forEachRemaining(el -> entries.add(new YamlNode(el)));
    return entries;
  }

  public List<YamlField> fields() {
    List<YamlField> entries = new ArrayList<>();
    currNode.fields().forEachRemaining(el -> entries.add(new YamlField(el.getKey(), new YamlNode(el.getValue()))));
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
    return value == null ? null : new YamlField(name, new YamlNode(currNode.findValue(name)));
  }

  private JsonNode getValueInternal(String key) {
    JsonNode value = !currNode.isObject() ? null : currNode.get(key);
    if (value != null && value.isNull()) {
      value = null;
    }
    return value;
  }
}
