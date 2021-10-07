package io.harness.pms.stages;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidYamlException;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class StagesExpressionExtractorTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetStageYamlList() {
    String invalidYaml = "pipeline:\nidentifier:s1";
    assertThatThrownBy(() -> StagesExpressionExtractor.getStageYamlList(invalidYaml, Collections.singletonList("a")))
        .isInstanceOf(InvalidYamlException.class)
        .hasMessage("Could not read pipeline yaml while extracting stage yaml list");

    String pipelineYaml = getPipelineYaml();
    List<String> stageYamlList =
        StagesExpressionExtractor
            .getStageYamlList(pipelineYaml, Arrays.asList("a1", "a2", "d1", "p_d1", "p_d2", "d1_again"))
            .stream()
            .map(BasicStageInfo::getYaml)
            .collect(Collectors.toList());
    assertThat(stageYamlList).hasSize(6);
    assertThat(stageYamlList.get(0)).isEqualTo(getStage("a1", "a1", "Approval", "notAnExpression"));
    assertThat(stageYamlList.get(1)).isEqualTo(getStage("a2", "a2", "Approval", "<+stage.name>"));
    assertThat(stageYamlList.get(2)).isEqualTo(getStage("d1", "d1", "Deployment", "<+stages.a1.name>"));
    assertThat(stageYamlList.get(3)).isEqualTo(getStage("p_d1", "p d1", "Deployment", "<+pipeline.stages.a2.name>"));
    assertThat(stageYamlList.get(4)).isEqualTo(getStage("p_d2", "p d2", "Deployment", "<+input>"));
    assertThat(stageYamlList.get(5)).isEqualTo(getStage("d1_again", "d1 again", "Deployment", "<+that.other.field>"));

    List<String> stageYamlListForD1 =
        StagesExpressionExtractor.getStageYamlList(pipelineYaml, Collections.singletonList("d1"))
            .stream()
            .map(BasicStageInfo::getYaml)
            .collect(Collectors.toList());
    assertThat(stageYamlListForD1).hasSize(1);
    assertThat(stageYamlListForD1.get(0)).isEqualTo(getStage("d1", "d1", "Deployment", "<+stages.a1.name>"));
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetListOfExpressions() {
    String a1 = getStage("a1", "a1", "Approval", "notAnExpression");
    assertThat(StagesExpressionExtractor.getListOfExpressions(a1)).hasSize(0);

    String a2 = getStage("a2", "a2", "Approval", "<+stage.name>");
    List<String> a2Expressions = StagesExpressionExtractor.getListOfExpressions(a2);
    assertThat(a2Expressions).hasSize(1);
    assertThat(a2Expressions.get(0)).isEqualTo("<+stage.name>");

    String pd2 = getStage("p_d2", "<+a>", "Deployment", "<+input>");
    List<String> pd2Expressions = StagesExpressionExtractor.getListOfExpressions(pd2);
    assertThat(pd2Expressions).hasSize(2);
    assertThat(pd2Expressions.get(0)).isEqualTo("<+a>");
    assertThat(pd2Expressions.get(1)).isEqualTo("<+input>");
  }

  private String getPipelineYaml() {
    return "pipeline:\n"
        + "  stages:\n"
        + "  - stage:\n"
        + "     identifier: a1\n"
        + "     name: a1\n"
        + "     type: Approval\n"
        + "     field: notAnExpression\n"
        + "  - stage:\n"
        + "     identifier: a2\n"
        + "     name: a2\n"
        + "     type: Approval\n"
        + "     field: <+stage.name>\n"
        + "  - stage:\n"
        + "      identifier: d1\n"
        + "      name: d1\n"
        + "      type: Deployment\n"
        + "      field: <+stages.a1.name>\n"
        + "  - parallel:\n"
        + "    - stage:\n"
        + "        identifier: p_d1\n"
        + "        name: p d1\n"
        + "        type: Deployment\n"
        + "        field: <+pipeline.stages.a2.name>\n"
        + "    - stage:\n"
        + "        identifier: p_d2\n"
        + "        name: p d2\n"
        + "        type: Deployment\n"
        + "        field: <+input>\n"
        + "  - stage:\n"
        + "      identifier: d1_again\n"
        + "      name: d1 again\n"
        + "      type: Deployment\n"
        + "      field: <+that.other.field>\n";
  }

  private String getStage(String identifier, String name, String type, String field) {
    return "stage:\n"
        + "  identifier: \"" + identifier + "\"\n"
        + "  name: \"" + name + "\"\n"
        + "  type: \"" + type + "\"\n"
        + "  field: \"" + field + "\"\n";
  }
}