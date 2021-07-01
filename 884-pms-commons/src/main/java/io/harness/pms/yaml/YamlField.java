package io.harness.pms.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.validation.constraints.NotNull;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
public class YamlField {
  private static final Charset CHARSET = Charset.forName(StandardCharsets.UTF_8.name());

  @NotNull YamlNode node;

  @JsonCreator
  public YamlField(@JsonProperty("node") YamlNode node) {
    this.node = node;
  }

  public String getName() {
    return node.getFieldName();
  }

  public String getYamlPath() {
    return node.getYamlPath();
  }

  public YamlFieldBlob toFieldBlob() {
    YamlFieldBlob.Builder builder =
        YamlFieldBlob.newBuilder().setBlob(ByteString.copyFrom(JsonUtils.asJson(this), CHARSET));
    String name = getName();
    if (name != null) {
      builder.setName(name);
    }
    return builder.build();
  }

  /**
   *  Check if parent of the above node is parallel or not
   *
   * @param expectedParent - The expected field name of the  parallel field's parent
   * @return
   */
  public boolean checkIfParentIsParallel(String expectedParent) {
    YamlNode parallelNode = YamlUtils.findParentNode(getNode(), YAMLFieldNameConstants.PARALLEL);
    if (parallelNode != null) {
      // YamlUtils#findParentNode might return the parallel node which might not be the direct parent therefore check if
      // parallelNode is part of expected Parent. Currently expected parent can be Stage, Step and StepGroup
      return YamlUtils.findParentNode(parallelNode, expectedParent) != null;
    }
    return false;
  }

  public static YamlField fromFieldBlob(YamlFieldBlob fieldBlob) {
    return JsonUtils.asObject(fieldBlob.getBlob().toString(CHARSET), YamlField.class);
  }

  public static YamlField fromYamlPath(String yaml, String path) throws IOException {
    YamlNode node = YamlNode.fromYamlPath(yaml, path);
    return node == null ? null : new YamlField(node);
  }
}
