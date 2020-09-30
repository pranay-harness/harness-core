package io.harness.cdng.inputset.helpers;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.inputset.mappers.InputSetElementMapper;
import io.harness.cdng.inputset.services.InputSetEntityService;
import io.harness.cdng.pipeline.service.PipelineService;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.walktree.visitor.mergeinputset.beans.MergeInputSetErrorResponse;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputSetMergeHelperTest extends CDNGBaseTest {
  @Inject InputSetMergeHelper inputSetMergeHelper;
  @Inject PipelineService ngPipelineService;
  @Inject InputSetEntityService inputSetEntityService;

  private final String ACCOUNT_ID = "accountId";
  private final String ORG_ID = "orgId";
  private final String PROJECT_ID = "projId";
  private final String PIPELINE_ID = "myPipeline1";

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetTemplateFromPipeline() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String pipelineFilename = "pipeline-extensive.yaml";
    String pipelineYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(pipelineFilename)), StandardCharsets.UTF_8);

    String tempYaml = inputSetMergeHelper.getTemplateFromPipeline(pipelineYaml).replaceAll("\"", "");
    assertThat(tempYaml).isNotNull();

    String templateFilename = "pipeline-extensive-template.yaml";
    String templateYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(templateFilename)), StandardCharsets.UTF_8);
    assertThat(tempYaml).isEqualTo(templateYaml);
  }

  @Test
  @Owner(developers = OwnerRule.ARCHIT)
  @Category(UnitTests.class)
  public void testMergingInputSets() throws IOException {
    setupPipelineAndInputSets();
    testMergingInputSetsWithYamlParam(
        "cdng/mergeInputSets/finalInputSet.yml", "cdng/mergeInputSets/expectedMergedPipeline.yml");
    testMergingInputSetsWithTemplateParam(false, "cdng/mergeInputSets/expectedMergedPipeline.yml");
    testMergingInputSetsWithTemplateParam(true, "cdng/mergeInputSets/expectedMergedPipelineInputTemplate.yml");
  }

  @Test
  @Owner(developers = OwnerRule.ARCHIT)
  @Category(UnitTests.class)
  public void testMergingInputSetsWithWrongTemplate() throws IOException {
    setupPipelineAndWrongInputSets();

    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputIdentifierList(ACCOUNT_ID, ORG_ID, PROJECT_ID, PIPELINE_ID,
            Stream.of("wrongInput1", "wrongInput2").collect(Collectors.toList()), false, true);

    assertThat(mergeInputSetResponse.isErrorResponse()).isTrue();
    String errorPipelineYaml = mergeInputSetResponse.getErrorPipelineYaml().replaceAll("\"", "");
    ClassLoader classLoader = getClass().getClassLoader();
    String expectedMergedPipelineYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("cdng/mergeInputSets/expectedErrorPipeline.yml")),
        StandardCharsets.UTF_8);
    assertThat(errorPipelineYaml).isEqualTo(expectedMergedPipelineYaml);
    Map<String, VisitorErrorResponseWrapper> uuidToErrorResponseMap = mergeInputSetResponse.getUuidToErrorResponseMap();
    assertThat(uuidToErrorResponseMap.keySet().size()).isEqualTo(5);

    String key = "myPipeline1.stages.qa.spec.service.serviceDefinition.spec.artifacts.primary.spec.tag";
    String expectedIdentifier = "wrongInput1";
    String expectedFieldName = "tag";
    String expectedMessage = "Input Set field cannot have value if not marked as runtime in original pipeline.";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key = "myPipeline1.stages.qa.spec.service.serviceDefinition.spec.artifacts.sidecar.spec.imagePath";
    expectedFieldName = "imagePath";
    expectedMessage = "Value inside input set cannot be another runtime expression.";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key = "myPipeline1.stages.qa.spec.infrastructure.infrastructureDefinition.spec.connectorIdentifier";
    expectedFieldName = "connectorIdentifier";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key =
        "myPipeline1.stages.prod.spec.service.stageOverrides.manifests.prodOverride.spec.store.spec.connectorIdentifier";
    expectedIdentifier = "wrongInput2";
    expectedFieldName = "connectorIdentifier";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key = "myPipeline1.stages.prod.spec.service.stageOverrides.manifests.prodOverride.spec.store.spec.metadata";
    expectedFieldName = "gitFetchType";
    expectedMessage = "Input Set field cannot have value if not marked as runtime in original pipeline.";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);
  }

  private void testMergingInputSetsWithYamlParam(
      String finalInputSetPipelineTemplateFileName, String expectedResponseYamlFileName) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String finalInputSetPipelineYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(finalInputSetPipelineTemplateFileName)), StandardCharsets.UTF_8);
    MergeInputSetResponse mergeInputSetResponse = inputSetMergeHelper.getMergePipelineYamlFromInputSetPipelineYaml(
        ACCOUNT_ID, ORG_ID, PROJECT_ID, PIPELINE_ID, finalInputSetPipelineYaml, false, false);
    assertThat(mergeInputSetResponse.isErrorResponse()).isFalse();
    String expectedMergedPipelineYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(expectedResponseYamlFileName)), StandardCharsets.UTF_8);
    String mergedPipelineYaml = mergeInputSetResponse.getPipelineYaml().replaceAll("\"", "");
    assertThat(mergedPipelineYaml).isEqualTo(expectedMergedPipelineYaml);
  }

  private void testMergingInputSetsWithTemplateParam(boolean isTemplate, String expectedResponseYamlFileName)
      throws IOException {
    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputIdentifierList(ACCOUNT_ID, ORG_ID, PROJECT_ID, PIPELINE_ID,
            Stream.of("input1", "input2", "overlay1").collect(Collectors.toList()), isTemplate, false);
    assertThat(mergeInputSetResponse.isErrorResponse()).isFalse();
    ClassLoader classLoader = getClass().getClassLoader();
    String expectedMergedPipelineYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(expectedResponseYamlFileName)), StandardCharsets.UTF_8);
    String mergedPipelineYaml = mergeInputSetResponse.getPipelineYaml().replaceAll("\"", "");
    assertThat(mergedPipelineYaml).isEqualTo(expectedMergedPipelineYaml);
  }

  private void setupPipelineAndInputSets() throws IOException {
    // Setup pipeline and input set in db
    addPipelineToDB();
    addInputSetToDB("cdng/mergeInputSets/inputSet1.yml");
    addInputSetToDB("cdng/mergeInputSets/inputSet2.yml");
    addInputSetToDB("cdng/mergeInputSets/inputSet3.yml");
    addInputSetToDB("cdng/mergeInputSets/inputSet4.yml");
    addOverlayInputSetToDB();
  }

  private void setupPipelineAndWrongInputSets() throws IOException {
    // Setup pipeline and input set in db
    addPipelineToDB();
    addInputSetToDB("cdng/mergeInputSets/wrongInputSet1.yml");
    addInputSetToDB("cdng/mergeInputSets/wrongInputSet2.yml");
  }

  private void addPipelineToDB() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String pipelineFilename = "cdng/mergeInputSets/pipelineWithRuntimeInput.yml";
    String pipelineYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(pipelineFilename)), StandardCharsets.UTF_8);

    ngPipelineService.createPipeline(pipelineYaml, ACCOUNT_ID, ORG_ID, PROJECT_ID);
  }

  private void addInputSetToDB(String inputSetFileName) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String inputYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);
    CDInputSetEntity cdInputSetEntity =
        InputSetElementMapper.toCDInputSetEntity(ACCOUNT_ID, ORG_ID, PROJECT_ID, PIPELINE_ID, inputYaml);
    inputSetEntityService.create(cdInputSetEntity);
  }

  private void addOverlayInputSetToDB() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String overlayYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource("cdng/mergeInputSets/overlayInputSet.yml")),
            StandardCharsets.UTF_8);
    OverlayInputSetEntity overlayInputSetEntity =
        InputSetElementMapper.toOverlayInputSetEntity(ACCOUNT_ID, ORG_ID, PROJECT_ID, PIPELINE_ID, overlayYaml);
    inputSetEntityService.create(overlayInputSetEntity);
  }

  private void validateAssertOnErrorResponse(Map<String, VisitorErrorResponseWrapper> uuidToErrorResponseMap,
      String key, String expectedIdentifier, String expectedFieldName, String expectedMessage) {
    assertThat(uuidToErrorResponseMap.containsKey(key)).isTrue();
    VisitorErrorResponseWrapper errorResponseWrapper = uuidToErrorResponseMap.get(key);
    assertThat(errorResponseWrapper.getErrors().size()).isEqualTo(1);
    MergeInputSetErrorResponse errorResponse = (MergeInputSetErrorResponse) errorResponseWrapper.getErrors().get(0);
    assertThat(errorResponse.getIdentifierOfErrorSource()).isEqualTo(expectedIdentifier);
    assertThat(errorResponse.getFieldName()).isEqualTo(expectedFieldName);
    assertThat(errorResponse.getMessage()).isEqualTo(expectedMessage);
  }
}