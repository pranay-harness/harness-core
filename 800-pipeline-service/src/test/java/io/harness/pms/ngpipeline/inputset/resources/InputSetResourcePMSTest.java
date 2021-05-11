package io.harness.pms.ngpipeline.inputset.resources;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAMARTH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTOPMS;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class InputSetResourcePMSTest extends PipelineServiceTestBase {
  InputSetResourcePMS inputSetResourcePMS;
  @Mock PMSInputSetService pmsInputSetService;
  @Mock ValidateAndMergeHelper validateAndMergeHelper;

  private static final String ACCOUNT_ID = "accountId";
  private static final String ORG_IDENTIFIER = "orgId";
  private static final String PROJ_IDENTIFIER = "projId";
  private static final String PIPELINE_IDENTIFIER = "pipeId";
  private static final String INPUT_SET_ID = "inputSetId";
  private static final String INVALID_INPUT_SET_ID = "invalidInputSetId";
  private static final String OVERLAY_INPUT_SET_ID = "overlayInputSetId";
  private static final String INVALID_OVERLAY_INPUT_SET_ID = "invalidOverlayInputSetId";
  private String inputSetYaml;
  private String overlayInputSetYaml;

  InputSetEntity inputSetEntity;
  InputSetEntity overlayInputSetEntity;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    inputSetResourcePMS = new InputSetResourcePMS(pmsInputSetService, validateAndMergeHelper);
    ClassLoader classLoader = this.getClass().getClassLoader();
    String inputSetFilename = "inputSet1.yml";
    inputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFilename)), StandardCharsets.UTF_8);
    String overlayInputSetFilename = "overlay1.yml";
    overlayInputSetYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(overlayInputSetFilename)), StandardCharsets.UTF_8);

    inputSetEntity = InputSetEntity.builder()
                         .accountId(ACCOUNT_ID)
                         .orgIdentifier(ORG_IDENTIFIER)
                         .projectIdentifier(PROJ_IDENTIFIER)
                         .pipelineIdentifier(PIPELINE_IDENTIFIER)
                         .identifier(INPUT_SET_ID)
                         .name(INPUT_SET_ID)
                         .yaml(inputSetYaml)
                         .inputSetEntityType(InputSetEntityType.INPUT_SET)
                         .version(1L)
                         .build();

    overlayInputSetEntity = InputSetEntity.builder()
                                .accountId(ACCOUNT_ID)
                                .orgIdentifier(ORG_IDENTIFIER)
                                .projectIdentifier(PROJ_IDENTIFIER)
                                .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                .identifier(OVERLAY_INPUT_SET_ID)
                                .name(OVERLAY_INPUT_SET_ID)
                                .yaml(overlayInputSetYaml)
                                .inputSetEntityType(InputSetEntityType.OVERLAY_INPUT_SET)
                                .version(1L)
                                .build();
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetInputSet() {
    doReturn(Optional.of(inputSetEntity))
        .when(pmsInputSetService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, INPUT_SET_ID, false);

    ResponseDTO<InputSetResponseDTOPMS> responseDTO = inputSetResourcePMS.getInputSet(
        INPUT_SET_ID, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false, null);

    assertThat(responseDTO.getData().getVersion()).isEqualTo(1L);
    assertThat(responseDTO.getData().getInputSetYaml()).isEqualTo(inputSetYaml);
    assertThat(responseDTO.getData().getName()).isEqualTo(INPUT_SET_ID);
    assertThat(responseDTO.getData().getIdentifier()).isEqualTo(INPUT_SET_ID);
    assertThat(responseDTO.getData().getPipelineIdentifier()).isEqualTo(PIPELINE_IDENTIFIER);
    assertThat(responseDTO.getData().getProjectIdentifier()).isEqualTo(PROJ_IDENTIFIER);
    assertThat(responseDTO.getData().getOrgIdentifier()).isEqualTo(ORG_IDENTIFIER);
    assertThat(responseDTO.getData().getAccountId()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetInputSetWithInvalidInputSetId() {
    doReturn(Optional.empty())
        .when(pmsInputSetService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, INVALID_INPUT_SET_ID, false);

    assertThatThrownBy(()
                           -> inputSetResourcePMS.getInputSet(INVALID_INPUT_SET_ID, ACCOUNT_ID, ORG_IDENTIFIER,
                               PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            String.format("InputSet with the given ID: %s does not exist or has been deleted", INVALID_INPUT_SET_ID));
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetOverlayInputSet() {
    doReturn(Optional.of(overlayInputSetEntity))
        .when(pmsInputSetService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, OVERLAY_INPUT_SET_ID, false);

    ResponseDTO<OverlayInputSetResponseDTOPMS> responseDTO = inputSetResourcePMS.getOverlayInputSet(
        OVERLAY_INPUT_SET_ID, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false, null);

    assertThat(responseDTO.getData().getVersion()).isEqualTo(1L);
    assertThat(responseDTO.getData().getOverlayInputSetYaml()).isEqualTo(overlayInputSetYaml);
    assertThat(responseDTO.getData().getName()).isEqualTo(OVERLAY_INPUT_SET_ID);
    assertThat(responseDTO.getData().getIdentifier()).isEqualTo(OVERLAY_INPUT_SET_ID);
    assertThat(responseDTO.getData().getPipelineIdentifier()).isEqualTo(PIPELINE_IDENTIFIER);
    assertThat(responseDTO.getData().getProjectIdentifier()).isEqualTo(PROJ_IDENTIFIER);
    assertThat(responseDTO.getData().getOrgIdentifier()).isEqualTo(ORG_IDENTIFIER);
    assertThat(responseDTO.getData().getAccountId()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetOverlayInputSetWithInvalidInputSetId() {
    doReturn(Optional.empty())
        .when(pmsInputSetService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, INVALID_OVERLAY_INPUT_SET_ID, false);

    assertThatThrownBy(()
                           -> inputSetResourcePMS.getOverlayInputSet(INVALID_OVERLAY_INPUT_SET_ID, ACCOUNT_ID,
                               ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(String.format(
            "InputSet with the given ID: %s does not exist or has been deleted", INVALID_OVERLAY_INPUT_SET_ID));
  }
}
