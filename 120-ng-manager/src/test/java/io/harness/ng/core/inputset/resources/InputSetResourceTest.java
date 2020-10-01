package io.harness.ng.core.inputset.resources;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.inputset.beans.resource.InputSetListType;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.resource.MergeInputSetRequestDTO;
import io.harness.cdng.inputset.beans.resource.MergeInputSetResponseDTO;
import io.harness.cdng.inputset.helpers.InputSetMergeHelper;
import io.harness.cdng.inputset.mappers.InputSetFilterHelper;
import io.harness.cdng.inputset.services.impl.InputSetEntityServiceImpl;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.service.PipelineService;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InputSetResourceTest extends CategoryTest {
  @Mock InputSetEntityServiceImpl inputSetEntityService;
  @Mock PipelineService ngPipelineService;
  @Mock InputSetMergeHelper inputSetMergeHelper;
  @InjectMocks InputSetResource inputSetResource;

  InputSetResponseDTO cdInputSetResponseDTO;
  OverlayInputSetResponseDTO overlayInputSetResponseDTO;
  InputSetSummaryResponseDTO inputSetSummaryResponseDTO;

  CDInputSetEntity cdInputSetEntity;
  OverlayInputSetEntity overlayInputSetEntity;

  CDPipelineResponseDTO cdPipelineResponseDTO;

  private final String IDENTIFIER = "identifier";
  private final String PIPELINE_IDENTIFIER = "pipeline_identifier";
  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private String cdInputSetYaml;
  private String overlayInputSetYaml;
  private String pipelineYaml;
  private NgPipeline ngPipeline;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = getClass().getClassLoader();

    String inputSetFileName = "input-set-test-file.yaml";
    cdInputSetYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);

    String overlayInputSetFileName = "overlay-input-set-test-file.yaml";
    overlayInputSetYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(overlayInputSetFileName)), StandardCharsets.UTF_8);

    pipelineYaml = "pipeline:\n  name: pName\n  identifier: pID\n";
    ngPipeline = NgPipeline.builder().name("pName").identifier("pID").build();

    cdInputSetResponseDTO = InputSetResponseDTO.builder()
                                .accountId(ACCOUNT_ID)
                                .orgIdentifier(ORG_IDENTIFIER)
                                .projectIdentifier(PROJ_IDENTIFIER)
                                .identifier(IDENTIFIER)
                                .name(IDENTIFIER)
                                .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                .inputSetYaml(cdInputSetYaml)
                                .build();

    overlayInputSetResponseDTO = OverlayInputSetResponseDTO.builder()
                                     .accountId(ACCOUNT_ID)
                                     .orgIdentifier(ORG_IDENTIFIER)
                                     .projectIdentifier(PROJ_IDENTIFIER)
                                     .identifier(IDENTIFIER)
                                     .name(IDENTIFIER)
                                     .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                     .overlayInputSetYaml(overlayInputSetYaml)
                                     .build();

    inputSetSummaryResponseDTO = InputSetSummaryResponseDTO.builder()
                                     .identifier(IDENTIFIER)
                                     .name(IDENTIFIER)
                                     .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                     .inputSetType(InputSetEntityType.INPUT_SET)
                                     .build();

    cdInputSetEntity = CDInputSetEntity.builder().build();
    setBaseEntityFields(cdInputSetEntity, InputSetEntityType.INPUT_SET, cdInputSetYaml);

    overlayInputSetEntity = OverlayInputSetEntity.builder().build();
    setBaseEntityFields(overlayInputSetEntity, InputSetEntityType.OVERLAY_INPUT_SET, overlayInputSetYaml);

    cdPipelineResponseDTO = CDPipelineResponseDTO.builder().yamlPipeline(pipelineYaml).build();
  }

  private void setBaseEntityFields(BaseInputSetEntity baseInputSetEntity, InputSetEntityType type, String yaml) {
    baseInputSetEntity.setAccountId(ACCOUNT_ID);
    baseInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    baseInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    baseInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    baseInputSetEntity.setIdentifier(IDENTIFIER);
    baseInputSetEntity.setName(IDENTIFIER);
    baseInputSetEntity.setInputSetType(type);
    baseInputSetEntity.setInputSetYaml(yaml);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetCDInputSet() {
    doReturn(Optional.of(cdInputSetEntity))
        .when(inputSetEntityService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);

    InputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource
            .getCDInputSet(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(cdInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetOverlayInputSet() {
    doReturn(Optional.of(overlayInputSetEntity))
        .when(inputSetEntityService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);

    OverlayInputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource
            .getOverlayInputSet(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreateCDInputSet() {
    doReturn(cdInputSetEntity).when(inputSetEntityService).create(any());

    InputSetResponseDTO inputSetResponseDTO1 =
        inputSetResource
            .createCDInputSet(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, cdInputSetYaml)
            .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(cdInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreateOverlayInputSet() {
    doReturn(overlayInputSetEntity).when(inputSetEntityService).create(any());

    OverlayInputSetResponseDTO inputSetResponseDTO1 = inputSetResource
                                                          .createOverlayInputSet(ACCOUNT_ID, ORG_IDENTIFIER,
                                                              PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, overlayInputSetYaml)
                                                          .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpdateCDInputSet() {
    doReturn(cdInputSetEntity).when(inputSetEntityService).update(any());

    InputSetResponseDTO inputSetResponseDTO1 = inputSetResource
                                                   .updateCDInputSet(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER,
                                                       PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, cdInputSetYaml)
                                                   .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(cdInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testUpdateOverlayInputSet() {
    doReturn(overlayInputSetEntity).when(inputSetEntityService).update(any());

    OverlayInputSetResponseDTO inputSetResponseDTO1 = inputSetResource
                                                          .updateOverlayInputSet(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER,
                                                              PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, overlayInputSetYaml)
                                                          .getData();

    assertThat(inputSetResponseDTO1).isEqualTo(overlayInputSetResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testDelete() {
    doReturn(true)
        .when(inputSetEntityService)
        .delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER);

    Boolean response =
        inputSetResource.delete(IDENTIFIER, ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER).getData();

    assertThat(response).isTrue();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testListServicesWithDESCSort() {
    Criteria criteria = InputSetFilterHelper.createCriteriaForGetList("", "", "", "", InputSetListType.ALL, "", false);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, BaseInputSetEntityKeys.createdAt));
    final Page<CDInputSetEntity> serviceList = new PageImpl<>(Collections.singletonList(cdInputSetEntity), pageable, 1);
    doReturn(serviceList).when(inputSetEntityService).list(criteria, pageable);

    List<InputSetSummaryResponseDTO> content =
        inputSetResource.listInputSetsForPipeline(0, 10, "", "", "", "", InputSetListType.ALL, "", null)
            .getData()
            .getContent();

    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(inputSetSummaryResponseDTO);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetTemplateFromPipeline() {
    doReturn(Optional.of(cdPipelineResponseDTO)).when(ngPipelineService).getPipeline(any(), any(), any(), any());
    doReturn("").when(inputSetMergeHelper).getTemplateFromPipeline(pipelineYaml);

    String responseTemplateYaml =
        inputSetResource.getTemplateFromPipeline(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER)
            .getData()
            .getInputSetTemplateYaml();
    assertThat(responseTemplateYaml).isEqualTo("");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetMergeInputSetFromPipelineTemplate() {
    MergeInputSetResponse mergeInputSetResponse = MergeInputSetResponse.builder().mergedPipeline(ngPipeline).build();
    List<String> inputSetReferences = Arrays.asList("input1", "input2");
    MergeInputSetRequestDTO mergeInputSetRequestDTO =
        MergeInputSetRequestDTO.builder().inputSetReferences(inputSetReferences).build();

    doReturn(mergeInputSetResponse)
        .when(inputSetMergeHelper)
        .getMergePipelineYamlFromInputIdentifierList(
            ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, inputSetReferences, true, false);
    MergeInputSetResponseDTO mergePipelineTemplate =
        inputSetResource
            .getMergeInputSetFromPipelineTemplate(
                ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false, mergeInputSetRequestDTO)
            .getData();
    assertThat(mergePipelineTemplate.getPipelineYaml().replaceAll("\"", "")).isEqualTo(pipelineYaml);
  }
}