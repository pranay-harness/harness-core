package io.harness.pms.exception;

import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class YamlNodeErrorInfo {
  String identifier;
  String name;
  String type;

  public static YamlNodeErrorInfo fromField(YamlField field) {
    YamlNode node = field.getNode();
    return new YamlNodeErrorInfo(
        node == null ? null : node.getIdentifier(), field.getName(), node == null ? null : node.getType());
  }
}
