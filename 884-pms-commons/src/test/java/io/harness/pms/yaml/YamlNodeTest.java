package io.harness.pms.yaml;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class YamlNodeTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testYamlPath() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    URL testFile = classLoader.getResource("pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlNode yamlNode = YamlUtils.readTree(YamlUtils.injectUuid(yamlContent)).getNode();
    assertThat(yamlNode.getYamlPath()).isEqualTo("");

    YamlNode pipelineNode = yamlNode.getField("pipeline").getNode();
    assertThat(pipelineNode.getYamlPath()).isEqualTo("pipeline");

    YamlField stagesNode = pipelineNode.getField("stages");
    assertThat(stagesNode.getYamlPath()).isEqualTo("pipeline/stages");

    YamlNode stage1Node = stagesNode.getNode().asArray().get(0).getField("stage").getNode();
    assertThat(stage1Node.getYamlPath()).isEqualTo("pipeline/stages/[0]/stage");

    YamlNode serviceNode = stage1Node.getField("spec").getNode().getField("service").getNode();
    assertThat(serviceNode.getYamlPath()).isEqualTo("pipeline/stages/[0]/stage/spec/service");

    YamlNode imagePathNode = serviceNode.getField("serviceDefinition")
                                 .getNode()
                                 .getField("spec")
                                 .getNode()
                                 .getField("artifacts")
                                 .getNode()
                                 .getField("primary")
                                 .getNode()
                                 .getField("spec")
                                 .getNode()
                                 .getField("imagePath")
                                 .getNode();
    assertThat(imagePathNode.getYamlPath())
        .isEqualTo("pipeline/stages/[0]/stage/spec/service/serviceDefinition/spec/artifacts/primary/spec/imagePath");

    assertThat(serviceNode.getIdentifier()).isEqualTo("manager");
    YamlNode newServiceNode = YamlNode.fromYamlPath(yamlContent, "pipeline/stages/[0]/stage/spec/service");
    assertThat(newServiceNode).isNotNull();
    assertThat(newServiceNode.getIdentifier()).isEqualTo("manager");
  }
}
