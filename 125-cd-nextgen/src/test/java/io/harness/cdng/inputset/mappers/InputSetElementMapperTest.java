package io.harness.cdng.inputset.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.inputset.beans.resource.InputSetErrorResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetErrorWrapperDTO;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.resource.MergeInputSetResponseDTO;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.rule.Owner;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

public class InputSetElementMapperTest extends CategoryTest {
  InputSetResponseDTO inputSetResponseDTO;
  OverlayInputSetResponseDTO overlayInputSetResponseDTO;

  InputSetSummaryResponseDTO cdInputSetSummaryResponseDTO;
  InputSetSummaryResponseDTO overlayInputSetSummaryResponseDTO;

  OverlayInputSetEntity requestOverlayInputSetEntity;
  OverlayInputSetEntity responseOverlayInputSetEntity;

  CDInputSetEntity requestCdInputSetEntity;
  CDInputSetEntity responseCdInputSetEntity;

  MergeInputSetResponse requestMergeInputResponse;
  MergeInputSetResponseDTO expectedMergeInputResponseDTO;

  private final String IDENTIFIER = "identifier";
  private final String PIPELINE_IDENTIFIER = "pipeline_identifier";
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String NAME = "input set name";
  private final String DESCRIPTION = "input set description";
  private String cdInputSetYaml;
  private String overlayInputSetYaml;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    String inputSetFileName = "input-set-test-file.yaml";
    cdInputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);
    CDInputSet inputSetObject = YamlPipelineUtils.read(cdInputSetYaml, CDInputSet.class);

    String overlayInputSetFileName = "overlay-input-set-test-file.yaml";
    overlayInputSetYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(overlayInputSetFileName)), StandardCharsets.UTF_8);

    inputSetResponseDTO = InputSetResponseDTO.builder()
                              .accountId(ACCOUNT_ID)
                              .orgIdentifier(ORG_IDENTIFIER)
                              .projectIdentifier(PROJ_IDENTIFIER)
                              .identifier(IDENTIFIER)
                              .name(NAME)
                              .description(DESCRIPTION)
                              .pipelineIdentifier(PIPELINE_IDENTIFIER)
                              .inputSetYaml(cdInputSetYaml)
                              .build();

    overlayInputSetResponseDTO = OverlayInputSetResponseDTO.builder()
                                     .accountId(ACCOUNT_ID)
                                     .orgIdentifier(ORG_IDENTIFIER)
                                     .projectIdentifier(PROJ_IDENTIFIER)
                                     .identifier(IDENTIFIER)
                                     .name(NAME)
                                     .description(DESCRIPTION)
                                     .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                     .overlayInputSetYaml(overlayInputSetYaml)
                                     .build();

    cdInputSetSummaryResponseDTO = InputSetSummaryResponseDTO.builder()
                                       .identifier(IDENTIFIER)
                                       .name(NAME)
                                       .description(DESCRIPTION)
                                       .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                       .inputSetType(InputSetEntityType.INPUT_SET)
                                       .build();

    overlayInputSetSummaryResponseDTO = InputSetSummaryResponseDTO.builder()
                                            .identifier(IDENTIFIER)
                                            .name(NAME)
                                            .description(DESCRIPTION)
                                            .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                            .inputSetType(InputSetEntityType.OVERLAY_INPUT_SET)
                                            .build();

    requestMergeInputResponse = MergeInputSetResponse.builder()
                                    .isErrorResponse(true)
                                    .uuidToErrorResponseMap(new HashMap<String, VisitorErrorResponseWrapper>() {
                                      {
                                        put("input1", VisitorErrorResponseWrapper.builder().build());
                                        put("input2", VisitorErrorResponseWrapper.builder().build());
                                      }
                                    })
                                    .errorPipeline(NgPipeline.builder().name("pName").identifier("pID").build())
                                    .build();
    expectedMergeInputResponseDTO =
        MergeInputSetResponseDTO.builder()
            .pipelineYaml("")
            .isErrorResponse(true)
            .inputSetErrorWrapper(InputSetErrorWrapperDTO.builder()
                                      .uuidToErrorResponseMap(new HashMap<String, InputSetErrorResponseDTO>() {
                                        {
                                          put("input1", InputSetErrorResponseDTO.builder().build());
                                          put("input2", InputSetErrorResponseDTO.builder().build());
                                        }
                                      })
                                      .errorPipelineYaml("pipeline:\n  name: pName\n  identifier: pID\n")
                                      .build())
            .build();

    requestCdInputSetEntity = CDInputSetEntity.builder().cdInputSet(inputSetObject).build();
    responseCdInputSetEntity = CDInputSetEntity.builder().cdInputSet(inputSetObject).build();
    setBaseEntityFields(requestCdInputSetEntity, InputSetEntityType.INPUT_SET, cdInputSetYaml);
    setBaseEntityFields(responseCdInputSetEntity, InputSetEntityType.INPUT_SET, cdInputSetYaml);

    requestOverlayInputSetEntity = OverlayInputSetEntity.builder().build();
    responseOverlayInputSetEntity = OverlayInputSetEntity.builder().build();
    setBaseEntityFields(requestOverlayInputSetEntity, InputSetEntityType.OVERLAY_INPUT_SET, overlayInputSetYaml);
    setBaseEntityFields(responseOverlayInputSetEntity, InputSetEntityType.OVERLAY_INPUT_SET, overlayInputSetYaml);
  }

  private void setBaseEntityFields(BaseInputSetEntity baseInputSetEntity, InputSetEntityType type, String yaml) {
    baseInputSetEntity.setAccountId(ACCOUNT_ID);
    baseInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    baseInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    baseInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    baseInputSetEntity.setIdentifier(IDENTIFIER);
    baseInputSetEntity.setName(NAME);
    baseInputSetEntity.setDescription(DESCRIPTION);
    baseInputSetEntity.setInputSetType(type);
    baseInputSetEntity.setInputSetYaml(yaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToCDInputSetEntity() {
    CDInputSetEntity mappedInputSet = InputSetElementMapper.toCDInputSetEntity(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, cdInputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestCdInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestCdInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestCdInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestCdInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestCdInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestCdInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestCdInputSetEntity.getDescription());
    assertThat(mappedInputSet.getInputSetYaml()).isEqualTo(requestCdInputSetEntity.getInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToCDInputSetEntityWithIdentifier() {
    CDInputSetEntity mappedInputSet = InputSetElementMapper.toCDInputSetEntityWithIdentifier(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, cdInputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestCdInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestCdInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestCdInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestCdInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestCdInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestCdInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestCdInputSetEntity.getDescription());
    assertThat(mappedInputSet.getInputSetYaml()).isEqualTo(requestCdInputSetEntity.getInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToOverlayInputSetEntity() {
    OverlayInputSetEntity mappedInputSet = InputSetElementMapper.toOverlayInputSetEntity(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, overlayInputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestOverlayInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestOverlayInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestOverlayInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestOverlayInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestOverlayInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestOverlayInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestOverlayInputSetEntity.getDescription());
    assertThat(mappedInputSet.getInputSetYaml()).isEqualTo(requestOverlayInputSetEntity.getInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToOverlayInputSetEntityWithIdentifier() {
    OverlayInputSetEntity mappedInputSet = InputSetElementMapper.toOverlayInputSetEntityWithIdentifier(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, overlayInputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestOverlayInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestOverlayInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestOverlayInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestOverlayInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestOverlayInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestOverlayInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestOverlayInputSetEntity.getDescription());
    assertThat(mappedInputSet.getInputSetYaml()).isEqualTo(requestOverlayInputSetEntity.getInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testWriteCDInputSetResponseDTO() {
    InputSetResponseDTO response = InputSetElementMapper.writeCDInputSetResponseDTO(responseCdInputSetEntity);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(inputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testWriteOverlayResponseDTO() {
    OverlayInputSetResponseDTO response = InputSetElementMapper.writeOverlayResponseDTO(responseOverlayInputSetEntity);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testWriteSummaryResponseDTO() {
    InputSetSummaryResponseDTO response = InputSetElementMapper.writeSummaryResponseDTO(responseCdInputSetEntity);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(cdInputSetSummaryResponseDTO);

    response = InputSetElementMapper.writeSummaryResponseDTO(responseOverlayInputSetEntity);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(overlayInputSetSummaryResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMergeInputSetResponseDTO() {
    MergeInputSetResponseDTO mergeInputSetResponseDTO =
        InputSetElementMapper.toMergeInputSetResponseDTO(requestMergeInputResponse);

    assertThat(mergeInputSetResponseDTO.getPipelineYaml()).isEqualTo(expectedMergeInputResponseDTO.getPipelineYaml());
    assertThat(mergeInputSetResponseDTO.getPipelineYaml()).isEqualTo(expectedMergeInputResponseDTO.getPipelineYaml());
    assertThat(mergeInputSetResponseDTO.getInputSetErrorWrapper().getErrorPipelineYaml().replaceAll("\"", ""))
        .isEqualTo(expectedMergeInputResponseDTO.getInputSetErrorWrapper().getErrorPipelineYaml());
    assertThat(mergeInputSetResponseDTO.isErrorResponse()).isEqualTo(expectedMergeInputResponseDTO.isErrorResponse());
    assertThat(mergeInputSetResponseDTO.getInputSetErrorWrapper().getUuidToErrorResponseMap().keySet().size())
        .isEqualTo(expectedMergeInputResponseDTO.getInputSetErrorWrapper().getUuidToErrorResponseMap().keySet().size());

    assertThat(mergeInputSetResponseDTO.getInputSetErrorWrapper().getUuidToErrorResponseMap().get("input1"))
        .isEqualTo(expectedMergeInputResponseDTO.getInputSetErrorWrapper().getUuidToErrorResponseMap().get("input1"));
    assertThat(mergeInputSetResponseDTO.getInputSetErrorWrapper().getUuidToErrorResponseMap().get("input2"))
        .isEqualTo(expectedMergeInputResponseDTO.getInputSetErrorWrapper().getUuidToErrorResponseMap().get("input2"));
  }
}