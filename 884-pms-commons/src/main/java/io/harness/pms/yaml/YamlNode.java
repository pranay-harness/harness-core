package io.harness.pms.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
public class YamlNode implements Visitable {
  public static final String UUID_FIELD_NAME = "__uuid";
  public static final String IDENTIFIER_FIELD_NAME = "identifier";
  public static final String TYPE_FIELD_NAME = "type";
  public static final String NAME_FIELD_NAME = "name";
  public static final String KEY_FIELD_NAME = "key";

  YamlNode parentNode;
  @NotNull JsonNode currJsonNode;

  public YamlNode(JsonNode currJsonNode) {
    this.currJsonNode = currJsonNode;
    this.parentNode = null;
  }

  @JsonCreator
  public YamlNode(
      @JsonProperty("currJsonNode") JsonNode currJsonNode, @JsonProperty("parentNode") YamlNode parentNode) {
    this.currJsonNode = currJsonNode;
    this.parentNode = parentNode;
  }

  @Override
  public String toString() {
    return currJsonNode.toString();
  }

  public boolean isObject() {
    return currJsonNode.isObject();
  }

  public boolean isArray() {
    return currJsonNode.isArray();
  }

  public String asText() {
    return currJsonNode.asText();
  }

  public YamlField nextSiblingFromParentArray(String currentFieldName, List<String> possibleSiblingFieldNames) {
    if (parentNode == null || parentNode.parentNode == null || parentNode.parentNode.isObject()) {
      return null;
    }
    List<YamlNode> yamlNodes = parentNode.parentNode.asArray();
    for (int i = 0; i < yamlNodes.size() - 1; i++) {
      YamlField givenNode = yamlNodes.get(i).getField(currentFieldName);
      if (givenNode != null && givenNode.getNode().getUuid() != null
          && givenNode.getNode().getUuid().equals(this.getUuid())) {
        return getMatchingFieldNameFromParent(yamlNodes.get(i + 1), new HashSet<>(possibleSiblingFieldNames));
      }
    }
    return null;
  }

  public YamlField nextSiblingNodeFromParentObject(String siblingFieldName) {
    if (parentNode == null || parentNode.isArray()) {
      return null;
    }
    return getMatchingFieldNameFromParent(parentNode, Collections.singleton(siblingFieldName));
  }

  private YamlField getMatchingFieldNameFromParent(YamlNode parent, Set<String> fieldNames) {
    for (YamlField field : parent.fields()) {
      if (fieldNames.contains(field.getName()) && !field.getNode().getCurrJsonNode().isNull()) {
        return field;
      }
    }
    return null;
  }

  public List<YamlNode> asArray() {
    List<YamlNode> entries = new ArrayList<>();
    currJsonNode.elements().forEachRemaining(el -> entries.add(new YamlNode(el, this)));
    return entries;
  }

  public List<YamlField> fields() {
    List<YamlField> entries = new ArrayList<>();
    currJsonNode.fields().forEachRemaining(
        el -> entries.add(new YamlField(el.getKey(), new YamlNode(el.getValue(), this))));
    return entries;
  }

  public String getUuid() {
    String uuidValue = getStringValue(UUID_FIELD_NAME);
    // This means that current node is of array type
    if (uuidValue == null && parentNode != null && parentNode.isObject()) {
      List<YamlField> childFields = parentNode.fields();
      for (YamlField childField : childFields) {
        if (compareFirstChildOfArrayNode(childField.getNode(), this)) {
          return parentNode.getUuid() + childField.getName();
        }
      }
      return null;
    }
    return uuidValue;
  }

  private boolean compareFirstChildOfArrayNode(YamlNode firstParent, YamlNode secondParent) {
    List<YamlNode> firstParentChildNodes = firstParent.asArray();
    List<YamlNode> secondParentChildNodes = secondParent.asArray();
    if (firstParentChildNodes.isEmpty() || secondParentChildNodes.isEmpty()
        || firstParentChildNodes.get(0).getUuid() == null || secondParentChildNodes.get(0).getUuid() == null) {
      return false;
    }
    return Objects.equals(firstParentChildNodes.get(0).getUuid(), secondParentChildNodes.get(0).getUuid());
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

  public String getKey() {
    return getStringValue(KEY_FIELD_NAME);
  }

  public String getNameOrIdentifier() {
    return Optional.ofNullable(getName()).orElse(getIdentifier());
  }

  public String getArrayUniqueIdentifier() {
    return Optional.ofNullable(getIdentifier()).orElse(Optional.ofNullable(getName()).orElse(getKey()));
  }

  public String getStringValue(String name) {
    JsonNode value = getValueInternal(name);
    return (value == null || !value.isTextual()) ? null : value.asText();
  }

  public YamlField getField(String name) {
    JsonNode valueFromField = getValueInternal(name);
    if (valueFromField != null) {
      return new YamlField(name, new YamlNode(valueFromField, this));
    }
    return null;
  }

  private JsonNode getValueInternal(String key) {
    JsonNode value = !currJsonNode.isObject() ? null : currJsonNode.get(key);
    if (value != null && value.isNull()) {
      value = null;
    }
    return value;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    if (isArray()) {
      for (YamlNode node : asArray()) {
        visitableChildren.add(UUIDGenerator.generateUuid(), node);
      }
    } else if (isObject()) {
      Map<String, YamlNode> yamlNodeFields =
          fields().stream().collect(Collectors.toMap(YamlField::getName, YamlField::getNode));
      for (Map.Entry<String, YamlNode> yamlNodeEntry : yamlNodeFields.entrySet()) {
        visitableChildren.add(yamlNodeEntry.getKey(), yamlNodeEntry.getValue());
      }
    }
    return visitableChildren;
  }
}
