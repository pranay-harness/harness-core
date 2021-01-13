package io.harness.plancreator.stages.parallel;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ParallelPlanCreatorTest extends CategoryTest {
  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetDependencyNodeIds() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();

    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");
    // Parallel Node
    YamlField parallelNode = stagesNode.getNode().asArray().get(0).getField("parallel");
    YamlField childNode = parallelNode.getNode().asArray().get(0).getField("stage");

    ParallelPlanCreator parallelPlanCreator = new ParallelPlanCreator();
    List<YamlField> result =
        parallelPlanCreator.getDependencyNodeIdsList(PlanCreationContext.builder().currentField(parallelNode).build());

    assertThat(result).isNotEmpty();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(0)).isEqualTo(childNode);
  }
}