package io.harness.cdng.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.mergeinputset.MergeInputSetVisitor;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MergeInputSetVisitorTest extends CDNGBaseTest {
  @Inject SimpleVisitorFactory simpleVisitorFactory;

  @Test
  @Owner(developers = OwnerRule.ARCHIT)
  @Category(UnitTests.class)
  public void testMergingInputSets() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    URL testFile = classLoader.getResource("cdng/mergeInputSets/pipelineWithRuntimeInput.yml");
    CDPipeline originalPipeline = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/inputSet1.yml");
    CDPipeline inputSet1 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/inputSet2.yml");
    CDPipeline inputSet2 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/inputSet3.yml");
    CDPipeline inputSet3 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/inputSet4.yml");
    CDPipeline inputSet4 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    List<Object> inputSetsPipeline = Stream.of(inputSet1, inputSet2, inputSet3, inputSet4).collect(Collectors.toList());
    MergeInputSetVisitor mergeInputSetVisitor =
        simpleVisitorFactory.obtainMergeInputSetVisitor(false, inputSetsPipeline);
    VisitElementResult visitElementResult = mergeInputSetVisitor.walkElementTree(originalPipeline);

    assertThat(visitElementResult).isEqualTo(VisitElementResult.CONTINUE);
    String mergedPipelineYaml = JsonPipelineUtils.writeYamlString(mergeInputSetVisitor.getCurrentObjectResult())
                                    .replaceAll("---\n", "")
                                    .replaceAll("\"", "");
    String expectedMergedPipelineYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("cdng/mergeInputSets/expectedMergedPipeline.yml")),
        StandardCharsets.UTF_8);
    assertThat(mergedPipelineYaml).isEqualTo(expectedMergedPipelineYaml);
  }
}
