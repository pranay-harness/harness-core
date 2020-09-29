package io.harness.cdng.inputset.services.impl;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.resource.InputSetListType;
import io.harness.cdng.inputset.mappers.InputSetElementMapper;
import io.harness.cdng.inputset.mappers.InputSetFilterHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputSetEntityServiceImplTest extends CDNGBaseTest {
  @Inject InputSetEntityServiceImpl inputSetEntityService;

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidatePresenceOfRequiredFields() {
    assertThatThrownBy(() -> inputSetEntityService.validatePresenceOfRequiredFields("", null, "2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("One of the required fields is null.");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testServiceLayerOnCDInputSet() {
    String ORG_IDENTIFIER = "orgId";
    String PROJ_IDENTIFIER = "projId";
    String PIPELINE_IDENTIFIER = "pipeline_identifier";
    String IDENTIFIER = "identifier";
    String ACCOUNT_ID = "account_id";

    CDInputSetEntity cdInputSetEntity = CDInputSetEntity.builder().build();
    cdInputSetEntity.setAccountId(ACCOUNT_ID);
    cdInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    cdInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    cdInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    cdInputSetEntity.setIdentifier(IDENTIFIER);
    cdInputSetEntity.setName("Input Set");

    // Create
    BaseInputSetEntity createdInputSet = inputSetEntityService.create(cdInputSetEntity);
    assertThat(createdInputSet).isNotNull();
    assertThat(createdInputSet.getAccountId()).isEqualTo(cdInputSetEntity.getAccountId());
    assertThat(createdInputSet.getOrgIdentifier()).isEqualTo(cdInputSetEntity.getOrgIdentifier());
    assertThat(createdInputSet.getProjectIdentifier()).isEqualTo(cdInputSetEntity.getProjectIdentifier());
    assertThat(createdInputSet.getIdentifier()).isEqualTo(cdInputSetEntity.getIdentifier());
    assertThat(createdInputSet.getName()).isEqualTo(cdInputSetEntity.getName());

    // Get
    Optional<BaseInputSetEntity> getInputSet =
        inputSetEntityService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(getInputSet).isPresent();
    assertThat(getInputSet.get()).isEqualTo(createdInputSet);

    // Update
    CDInputSetEntity updatedCdInputSetEntity = CDInputSetEntity.builder().build();
    updatedCdInputSetEntity.setAccountId(ACCOUNT_ID);
    updatedCdInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    updatedCdInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    updatedCdInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    updatedCdInputSetEntity.setIdentifier(IDENTIFIER);
    updatedCdInputSetEntity.setName("Input Set Updated");

    BaseInputSetEntity updatedCdInputSetResponse = inputSetEntityService.update(updatedCdInputSetEntity);
    assertThat(updatedCdInputSetResponse.getAccountId()).isEqualTo(updatedCdInputSetEntity.getAccountId());
    assertThat(updatedCdInputSetResponse.getOrgIdentifier()).isEqualTo(updatedCdInputSetEntity.getOrgIdentifier());
    assertThat(updatedCdInputSetResponse.getProjectIdentifier())
        .isEqualTo(updatedCdInputSetEntity.getProjectIdentifier());
    assertThat(updatedCdInputSetResponse.getIdentifier()).isEqualTo(updatedCdInputSetEntity.getIdentifier());
    assertThat(updatedCdInputSetResponse.getName()).isEqualTo(updatedCdInputSetEntity.getName());
    assertThat(updatedCdInputSetResponse.getDescription()).isEqualTo(updatedCdInputSetEntity.getDescription());

    // Update non existing entity
    updatedCdInputSetEntity.setAccountId("newAccountId");
    assertThatThrownBy(() -> inputSetEntityService.update(updatedCdInputSetEntity))
        .isInstanceOf(InvalidRequestException.class);
    updatedCdInputSetEntity.setAccountId(ACCOUNT_ID);

    // Delete
    boolean delete =
        inputSetEntityService.delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER);
    assertThat(delete).isTrue();

    Optional<BaseInputSetEntity> deletedInputSet =
        inputSetEntityService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(deletedInputSet.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testServiceLayerOnOverlayInputSet() {
    String ORG_IDENTIFIER = "orgId";
    String PROJ_IDENTIFIER = "projId";
    String PIPELINE_IDENTIFIER = "pipeline_identifier";
    String IDENTIFIER = "identifier";
    String ACCOUNT_ID = "account_id";

    OverlayInputSetEntity overlayInputSetEntity = OverlayInputSetEntity.builder().build();
    overlayInputSetEntity.setAccountId(ACCOUNT_ID);
    overlayInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    overlayInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    overlayInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    overlayInputSetEntity.setIdentifier(IDENTIFIER);
    overlayInputSetEntity.setName("Input Set");

    // Create
    BaseInputSetEntity createdInputSet = inputSetEntityService.create(overlayInputSetEntity);
    assertThat(createdInputSet).isNotNull();
    assertThat(createdInputSet.getAccountId()).isEqualTo(overlayInputSetEntity.getAccountId());
    assertThat(createdInputSet.getOrgIdentifier()).isEqualTo(overlayInputSetEntity.getOrgIdentifier());
    assertThat(createdInputSet.getProjectIdentifier()).isEqualTo(overlayInputSetEntity.getProjectIdentifier());
    assertThat(createdInputSet.getIdentifier()).isEqualTo(overlayInputSetEntity.getIdentifier());
    assertThat(createdInputSet.getName()).isEqualTo(overlayInputSetEntity.getName());

    // Get
    Optional<BaseInputSetEntity> getInputSet =
        inputSetEntityService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(getInputSet).isPresent();
    assertThat(getInputSet.get()).isEqualTo(createdInputSet);

    // Update
    OverlayInputSetEntity updatedOverlayInputSetEntity = OverlayInputSetEntity.builder().build();
    updatedOverlayInputSetEntity.setAccountId(ACCOUNT_ID);
    updatedOverlayInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    updatedOverlayInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    updatedOverlayInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    updatedOverlayInputSetEntity.setIdentifier(IDENTIFIER);
    updatedOverlayInputSetEntity.setName("Input Set Updated");

    BaseInputSetEntity updatedCdInputSetResponse = inputSetEntityService.update(updatedOverlayInputSetEntity);
    assertThat(updatedCdInputSetResponse.getAccountId()).isEqualTo(updatedOverlayInputSetEntity.getAccountId());
    assertThat(updatedCdInputSetResponse.getOrgIdentifier()).isEqualTo(updatedOverlayInputSetEntity.getOrgIdentifier());
    assertThat(updatedCdInputSetResponse.getProjectIdentifier())
        .isEqualTo(updatedOverlayInputSetEntity.getProjectIdentifier());
    assertThat(updatedCdInputSetResponse.getIdentifier()).isEqualTo(updatedOverlayInputSetEntity.getIdentifier());
    assertThat(updatedCdInputSetResponse.getName()).isEqualTo(updatedOverlayInputSetEntity.getName());
    assertThat(updatedCdInputSetResponse.getDescription()).isEqualTo(updatedOverlayInputSetEntity.getDescription());

    // Update non existing entity
    updatedOverlayInputSetEntity.setAccountId("newAccountId");
    assertThatThrownBy(() -> inputSetEntityService.update(updatedOverlayInputSetEntity))
        .isInstanceOf(InvalidRequestException.class);
    updatedOverlayInputSetEntity.setAccountId(ACCOUNT_ID);

    // Delete
    boolean delete =
        inputSetEntityService.delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER);
    assertThat(delete).isTrue();

    Optional<BaseInputSetEntity> deletedInputSet =
        inputSetEntityService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(deletedInputSet.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testList() {
    final String ORG_IDENTIFIER = "orgId";
    final String PROJ_IDENTIFIER = "projId";
    final String PIPELINE_IDENTIFIER = "pipeline_identifier";
    final String CD_IDENTIFIER = "cdIdentifier";
    final String CD_IDENTIFIER_2 = "cdIdentifier2";
    final String OVERLAY_IDENTIFIER = "overlayIdentifier";
    final String ACCOUNT_ID = "account_id";

    CDInputSetEntity cdInputSetEntity = CDInputSetEntity.builder().build();
    cdInputSetEntity.setAccountId(ACCOUNT_ID);
    cdInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    cdInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    cdInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    cdInputSetEntity.setIdentifier(CD_IDENTIFIER);
    cdInputSetEntity.setName("Input Set");

    OverlayInputSetEntity overlayInputSetEntity = OverlayInputSetEntity.builder().build();
    overlayInputSetEntity.setAccountId(ACCOUNT_ID);
    overlayInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    overlayInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    overlayInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    overlayInputSetEntity.setIdentifier(OVERLAY_IDENTIFIER);
    overlayInputSetEntity.setName("Input Set");

    BaseInputSetEntity createdCDInputSet = inputSetEntityService.create(cdInputSetEntity);
    BaseInputSetEntity createdOverlayInputSet = inputSetEntityService.create(overlayInputSetEntity);

    Criteria criteriaFromFilter = InputSetFilterHelper.createCriteriaForGetList(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, InputSetListType.ALL, "", false);
    Pageable pageRequest = PageUtils.getPageRequest(0, 10, null);
    Page<BaseInputSetEntity> list = inputSetEntityService.list(criteriaFromFilter, pageRequest);

    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(2);
    assertThat(InputSetElementMapper.writeSummaryResponseDTO(list.getContent().get(0)))
        .isEqualTo(InputSetElementMapper.writeSummaryResponseDTO(createdCDInputSet));
    assertThat(InputSetElementMapper.writeSummaryResponseDTO(list.getContent().get(1)))
        .isEqualTo(InputSetElementMapper.writeSummaryResponseDTO(createdOverlayInputSet));

    // Add another entity.
    CDInputSetEntity cdInputSetEntity2 = CDInputSetEntity.builder().build();
    cdInputSetEntity2.setAccountId(ACCOUNT_ID);
    cdInputSetEntity2.setOrgIdentifier(ORG_IDENTIFIER);
    cdInputSetEntity2.setProjectIdentifier(PROJ_IDENTIFIER);
    cdInputSetEntity2.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    cdInputSetEntity2.setIdentifier(CD_IDENTIFIER_2);
    cdInputSetEntity2.setName("Input Set");

    BaseInputSetEntity createdCDInputSet2 = inputSetEntityService.create(cdInputSetEntity2);
    List<BaseInputSetEntity> givenInputSetList =
        inputSetEntityService.getGivenInputSetList(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER,
            Stream.of(CD_IDENTIFIER_2, OVERLAY_IDENTIFIER).collect(Collectors.toSet()));
    assertThat(givenInputSetList).containsExactlyInAnyOrder(createdCDInputSet2, overlayInputSetEntity);
  }
}