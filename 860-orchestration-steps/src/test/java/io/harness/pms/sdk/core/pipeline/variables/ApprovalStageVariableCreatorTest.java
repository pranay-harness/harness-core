package io.harness.pms.sdk.core.pipeline.variables;

import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApprovalStageVariableCreatorTest extends CategoryTest {
  ApprovalStageVariableCreator approvalStageVariableCreator = new ApprovalStageVariableCreator();
  private static final String STAGE_ID = "NnmWEe_TRXCba1-R2EsDrw";

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void createVariablesForChildrenNodes() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("approval_stage.json");
    String json = Resources.toString(testFile, Charsets.UTF_8);
    JsonNode jsonNode = JsonUtils.asObject(json, JsonNode.class);
    YamlNode approvalYamlNode = new YamlNode("stage", jsonNode);
    YamlField yamlField = new YamlField(approvalYamlNode);
    LinkedHashMap<String, VariableCreationResponse> variablesMap =
        approvalStageVariableCreator.createVariablesForChildrenNodes(null, yamlField);
    assertThat(variablesMap.get(STAGE_ID)).isNotNull();
    YamlField specYaml = variablesMap.get(STAGE_ID).getDependencies().get(STAGE_ID);
    assertThat(yamlField.getNode().getFieldName()).isNotEmpty();
    assertThat(specYaml.getName()).isEqualTo("execution");
    assertThat(specYaml.getNode().fetchKeys()).containsExactlyInAnyOrder("steps", "rollbackSteps", "__uuid");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void getSupportedTypes() {
    assertThat(approvalStageVariableCreator.getSupportedTypes())
        .containsEntry(YAMLFieldNameConstants.STAGE, Collections.singleton("Approval"));
  }
}