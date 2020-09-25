package io.harness.cdng.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.mergeinputset.MergeInputSetVisitor;
import io.harness.walktree.visitor.mergeinputset.beans.MergeInputSetErrorResponse;
import io.harness.walktree.visitor.mergeinputset.beans.MergeVisitorInputSetElement;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.walktree.visitor.response.VisitorResponse;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    List<MergeVisitorInputSetElement> inputSetPipelineList = new LinkedList<>();
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input1").inputSetElement(inputSet1).build());
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input2").inputSetElement(inputSet2).build());
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input3").inputSetElement(inputSet3).build());
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input4").inputSetElement(inputSet4).build());

    MergeInputSetVisitor mergeInputSetVisitor =
        simpleVisitorFactory.obtainMergeInputSetVisitor(false, inputSetPipelineList);
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

  @Test
  @Owner(developers = OwnerRule.ARCHIT)
  @Category(UnitTests.class)
  public void testMergingInputSetsWithPipelineInputTemplate() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    URL testFile = classLoader.getResource("cdng/mergeInputSets/originalPipelineInputSetTemplate.yml");
    CDPipeline originalPipeline = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/inputSet1.yml");
    CDPipeline inputSet1 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/inputSet2.yml");
    CDPipeline inputSet2 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/inputSet3.yml");
    CDPipeline inputSet3 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/inputSet4.yml");
    CDPipeline inputSet4 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    List<MergeVisitorInputSetElement> inputSetPipelineList = new LinkedList<>();
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input1").inputSetElement(inputSet1).build());
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input2").inputSetElement(inputSet2).build());
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input3").inputSetElement(inputSet3).build());
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input4").inputSetElement(inputSet4).build());

    MergeInputSetVisitor mergeInputSetVisitor =
        simpleVisitorFactory.obtainMergeInputSetVisitor(false, inputSetPipelineList);
    VisitElementResult visitElementResult = mergeInputSetVisitor.walkElementTree(originalPipeline);

    assertThat(visitElementResult).isEqualTo(VisitElementResult.CONTINUE);
    String mergedPipelineYaml = JsonPipelineUtils.writeYamlString(mergeInputSetVisitor.getCurrentObjectResult())
                                    .replaceAll("---\n", "")
                                    .replaceAll("\"", "");
    String expectedMergedPipelineYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("cdng/mergeInputSets/expectedMergedPipelineInputTemplate.yml")),
        StandardCharsets.UTF_8);
    assertThat(mergedPipelineYaml).isEqualTo(expectedMergedPipelineYaml);
  }

  @Test
  @Owner(developers = OwnerRule.ARCHIT)
  @Category(UnitTests.class)
  public void testMergingInputSetsWithWrongTemplate() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    URL testFile = classLoader.getResource("cdng/mergeInputSets/pipelineWithRuntimeInput.yml");
    CDPipeline originalPipeline = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/wrongInputSet1.yml");
    CDPipeline inputSet1 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    testFile = classLoader.getResource("cdng/mergeInputSets/wrongInputSet2.yml");
    CDPipeline inputSet2 = YamlPipelineUtils.read(testFile, CDPipeline.class);

    List<MergeVisitorInputSetElement> inputSetPipelineList = new LinkedList<>();
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input1").inputSetElement(inputSet1).build());
    inputSetPipelineList.add(
        MergeVisitorInputSetElement.builder().identifier("input2").inputSetElement(inputSet2).build());

    MergeInputSetVisitor mergeInputSetVisitor =
        simpleVisitorFactory.obtainMergeInputSetVisitor(true, inputSetPipelineList);
    VisitElementResult visitElementResult = mergeInputSetVisitor.walkElementTree(originalPipeline);

    assertThat(visitElementResult).isEqualTo(VisitElementResult.CONTINUE);
    assertThat(mergeInputSetVisitor.isResultValidInputSet()).isFalse();

    Object currentObjectErrorResult = mergeInputSetVisitor.getCurrentObjectErrorResult();
    String errorPipelineYaml =
        JsonPipelineUtils.writeYamlString(currentObjectErrorResult).replaceAll("---\n", "").replaceAll("\"", "");
    String expectedMergedPipelineYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("cdng/mergeInputSets/expectedErrorPipeline.yml")),
        StandardCharsets.UTF_8);
    assertThat(errorPipelineYaml).isEqualTo(expectedMergedPipelineYaml);

    Map<String, VisitorResponse> uuidToErrorResponseMap = mergeInputSetVisitor.getUuidToErrorResponseMap();
    assertThat(uuidToErrorResponseMap.keySet().size()).isEqualTo(5);

    CDPipeline errorResult = (CDPipeline) currentObjectErrorResult;
    ArtifactListConfig stage1Artifacts =
        ((DeploymentStage) ((StageElement) errorResult.getStages().get(0)).getStageType())
            .getService()
            .getServiceDefinition()
            .getServiceSpec()
            .getArtifacts();
    String key =
        ((DockerHubArtifactConfig) stage1Artifacts.getPrimary().getArtifactConfig()).getTag().getResponseField();
    String expectedIdentifier = "input1";
    String expectedFieldName = "tag";
    String expectedMessage = "Input Set field cannot have value if not marked as runtime in original pipeline.";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key = ((DockerHubArtifactConfig) stage1Artifacts.getSidecars().get(0).getArtifactConfig())
              .getImagePath()
              .getResponseField();
    expectedFieldName = "imagePath";
    expectedMessage = "Value inside input set cannot be another runtime expression.";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key = ((K8SDirectInfrastructure) ((DeploymentStage) ((StageElement) errorResult.getStages().get(0)).getStageType())
               .getInfrastructure()
               .getInfrastructureDefinition()
               .getInfrastructure())
              .getConnectorIdentifier()
              .getResponseField();
    expectedFieldName = "connectorIdentifier";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    GitStore stage2GitStore =
        (GitStore) ((DeploymentStage) ((StageElement) errorResult.getStages().get(1)).getStageType())
            .getService()
            .getStageOverrides()
            .getManifests()
            .get(0)
            .getManifestAttributes()
            .getStoreConfig();

    key = stage2GitStore.getConnectorIdentifier().getResponseField();
    expectedIdentifier = "input2";
    expectedFieldName = "connectorIdentifier";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key = stage2GitStore.getMetadata();
    expectedIdentifier = "input2";
    expectedFieldName = "gitFetchType";
    expectedMessage = "Input Set field cannot have value if not marked as runtime in original pipeline.";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);
  }

  private void validateAssertOnErrorResponse(Map<String, VisitorResponse> uuidToErrorResponseMap, String key,
      String expectedIdentifier, String expectedFieldName, String expectedMessage) {
    assertThat(uuidToErrorResponseMap.containsKey(key)).isTrue();
    VisitorErrorResponseWrapper errorResponseWrapper = (VisitorErrorResponseWrapper) uuidToErrorResponseMap.get(key);
    assertThat(errorResponseWrapper.getErrors().size()).isEqualTo(1);
    MergeInputSetErrorResponse errorResponse = (MergeInputSetErrorResponse) errorResponseWrapper.getErrors().get(0);
    assertThat(errorResponse.getCausedByInputSetIdentifier()).isEqualTo(expectedIdentifier);
    assertThat(errorResponse.getFieldName()).isEqualTo(expectedFieldName);
    assertThat(errorResponse.getMessage()).isEqualTo(expectedMessage);
  }
}
