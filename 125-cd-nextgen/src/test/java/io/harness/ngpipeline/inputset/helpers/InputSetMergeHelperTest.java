package io.harness.ngpipeline.inputset.helpers;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.inputset.mappers.InputSetElementMapper;
import io.harness.ngpipeline.inputset.services.InputSetEntityService;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.pipeline.mappers.PipelineDtoMapper;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.walktree.visitor.mergeinputset.beans.MergeInputSetErrorResponse;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import retrofit2.Call;
import retrofit2.Response;

@Slf4j
public class InputSetMergeHelperTest extends CDNGBaseTest {
  @Inject InputSetMergeHelper inputSetMergeHelper;
  @Inject NGPipelineService ngPipelineService;
  @Inject InputSetEntityService inputSetEntityService;
  @Mock EntitySetupUsageClient entitySetupUsageClient;

  private final String ACCOUNT_ID = "accountId";
  private final String ORG_ID = "orgId";
  private final String PROJECT_ID = "projId";
  private final String PIPELINE_ID = "myPipeline1";

  @Before
  public void setUp() {
    Reflect.on(ngPipelineService).set("entitySetupUsageClient", entitySetupUsageClient);
    Reflect.on(inputSetEntityService).set("entitySetupUsageClient", entitySetupUsageClient);
  }

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
    Call<ResponseDTO<Page<EntitySetupUsageDTO>>> request = mock(Call.class);
    doReturn(request).when(entitySetupUsageClient).save(any());
    try {
      when(request.execute()).thenReturn(Response.success(ResponseDTO.newResponse(Page.empty())));
    } catch (IOException ex) {
      log.info("Encountered exception ", ex);
    }
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
    Call<ResponseDTO<Page<EntitySetupUsageDTO>>> request = mock(Call.class);
    doReturn(request).when(entitySetupUsageClient).save(any());
    try {
      when(request.execute()).thenReturn(Response.success(ResponseDTO.newResponse(Page.empty())));
    } catch (IOException ex) {
      log.info("Encountered exception ", ex);
    }
    setupPipelineAndWrongInputSets();

    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputIdentifierList(ACCOUNT_ID, ORG_ID, PROJECT_ID, PIPELINE_ID,
            Stream.of("wrongInput1", "wrongInput2").collect(Collectors.toList()), false, true);

    assertThat(mergeInputSetResponse.isErrorResponse()).isTrue();
    String errorPipelineYaml = JsonPipelineUtils.writeYamlString(mergeInputSetResponse.getErrorPipeline())
                                   .replaceAll("---\n", "")
                                   .replaceAll("\"", "");
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

    key = "myPipeline1.stages.qa.spec.service.serviceDefinition.spec.artifacts.sidecars.sidecar.spec.imagePath";
    expectedFieldName = "imagePath";
    expectedMessage = "Value inside input set cannot be another runtime expression.";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key = "myPipeline1.stages.qa.spec.infrastructure.infrastructureDefinition.spec.connectorRef";
    expectedFieldName = "connectorRef";
    validateAssertOnErrorResponse(uuidToErrorResponseMap, key, expectedIdentifier, expectedFieldName, expectedMessage);

    key = "myPipeline1.stages.prod.spec.service.stageOverrides.manifests.prodOverride.spec.store.spec.connectorRef";
    expectedIdentifier = "wrongInput2";
    expectedFieldName = "connectorRef";
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
    String mergedPipelineYaml = JsonPipelineUtils.writeYamlString(mergeInputSetResponse.getMergedPipeline())
                                    .replaceAll("---\n", "")
                                    .replaceAll("\"", "");
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
    String mergedPipelineYaml = JsonPipelineUtils.writeYamlString(mergeInputSetResponse.getMergedPipeline())
                                    .replaceAll("---\n", "")
                                    .replaceAll("\"", "");
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

    ngPipelineService.create(PipelineDtoMapper.toPipelineEntity(ACCOUNT_ID, ORG_ID, PROJECT_ID, pipelineYaml));
  }

  private void addInputSetToDB(String inputSetFileName) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String inputYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);
    InputSetEntity inputSetEntity =
        InputSetElementMapper.toInputSetEntity(ACCOUNT_ID, ORG_ID, PROJECT_ID, PIPELINE_ID, inputYaml);
    inputSetEntityService.create(inputSetEntity);
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

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testRemoveRuntimeInputFromInputSet() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "input-set-with-runtime-input.yaml";
    String inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    InputSetEntity entity =
        InputSetEntity.builder().inputSetConfig(YamlPipelineUtils.read(inputSetYaml, InputSetConfig.class)).build();
    entity.setIdentifier("identifier");
    entity = inputSetMergeHelper.removeRuntimeInputs(entity);

    String clearedFileName = "input-set-with-runtime-input-cleared.yaml";
    String clearedInputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(clearedFileName)), StandardCharsets.UTF_8);
    assertThat(entity.getInputSetYaml()).isEqualTo(clearedInputSetYaml);
  }
}
