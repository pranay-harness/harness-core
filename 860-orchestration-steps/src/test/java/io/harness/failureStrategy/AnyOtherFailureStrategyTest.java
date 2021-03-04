package io.harness.failureStrategy;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

@Slf4j
public class AnyOtherFailureStrategyTest extends GenericStepPMSPlanCreator {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void anyOtherFailureStrategy() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipelineAnyOtherStageFailure.yaml");

    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent));
    // Pipeline Node
    YamlNode pipelineNode = yamlField.getNode().getField("pipeline").getNode();

    // Stages Node
    YamlField stagesNode = pipelineNode.getField("stages");

    YamlField childNode = stagesNode.getNode().asArray().get(0).getField("stage");


  }

  @Override
  public Set<String> getSupportedStepTypes() {
    return null;
  }
}
