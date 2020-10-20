package io.harness.pipeline.plan.scratch.common.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.util.Map;
import javax.validation.constraints.NotNull;

@Value
public class YamlField {
  String name;
  @NotNull YamlNode node;
  boolean nestedArray;

  public YamlField(String name, YamlNode node, boolean nestedArray) {
    this.name = name;
    this.node = node;
    this.nestedArray = nestedArray;
  }

  public YamlField(YamlNode node) {
    this(null, node, false);
  }

  public YamlField(String name, YamlNode node) {
    this(name, node, false);
  }

  public YamlField(Map.Entry<String, JsonNode> entry, YamlNode parent) {
    this(entry.getKey(), new YamlNode(entry.getValue(), parent), false);
  }
}
