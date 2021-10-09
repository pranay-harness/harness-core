package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType.INPUT_SET;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class ValidateAndMergeHelperTest extends PipelineServiceTestBase {
  @InjectMocks ValidateAndMergeHelper validateAndMergeHelper;
  @Mock PMSPipelineService pmsPipelineService;
  @Mock PMSInputSetService pmsInputSetService;

  private static final String accountId = "accountId";
  private static final String orgId = "orgId";
  private static final String projectId = "projectId";
  private static final String pipelineId = "Test_Pipline11";
  private static final String branch = null;
  private static final String repoId = null;

  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateInputSet() {
    String pipelineFile = "pipeline-extensive.yml";
    String pipelineYaml = readFile(pipelineFile);
    PipelineEntity pipelineEntity = PipelineEntity.builder().yaml(pipelineYaml).build();
    String inputSetFileWithNoProjOrOrg = "inputSet1.yml";
    String inputSetYamlWithNoProjOrOrg = readFile(inputSetFileWithNoProjOrOrg);

    String wrongPipelineId = "wrongPipeline";
    assertThatThrownBy(()
                           -> validateAndMergeHelper.validateInputSet(accountId, orgId, projectId, wrongPipelineId,
                               inputSetYamlWithNoProjOrOrg, branch, repoId))
        .hasMessage("Pipeline identifier in input set does not match");

    assertThatThrownBy(()
                           -> validateAndMergeHelper.validateInputSet(
                               accountId, orgId, projectId, pipelineId, inputSetYamlWithNoProjOrOrg, branch, repoId))
        .hasMessage("Org identifier in input set does not match");

    String inputSetFileWithNoProj = "inputset1-with-org-id.yaml";
    String inputSetYamlWithNoProj = readFile(inputSetFileWithNoProj);
    assertThatThrownBy(()
                           -> validateAndMergeHelper.validateInputSet(
                               accountId, orgId, projectId, pipelineId, inputSetYamlWithNoProj, branch, repoId))
        .hasMessage("Project identifier in input set does not match");

    String inputSetFile = "inputset1-with-org-proj-id.yaml";
    String inputSetYaml = readFile(inputSetFile);
    doReturn(Optional.of(pipelineEntity)).when(pmsPipelineService).get(accountId, orgId, projectId, pipelineId, false);
    InputSetErrorWrapperDTOPMS nullDto =
        validateAndMergeHelper.validateInputSet(accountId, orgId, projectId, pipelineId, inputSetYaml, branch, repoId);
    assertThat(nullDto).isNull();
    verify(pmsPipelineService, times(1)).get(accountId, orgId, projectId, pipelineId, false);

    String inputSetFileWrong = "inputSetWrong1.yml";
    String inputSetYamlWrong = readFile(inputSetFileWrong);
    InputSetErrorWrapperDTOPMS nonNullDto = validateAndMergeHelper.validateInputSet(
        accountId, orgId, projectId, pipelineId, inputSetYamlWrong, branch, repoId);
    assertThat(nonNullDto).isNotNull();
    assertThat(nonNullDto.getUuidToErrorResponseMap().size()).isEqualTo(1);
    assertThat(nonNullDto.getUuidToErrorResponseMap().containsKey(
                   "pipeline.stages.qaStage.spec.execution.steps.httpStep1.spec.method"))
        .isTrue();
    verify(pmsPipelineService, times(2)).get(accountId, orgId, projectId, pipelineId, false);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateOverlayInputSet() {
    String overlayInputSetYamlWithoutReferences = getOverlayInputSetWithAllIds(false);
    assertThatThrownBy(()
                           -> validateAndMergeHelper.validateOverlayInputSet(
                               accountId, orgId, projectId, pipelineId, overlayInputSetYamlWithoutReferences))
        .hasMessage("Input Set References can't be empty");

    String overlayInputSetYamlWithoutOrgId = getOverlayInputSetYaml(true, false, true, true, false);
    assertThatThrownBy(()
                           -> validateAndMergeHelper.validateOverlayInputSet(
                               accountId, orgId, projectId, pipelineId, overlayInputSetYamlWithoutOrgId))
        .hasMessage("Org identifier in input set does not match");

    String overlayInputSetYamlWithoutProjectId = getOverlayInputSetYaml(true, true, false, true, false);
    assertThatThrownBy(()
                           -> validateAndMergeHelper.validateOverlayInputSet(
                               accountId, orgId, projectId, pipelineId, overlayInputSetYamlWithoutProjectId))
        .hasMessage("Project identifier in input set does not match");

    String overlayInputSetYamlWithoutPipelineId = getOverlayInputSetYaml(true, true, true, false, false);
    assertThatThrownBy(()
                           -> validateAndMergeHelper.validateOverlayInputSet(
                               accountId, orgId, projectId, pipelineId, overlayInputSetYamlWithoutPipelineId))
        .hasMessage("Pipeline identifier in input set does not match");

    String inputSetFile1 = "inputset1-with-org-proj-id.yaml";
    String inputSetYaml1 = readFile(inputSetFile1);
    String identifier1 = "input1";
    InputSetEntity inputSetEntity1 = InputSetEntity.builder().inputSetEntityType(INPUT_SET).yaml(inputSetYaml1).build();
    doReturn(Optional.of(inputSetEntity1))
        .when(pmsInputSetService)
        .get(accountId, orgId, projectId, pipelineId, identifier1, false);

    String inputSetFile2 = "inputSetWrong1.yml";
    String inputSetYaml2 = readFile(inputSetFile2);
    String identifier2 = "thisInputSetIsWrong";
    InputSetEntity inputSetEntity2 = InputSetEntity.builder().inputSetEntityType(INPUT_SET).yaml(inputSetYaml2).build();
    doReturn(Optional.of(inputSetEntity2))
        .when(pmsInputSetService)
        .get(accountId, orgId, projectId, pipelineId, identifier2, false);

    String overlayInputSetYaml = getOverlayInputSetWithAllIds(true);
    Map<String, String> noInvalidReferences =
        validateAndMergeHelper.validateOverlayInputSet(accountId, orgId, projectId, pipelineId, overlayInputSetYaml);
    assertThat(noInvalidReferences.size()).isEqualTo(0);
    verify(pmsInputSetService, times(1)).get(accountId, orgId, projectId, pipelineId, identifier1, false);
    verify(pmsInputSetService, times(1)).get(accountId, orgId, projectId, pipelineId, identifier2, false);

    InputSetEntity inputSetEntityInvalid =
        InputSetEntity.builder().inputSetEntityType(INPUT_SET).yaml(inputSetYaml2).isInvalid(true).build();
    doReturn(Optional.of(inputSetEntityInvalid))
        .when(pmsInputSetService)
        .get(accountId, orgId, projectId, pipelineId, identifier2, false);
    Map<String, String> oneInvalidReference =
        validateAndMergeHelper.validateOverlayInputSet(accountId, orgId, projectId, pipelineId, overlayInputSetYaml);
    assertThat(oneInvalidReference.size()).isEqualTo(1);
    verify(pmsInputSetService, times(2)).get(accountId, orgId, projectId, pipelineId, identifier1, false);
    verify(pmsInputSetService, times(2)).get(accountId, orgId, projectId, pipelineId, identifier2, false);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateNonExistentReferencesInOverlayInputSet() {
    String nonExistentReference = "doesNotExist";
    String overlayYaml = getOverlayInputSetWithNonExistentReference();
    doReturn(Optional.empty())
        .when(pmsInputSetService)
        .get(accountId, orgId, projectId, pipelineId, nonExistentReference, false);
    Map<String, String> nonExistentReferenceMap =
        validateAndMergeHelper.validateOverlayInputSet(accountId, orgId, projectId, pipelineId, overlayYaml);
    assertThat(nonExistentReferenceMap).hasSize(1);
    assertThat(nonExistentReferenceMap.get(nonExistentReference)).isEqualTo("Reference does not exist");
    verify(pmsInputSetService, times(1)).get(accountId, orgId, projectId, pipelineId, nonExistentReference, false);
  }

  private String getOverlayInputSetWithNonExistentReference() {
    return getOverlayInputSetYaml(true, true, true, true, true);
  }

  private String getOverlayInputSetWithAllIds(boolean hasReferences) {
    return getOverlayInputSetYaml(hasReferences, true, true, true, false);
  }

  private String getOverlayInputSetYaml(
      boolean hasReferences, boolean hasOrg, boolean hasProj, boolean hasPipeline, boolean nonExistentReference) {
    String base = "overlayInputSet:\n"
        + "  identifier: overlay1\n"
        + "  name : thisName\n";
    String orgId = "  orgIdentifier: orgId\n";
    String projectId = "  projectIdentifier: projectId\n";
    String pipelineId = "  pipelineIdentifier: Test_Pipline11\n";
    String references = "  inputSetReferences:\n"
        + (nonExistentReference ? "    - doesNotExist"
                                : ("    - input1\n"
                                    + "    - thisInputSetIsWrong"));
    String noReferences = "  inputSetReferences: []\n";

    return base + (hasOrg ? orgId : "") + (hasProj ? projectId : "") + (hasPipeline ? pipelineId : "")
        + (hasReferences ? references : noReferences);
  }
}