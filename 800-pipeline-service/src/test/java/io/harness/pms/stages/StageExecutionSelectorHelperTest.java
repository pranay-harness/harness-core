package io.harness.pms.stages;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class StageExecutionSelectorHelperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetStageExecutionResponse() {
    String pipelineYaml = getPipelineYaml();
    List<StageExecutionResponse> stageExecutionResponse =
        StageExecutionSelectorHelper.getStageExecutionResponse(pipelineYaml);
    assertThat(stageExecutionResponse).hasSize(6);

    StageExecutionResponse a1 = stageExecutionResponse.get(0);
    assertStageResponse(a1, "a1", "a1", "[]", true);
    StageExecutionResponse a2 = stageExecutionResponse.get(1);
    assertStageResponse(a2, "a2", "a2", "[a1]", true);
    StageExecutionResponse d1 = stageExecutionResponse.get(2);
    assertStageResponse(d1, "d1", "d1", "[a1, a2]", false);
    StageExecutionResponse p_d1 = stageExecutionResponse.get(3);
    assertStageResponse(p_d1, "p_d1", "p d1", "[a1, a2]", false);
    StageExecutionResponse p_d2 = stageExecutionResponse.get(4);
    assertStageResponse(p_d2, "p_d2", "p d2", "[a1, a2]", false);
    StageExecutionResponse d1_again = stageExecutionResponse.get(5);
    assertStageResponse(d1_again, "d1_again", "d1 again", "[a1, a2]", false);
  }

  private void assertStageResponse(
      StageExecutionResponse response, String identifier, String name, String requiredStages, boolean isApproval) {
    assertThat(response.getStageIdentifier()).isEqualTo(identifier);
    assertThat(response.getStageName()).isEqualTo(name);
    if (isApproval) {
      assertThat(response.getMessage()).isEqualTo("Running an approval stage individually can be redundant");
    } else {
      assertThat(response.getMessage()).isNull();
    }
    assertThat(response.getStagesRequired().toString()).isEqualTo(requiredStages);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetStageInfoListWithStagesRequired() {
    String pipelineYaml = getPipelineYaml();
    List<BasicStageInfo> stageInfoList = StageExecutionSelectorHelper.getStageInfoListWithStagesRequired(pipelineYaml);

    assertBasicStageInfo(stageInfoList);
    assertThat(stageInfoList.get(0).getStagesRequired().toString()).isEqualTo("[]");
    assertThat(stageInfoList.get(1).getStagesRequired().toString()).isEqualTo("[a1]");
    assertThat(stageInfoList.get(2).getStagesRequired().toString()).isEqualTo("[a1, a2]");
    assertThat(stageInfoList.get(3).getStagesRequired().toString()).isEqualTo("[a1, a2]");
    assertThat(stageInfoList.get(4).getStagesRequired().toString()).isEqualTo("[a1, a2]");
    assertThat(stageInfoList.get(5).getStagesRequired().toString()).isEqualTo("[a1, a2]");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetStageInfoList() {
    String pipelineYaml = getPipelineYaml();
    List<BasicStageInfo> stageYamlList = StageExecutionSelectorHelper.getStageInfoList(pipelineYaml);
    assertBasicStageInfo(stageYamlList);
  }

  private void assertBasicStageInfo(List<BasicStageInfo> stageYamlList) {
    assertThat(stageYamlList).hasSize(6);
    assertThat(stageYamlList.get(0).getIdentifier()).isEqualTo("a1");
    assertThat(stageYamlList.get(1).getIdentifier()).isEqualTo("a2");
    assertThat(stageYamlList.get(2).getIdentifier()).isEqualTo("d1");
    assertThat(stageYamlList.get(3).getIdentifier()).isEqualTo("p_d1");
    assertThat(stageYamlList.get(4).getIdentifier()).isEqualTo("p_d2");
    assertThat(stageYamlList.get(5).getIdentifier()).isEqualTo("d1_again");

    assertThat(stageYamlList.get(0).getName()).isEqualTo("a1");
    assertThat(stageYamlList.get(1).getName()).isEqualTo("a2");
    assertThat(stageYamlList.get(2).getName()).isEqualTo("d1");
    assertThat(stageYamlList.get(3).getName()).isEqualTo("p d1");
    assertThat(stageYamlList.get(4).getName()).isEqualTo("p d2");
    assertThat(stageYamlList.get(5).getName()).isEqualTo("d1 again");

    assertThat(stageYamlList.get(0).getType()).isEqualTo("Approval");
    assertThat(stageYamlList.get(1).getType()).isEqualTo("Approval");
    assertThat(stageYamlList.get(2).getType()).isEqualTo("Deployment");
    assertThat(stageYamlList.get(3).getType()).isEqualTo("Deployment");
    assertThat(stageYamlList.get(4).getType()).isEqualTo("Deployment");
    assertThat(stageYamlList.get(5).getType()).isEqualTo("Deployment");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testAddStagesRequiredStartingWithApproval() {
    BasicStageInfo stageInfo0 = BasicStageInfo.builder().name("a0").identifier("a0").type("Approval").build();
    BasicStageInfo stageInfo1 = BasicStageInfo.builder().name("a1").identifier("a1").type("Approval").build();
    BasicStageInfo stageInfo2 = BasicStageInfo.builder().name("d0").identifier("d0").type("Deployment").build();
    BasicStageInfo stageInfo3 = BasicStageInfo.builder().name("d1").identifier("d1").type("Deployment").build();
    BasicStageInfo stageInfo4 = BasicStageInfo.builder().name("a2").identifier("a2").type("Approval").build();
    BasicStageInfo stageInfo5 = BasicStageInfo.builder().name("d2").identifier("d2").type("Deployment").build();
    List<BasicStageInfo> stageInfos =
        Arrays.asList(stageInfo0, stageInfo1, stageInfo2, stageInfo3, stageInfo4, stageInfo5);
    List<BasicStageInfo> stageInfosWithRequiredStages = StageExecutionSelectorHelper.addStagesRequired(stageInfos);
    assertThat(stageInfosWithRequiredStages).hasSize(6);
    assertThat(stageInfosWithRequiredStages.get(0).getStagesRequired().toString()).isEqualTo("[]");
    assertThat(stageInfosWithRequiredStages.get(1).getStagesRequired().toString()).isEqualTo("[a0]");
    assertThat(stageInfosWithRequiredStages.get(2).getStagesRequired().toString()).isEqualTo("[a0, a1]");
    assertThat(stageInfosWithRequiredStages.get(3).getStagesRequired().toString()).isEqualTo("[a0, a1]");
    assertThat(stageInfosWithRequiredStages.get(4).getStagesRequired().toString()).isEqualTo("[]");
    assertThat(stageInfosWithRequiredStages.get(5).getStagesRequired().toString()).isEqualTo("[a2]");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testAddStagesRequiredStartingWithNonApproval() {
    BasicStageInfo stageInfo0 = BasicStageInfo.builder().name("b0").identifier("b0").type("Build").build();
    BasicStageInfo stageInfo1 = BasicStageInfo.builder().name("d1").identifier("d1").type("Deployment").build();
    BasicStageInfo stageInfo2 = BasicStageInfo.builder().name("a2").identifier("a2").type("Approval").build();
    BasicStageInfo stageInfo3 = BasicStageInfo.builder().name("d2").identifier("d2").type("Deployment").build();
    List<BasicStageInfo> stageInfos = Arrays.asList(stageInfo0, stageInfo1, stageInfo2, stageInfo3);
    List<BasicStageInfo> stageInfosWithRequiredStages = StageExecutionSelectorHelper.addStagesRequired(stageInfos);
    assertThat(stageInfosWithRequiredStages).hasSize(4);
    assertThat(stageInfosWithRequiredStages.get(0).getStagesRequired().toString()).isEqualTo("[]");
    assertThat(stageInfosWithRequiredStages.get(1).getStagesRequired().toString()).isEqualTo("[]");
    assertThat(stageInfosWithRequiredStages.get(2).getStagesRequired().toString()).isEqualTo("[]");
    assertThat(stageInfosWithRequiredStages.get(3).getStagesRequired().toString()).isEqualTo("[a2]");
  }

  private String getPipelineYaml() {
    return "pipeline:\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "     identifier: a1\n"
        + "     name: a1\n"
        + "     type: Approval\n"
        + "  - stage:\n"
        + "     identifier: a2\n"
        + "     name: a2\n"
        + "     type: Approval\n"
        + "  - stage:\n"
        + "      identifier: d1\n"
        + "      name: d1\n"
        + "      type: Deployment\n"
        + "  - parallel:\n"
        + "    - stage:\n"
        + "        identifier: p_d1\n"
        + "        name: p d1\n"
        + "        type: Deployment\n"
        + "    - stage:\n"
        + "        identifier: p_d2\n"
        + "        name: p d2\n"
        + "        type: Deployment\n"
        + "  - stage:\n"
        + "      identifier: d1_again\n"
        + "      name: d1 again\n"
        + "      type: Deployment";
  }
}