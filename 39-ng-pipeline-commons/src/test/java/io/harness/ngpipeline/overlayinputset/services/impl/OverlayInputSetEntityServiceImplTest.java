package io.harness.ngpipeline.overlayinputset.services.impl;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.NGPipelineBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.mappers.OverlayInputSetElementMapper;
import io.harness.ngpipeline.overlayinputset.mappers.OverlayInputSetFilterHelper;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public class OverlayInputSetEntityServiceImplTest extends NGPipelineBaseTest {
  @Inject OverlayInputSetEntityServiceImpl overlayInputSetEntityService;

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidatePresenceOfRequiredFields() {
    assertThatThrownBy(() -> overlayInputSetEntityService.validatePresenceOfRequiredFields("", null, "2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("One of the required fields is null.");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testServiceLayer() {
    String ORG_IDENTIFIER = "orgId";
    String PROJ_IDENTIFIER = "projId";
    String PIPELINE_IDENTIFIER = "pipeline_identifier";
    String IDENTIFIER = "identifier";
    String ACCOUNT_ID = "account_id";

    OverlayInputSetEntity overlayInputSetEntity = OverlayInputSetEntity.builder()
                                                      .accountId(ACCOUNT_ID)
                                                      .identifier(IDENTIFIER)
                                                      .orgIdentifier(ORG_IDENTIFIER)
                                                      .projectIdentifier(PROJ_IDENTIFIER)
                                                      .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                                      .name("Input Set")
                                                      .build();

    // Create
    OverlayInputSetEntity createdInputSet = overlayInputSetEntityService.create(overlayInputSetEntity);
    assertThat(createdInputSet).isNotNull();
    assertThat(createdInputSet.getAccountId()).isEqualTo(overlayInputSetEntity.getAccountId());
    assertThat(createdInputSet.getOrgIdentifier()).isEqualTo(overlayInputSetEntity.getOrgIdentifier());
    assertThat(createdInputSet.getProjectIdentifier()).isEqualTo(overlayInputSetEntity.getProjectIdentifier());
    assertThat(createdInputSet.getIdentifier()).isEqualTo(overlayInputSetEntity.getIdentifier());
    assertThat(createdInputSet.getName()).isEqualTo(overlayInputSetEntity.getName());

    // Get
    Optional<OverlayInputSetEntity> getInputSet = overlayInputSetEntityService.get(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(getInputSet).isPresent();
    assertThat(getInputSet.get()).isEqualTo(createdInputSet);

    // Update
    OverlayInputSetEntity updatedOverlayInputSetEntity = OverlayInputSetEntity.builder()
                                                             .accountId(ACCOUNT_ID)
                                                             .identifier(IDENTIFIER)
                                                             .orgIdentifier(ORG_IDENTIFIER)
                                                             .projectIdentifier(PROJ_IDENTIFIER)
                                                             .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                                             .name("Input Set Updated")
                                                             .build();

    OverlayInputSetEntity updatedOverlayInputSetResponse =
        overlayInputSetEntityService.update(updatedOverlayInputSetEntity);
    assertThat(updatedOverlayInputSetResponse.getAccountId()).isEqualTo(updatedOverlayInputSetEntity.getAccountId());
    assertThat(updatedOverlayInputSetResponse.getOrgIdentifier())
        .isEqualTo(updatedOverlayInputSetEntity.getOrgIdentifier());
    assertThat(updatedOverlayInputSetResponse.getProjectIdentifier())
        .isEqualTo(updatedOverlayInputSetEntity.getProjectIdentifier());
    assertThat(updatedOverlayInputSetResponse.getIdentifier()).isEqualTo(updatedOverlayInputSetEntity.getIdentifier());
    assertThat(updatedOverlayInputSetResponse.getName()).isEqualTo(updatedOverlayInputSetEntity.getName());
    assertThat(updatedOverlayInputSetResponse.getDescription())
        .isEqualTo(updatedOverlayInputSetEntity.getDescription());

    // Update non existing entity
    updatedOverlayInputSetEntity.setAccountId("newAccountId");
    assertThatThrownBy(() -> overlayInputSetEntityService.update(updatedOverlayInputSetEntity))
        .isInstanceOf(InvalidRequestException.class);
    updatedOverlayInputSetEntity.setAccountId(ACCOUNT_ID);

    // Upsert
    OverlayInputSetEntity upsertedOverlayInputSetEntity = OverlayInputSetEntity.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .identifier("NEW_IDENTIFIER")
                                                              .orgIdentifier(ORG_IDENTIFIER)
                                                              .projectIdentifier(PROJ_IDENTIFIER)
                                                              .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                                              .name("Input Set Upserted")
                                                              .build();

    OverlayInputSetEntity upsertedOverlayInputSetResponse =
        overlayInputSetEntityService.upsert(upsertedOverlayInputSetEntity);
    assertThat(upsertedOverlayInputSetResponse.getAccountId()).isEqualTo(upsertedOverlayInputSetEntity.getAccountId());
    assertThat(upsertedOverlayInputSetResponse.getOrgIdentifier())
        .isEqualTo(upsertedOverlayInputSetEntity.getOrgIdentifier());
    assertThat(upsertedOverlayInputSetResponse.getProjectIdentifier())
        .isEqualTo(upsertedOverlayInputSetEntity.getProjectIdentifier());
    assertThat(upsertedOverlayInputSetResponse.getIdentifier())
        .isEqualTo(upsertedOverlayInputSetEntity.getIdentifier());
    assertThat(upsertedOverlayInputSetResponse.getName()).isEqualTo(upsertedOverlayInputSetEntity.getName());
    assertThat(upsertedOverlayInputSetResponse.getDescription())
        .isEqualTo(upsertedOverlayInputSetEntity.getDescription());

    // List
    Criteria criteriaFromFilter = OverlayInputSetFilterHelper.createCriteriaForGetList(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false);
    Pageable pageRequest = PageUtils.getPageRequest(0, 10, null);
    Page<OverlayInputSetEntity> list = overlayInputSetEntityService.list(criteriaFromFilter, pageRequest);

    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(2);
    assertThat(OverlayInputSetElementMapper.writeResponseDTO(list.getContent().get(0)))
        .isEqualTo(OverlayInputSetElementMapper.writeResponseDTO(updatedOverlayInputSetResponse));

    // Delete
    boolean delete = overlayInputSetEntityService.delete(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER);
    assertThat(delete).isTrue();

    Optional<OverlayInputSetEntity> deletedInputSet = overlayInputSetEntityService.get(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(deletedInputSet.isPresent()).isFalse();
  }
}