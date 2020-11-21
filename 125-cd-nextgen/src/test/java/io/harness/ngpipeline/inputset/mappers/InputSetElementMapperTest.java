package io.harness.ngpipeline.inputset.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.inputset.beans.resource.InputSetErrorResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetErrorWrapperDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.MergeInputSetResponseDTO;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.rule.Owner;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InputSetElementMapperTest extends CategoryTest {
  InputSetResponseDTO inputSetResponseDTO;
  OverlayInputSetResponseDTO overlayInputSetResponseDTO;

  InputSetSummaryResponseDTO cdInputSetSummaryResponseDTO;
  InputSetSummaryResponseDTO overlayInputSetSummaryResponseDTO;

  OverlayInputSetEntity requestOverlayInputSetEntity;
  OverlayInputSetEntity responseOverlayInputSetEntity;

  InputSetEntity requestInputSetEntity;
  InputSetEntity responseInputSetEntity;

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
  private List<NGTag> tags;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    tags = Collections.singletonList(NGTag.builder().key("company").value("harness").build());

    String inputSetFileName = "input-set-test-file.yaml";
    cdInputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);
    InputSetConfig inputSetObject = YamlPipelineUtils.read(cdInputSetYaml, InputSetConfig.class);

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
                              .isErrorResponse(false)
                              .tags(TagMapper.convertToMap(tags))
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
                                     .isErrorResponse(false)
                                     .tags(TagMapper.convertToMap(tags))
                                     .build();

    cdInputSetSummaryResponseDTO = InputSetSummaryResponseDTO.builder()
                                       .identifier(IDENTIFIER)
                                       .name(NAME)
                                       .description(DESCRIPTION)
                                       .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                       .inputSetType(InputSetEntityType.INPUT_SET)
                                       .tags(TagMapper.convertToMap(tags))
                                       .build();

    overlayInputSetSummaryResponseDTO = InputSetSummaryResponseDTO.builder()
                                            .identifier(IDENTIFIER)
                                            .name(NAME)
                                            .description(DESCRIPTION)
                                            .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                            .inputSetType(InputSetEntityType.OVERLAY_INPUT_SET)
                                            .tags(TagMapper.convertToMap(tags))
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

    requestInputSetEntity = InputSetEntity.builder().inputSetConfig(inputSetObject).build();
    responseInputSetEntity = InputSetEntity.builder().inputSetConfig(inputSetObject).build();
    setBaseEntityFields(requestInputSetEntity, InputSetEntityType.INPUT_SET, cdInputSetYaml);
    setBaseEntityFields(responseInputSetEntity, InputSetEntityType.INPUT_SET, cdInputSetYaml);

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
    baseInputSetEntity.setTags(tags);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToCDInputSetEntity() {
    InputSetEntity mappedInputSet = InputSetElementMapper.toInputSetEntity(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, cdInputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestInputSetEntity.getDescription());
    assertThat(mappedInputSet.getInputSetYaml()).isEqualTo(requestInputSetEntity.getInputSetYaml());
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToCDInputSetEntityWithIdentifier() {
    InputSetEntity mappedInputSet = InputSetElementMapper.toInputSetEntityWithIdentifier(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, cdInputSetYaml);
    assertThat(mappedInputSet).isNotNull();
    assertThat(mappedInputSet.getIdentifier()).isEqualTo(requestInputSetEntity.getIdentifier());
    assertThat(mappedInputSet.getAccountId()).isEqualTo(requestInputSetEntity.getAccountId());
    assertThat(mappedInputSet.getOrgIdentifier()).isEqualTo(requestInputSetEntity.getOrgIdentifier());
    assertThat(mappedInputSet.getProjectIdentifier()).isEqualTo(requestInputSetEntity.getProjectIdentifier());
    assertThat(mappedInputSet.getPipelineIdentifier()).isEqualTo(requestInputSetEntity.getPipelineIdentifier());
    assertThat(mappedInputSet.getName()).isEqualTo(requestInputSetEntity.getName());
    assertThat(mappedInputSet.getDescription()).isEqualTo(requestInputSetEntity.getDescription());
    assertThat(mappedInputSet.getInputSetYaml()).isEqualTo(requestInputSetEntity.getInputSetYaml());
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
    InputSetResponseDTO response = InputSetElementMapper.writeInputSetResponseDTO(responseInputSetEntity, null);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(inputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testWriteOverlayResponseDTO() {
    OverlayInputSetResponseDTO response =
        InputSetElementMapper.writeOverlayResponseDTO(responseOverlayInputSetEntity, null);
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testWriteSummaryResponseDTO() {
    InputSetSummaryResponseDTO response = InputSetElementMapper.writeSummaryResponseDTO(responseInputSetEntity);
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
