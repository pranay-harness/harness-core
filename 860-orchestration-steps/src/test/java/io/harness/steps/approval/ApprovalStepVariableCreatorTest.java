package io.harness.steps.approval;

import static io.harness.rule.OwnerRule.MOUNIK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class ApprovalStepVariableCreatorTest extends CategoryTest {
  String approvalYaml1 = "pipeline:\n"
      + "  step:\n"
      + "    name: Approval\n"
      + "    identifier: approval\n"
      + "    type: HarnessApproval\n"
      + "    timeout: 1d\n"
      + "    spec:\n"
      + "      approvalMessage: |-\n"
      + "        Please review the following information\n"
      + "      approvers:\n"
      + "        minimumCount: 1\n"
      + "        disallowPipelineExecutor: false\n"
      + "        userGroups:\n"
      + "          - UG1\n"
      + "      approverInputs:\n"
      + "        - name: a1\n"
      + "          defaultValue: b2\n"
      + "          __uuid: uuid1\n"
      + "        - name: a2\n"
      + "          defaultValue: b1\n"
      + "          __uuid: uuid2";

  String approvalYaml2 = "pipeline:\n"
      + "  step:\n"
      + "    name: Approval\n"
      + "    identifier: approval\n"
      + "    type: HarnessApproval\n"
      + "    timeout: 1d\n"
      + "    spec:\n"
      + "      approvalMessage: |-\n"
      + "        Please review the following information\n"
      + "      approvers:\n"
      + "        minimumCount: 1\n"
      + "        disallowPipelineExecutor: false\n"
      + "        userGroups:\n"
      + "          - UG1";

  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void testGetSupportedStepTypes() {
    Set<String> supportedStepTypes =
        new io.harness.steps.approval.ApprovalStepVariableCreator().getSupportedStepTypes();
    assertThat(supportedStepTypes).hasSize(1);
    assertThat(supportedStepTypes).contains("HarnessApproval");
  }

  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void testAddVariablesInComplexObject() throws IOException {
    io.harness.steps.approval.ApprovalStepVariableCreator approvalStepVariableCreator =
        new ApprovalStepVariableCreator();
    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();
    Map<String, YamlOutputProperties> yamlOutputPropertiesMap = new HashMap<>();
    YamlNode step1 = getStepNode(approvalYaml1);
    YamlNode step2 = getStepNode(approvalYaml2);
    approvalStepVariableCreator.addVariablesInComplexObject(yamlPropertiesMap, yamlOutputPropertiesMap, step1);
    assertThat(yamlOutputPropertiesMap).hasSize(0);
    assertThat(yamlPropertiesMap).hasSize(3);
    assertThat(yamlPropertiesMap.values().stream().map(YamlProperties::getFqn))
        .containsExactlyInAnyOrder(
            "pipeline.spec.approvalMessage", "pipeline.output.approverInputs.a1", "pipeline.output.approverInputs.a2");
    yamlPropertiesMap.clear();
    yamlOutputPropertiesMap.clear();
    approvalStepVariableCreator.addVariablesInComplexObject(yamlPropertiesMap, yamlOutputPropertiesMap, step2);
    assertThat(yamlOutputPropertiesMap).hasSize(0);
    assertThat(yamlPropertiesMap).hasSize(1);
    assertThat(yamlPropertiesMap.values().stream().map(YamlProperties::getFqn))
        .containsExactlyInAnyOrder("pipeline.spec.approvalMessage");
  }

  private YamlNode getStepNode(String yaml) throws IOException {
    return YamlUtils.readTree(yaml)
        .getNode()
        .getField("pipeline")
        .getNode()
        .getField("step")
        .getNode()
        .getField("spec")
        .getNode();
  }
}
