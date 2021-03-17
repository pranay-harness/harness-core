package io.harness.pms.ngpipeline.inputset.service;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetListTypePMS;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetFilterHelper;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public class PMSInputSetServiceImplTest extends PipelineServiceTestBase {
  @Inject PMSInputSetServiceImpl pmsInputSetService;

  String ACCOUNT_ID = "account_id";
  String ORG_IDENTIFIER = "orgId";
  String PROJ_IDENTIFIER = "projId";
  String PIPELINE_IDENTIFIER = "pipeline_identifier";

  String INPUT_SET_IDENTIFIER = "identifier";
  String NAME = "identifier";
  String inputSetFileName = "inputSet1.yml";
  String YAML;

  InputSetEntity inputSetEntity;

  String OVERLAY_INPUT_SET_IDENTIFIER = "overlay-identifier";
  String overlayInputSetFileName = "overlay1.yml";
  List<String> inputSetReferences = ImmutableList.of("inputSet2", "inputSet22");
  String OVERLAY_YAML;

  InputSetEntity overlayInputSetEntity;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    YAML =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(inputSetFileName)), StandardCharsets.UTF_8);
    inputSetEntity = InputSetEntity.builder()
                         .identifier(INPUT_SET_IDENTIFIER)
                         .name(NAME)
                         .yaml(YAML)
                         .inputSetEntityType(InputSetEntityType.INPUT_SET)
                         .accountId(ACCOUNT_ID)
                         .orgIdentifier(ORG_IDENTIFIER)
                         .projectIdentifier(PROJ_IDENTIFIER)
                         .pipelineIdentifier(PIPELINE_IDENTIFIER)
                         .build();

    OVERLAY_YAML = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(overlayInputSetFileName)), StandardCharsets.UTF_8);
    overlayInputSetEntity = InputSetEntity.builder()
                                .identifier(OVERLAY_INPUT_SET_IDENTIFIER)
                                .name(NAME)
                                .yaml(OVERLAY_YAML)
                                .inputSetEntityType(InputSetEntityType.OVERLAY_INPUT_SET)
                                .accountId(ACCOUNT_ID)
                                .orgIdentifier(ORG_IDENTIFIER)
                                .projectIdentifier(PROJ_IDENTIFIER)
                                .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                .inputSetReferences(inputSetReferences)
                                .build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testServiceLayer() {
    List<InputSetEntity> inputSets = ImmutableList.of(inputSetEntity, overlayInputSetEntity);

    for (InputSetEntity entity : inputSets) {
      InputSetEntity createdInputSet = pmsInputSetService.create(entity);
      assertThat(createdInputSet).isNotNull();
      assertThat(createdInputSet.getAccountId()).isEqualTo(entity.getAccountId());
      assertThat(createdInputSet.getOrgIdentifier()).isEqualTo(entity.getOrgIdentifier());
      assertThat(createdInputSet.getProjectIdentifier()).isEqualTo(entity.getProjectIdentifier());
      assertThat(createdInputSet.getIdentifier()).isEqualTo(entity.getIdentifier());
      assertThat(createdInputSet.getName()).isEqualTo(entity.getName());
      assertThat(createdInputSet.getYaml()).isEqualTo(entity.getYaml());
      assertThat(createdInputSet.getVersion()).isEqualTo(0L);

      Optional<InputSetEntity> getInputSet = pmsInputSetService.get(
          ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, entity.getIdentifier(), false);
      assertThat(getInputSet).isPresent();
      assertThat(getInputSet.get().getAccountId()).isEqualTo(createdInputSet.getAccountId());
      assertThat(getInputSet.get().getOrgIdentifier()).isEqualTo(createdInputSet.getOrgIdentifier());
      assertThat(getInputSet.get().getProjectIdentifier()).isEqualTo(createdInputSet.getProjectIdentifier());
      assertThat(getInputSet.get().getIdentifier()).isEqualTo(createdInputSet.getIdentifier());
      assertThat(getInputSet.get().getName()).isEqualTo(createdInputSet.getName());
      assertThat(getInputSet.get().getYaml()).isEqualTo(createdInputSet.getYaml());
      assertThat(getInputSet.get().getVersion()).isEqualTo(0L);

      String DESCRIPTION = "Added a description here";
      InputSetEntity updateInputSetEntity = InputSetEntity.builder()
                                                .identifier(entity.getIdentifier())
                                                .name(NAME)
                                                .description(DESCRIPTION)
                                                .yaml(YAML)
                                                .inputSetEntityType(entity.getInputSetEntityType())
                                                .accountId(ACCOUNT_ID)
                                                .orgIdentifier(ORG_IDENTIFIER)
                                                .projectIdentifier(PROJ_IDENTIFIER)
                                                .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                                .inputSetReferences(entity.getInputSetReferences())
                                                .build();
      InputSetEntity updatedInputSet = pmsInputSetService.update(updateInputSetEntity);
      assertThat(updatedInputSet.getAccountId()).isEqualTo(updateInputSetEntity.getAccountId());
      assertThat(updatedInputSet.getOrgIdentifier()).isEqualTo(updateInputSetEntity.getOrgIdentifier());
      assertThat(updatedInputSet.getProjectIdentifier()).isEqualTo(updateInputSetEntity.getProjectIdentifier());
      assertThat(updatedInputSet.getIdentifier()).isEqualTo(updateInputSetEntity.getIdentifier());
      assertThat(updatedInputSet.getName()).isEqualTo(updateInputSetEntity.getName());
      assertThat(updatedInputSet.getDescription()).isEqualTo(updateInputSetEntity.getDescription());
      assertThat(updatedInputSet.getYaml()).isEqualTo(updateInputSetEntity.getYaml());
      assertThat(updatedInputSet.getVersion()).isEqualTo(1L);

      updateInputSetEntity.setAccountId("newAccountID");
      assertThatThrownBy(() -> pmsInputSetService.update(updateInputSetEntity))
          .isInstanceOf(InvalidRequestException.class);
      updateInputSetEntity.setAccountId(ACCOUNT_ID);

      boolean delete = pmsInputSetService.delete(
          ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, entity.getIdentifier(), 1L);
      assertThat(delete).isTrue();

      Optional<InputSetEntity> deletedInputSet = pmsInputSetService.get(
          ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, entity.getIdentifier(), false);
      assertThat(deletedInputSet.isPresent()).isFalse();
    }
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testList() {
    pmsInputSetService.create(inputSetEntity);
    pmsInputSetService.create(overlayInputSetEntity);

    Criteria criteriaFromFilter = PMSInputSetFilterHelper.createCriteriaForGetList(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, InputSetListTypePMS.ALL, "", false);
    Pageable pageRequest = PageUtils.getPageRequest(0, 10, null);

    Page<InputSetEntity> list = pmsInputSetService.list(criteriaFromFilter, pageRequest);
    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(2);
    assertThat(list.getContent().get(0)).isEqualTo(inputSetEntity);
    assertThat(list.getContent().get(1)).isEqualTo(overlayInputSetEntity);

    InputSetEntity inputSetEntity2 = InputSetEntity.builder()
                                         .identifier(INPUT_SET_IDENTIFIER + "2")
                                         .name(NAME)
                                         .yaml(YAML)
                                         .inputSetEntityType(InputSetEntityType.INPUT_SET)
                                         .accountId(ACCOUNT_ID)
                                         .orgIdentifier(ORG_IDENTIFIER)
                                         .projectIdentifier(PROJ_IDENTIFIER)
                                         .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                         .build();

    pmsInputSetService.create(inputSetEntity2);
    Page<InputSetEntity> list2 = pmsInputSetService.list(criteriaFromFilter, pageRequest);
    assertThat(list2.getContent()).isNotNull();
    assertThat(list2.getContent().size()).isEqualTo(3);
    assertThat(list2.getContent().get(0)).isEqualTo(inputSetEntity);
    assertThat(list2.getContent().get(1)).isEqualTo(overlayInputSetEntity);
    assertThat(list2.getContent().get(2)).isEqualTo(inputSetEntity2);
  }
}