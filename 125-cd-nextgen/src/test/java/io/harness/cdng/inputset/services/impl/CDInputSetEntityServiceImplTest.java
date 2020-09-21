package io.harness.cdng.inputset.services.impl;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.mappers.CDInputSetElementMapper;
import io.harness.cdng.inputset.mappers.CDInputSetFilterHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public class CDInputSetEntityServiceImplTest extends CDNGBaseTest {
  @Inject CDInputSetEntityServiceImpl cdInputSetEntityService;

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidatePresenceOfRequiredFields() {
    assertThatThrownBy(() -> cdInputSetEntityService.validatePresenceOfRequiredFields("", null, "2"))
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

    CDInputSetEntity cdInputSetEntity = CDInputSetEntity.builder()
                                            .accountId(ACCOUNT_ID)
                                            .identifier(IDENTIFIER)
                                            .orgIdentifier(ORG_IDENTIFIER)
                                            .projectIdentifier(PROJ_IDENTIFIER)
                                            .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                            .name("Input Set")
                                            .build();

    // Create
    CDInputSetEntity createdInputSet = cdInputSetEntityService.create(cdInputSetEntity);
    assertThat(createdInputSet).isNotNull();
    assertThat(createdInputSet.getAccountId()).isEqualTo(cdInputSetEntity.getAccountId());
    assertThat(createdInputSet.getOrgIdentifier()).isEqualTo(cdInputSetEntity.getOrgIdentifier());
    assertThat(createdInputSet.getProjectIdentifier()).isEqualTo(cdInputSetEntity.getProjectIdentifier());
    assertThat(createdInputSet.getIdentifier()).isEqualTo(cdInputSetEntity.getIdentifier());
    assertThat(createdInputSet.getName()).isEqualTo(cdInputSetEntity.getName());

    // Get
    Optional<CDInputSetEntity> getInputSet = cdInputSetEntityService.get(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(getInputSet).isPresent();
    assertThat(getInputSet.get()).isEqualTo(createdInputSet);

    // Update
    CDInputSetEntity updatedCdInputSetEntity = CDInputSetEntity.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .identifier(IDENTIFIER)
                                                   .orgIdentifier(ORG_IDENTIFIER)
                                                   .projectIdentifier(PROJ_IDENTIFIER)
                                                   .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                                   .name("Input Set Updated")
                                                   .build();

    CDInputSetEntity updatedCdInputSetResponse = cdInputSetEntityService.update(updatedCdInputSetEntity);
    assertThat(updatedCdInputSetResponse.getAccountId()).isEqualTo(updatedCdInputSetEntity.getAccountId());
    assertThat(updatedCdInputSetResponse.getOrgIdentifier()).isEqualTo(updatedCdInputSetEntity.getOrgIdentifier());
    assertThat(updatedCdInputSetResponse.getProjectIdentifier())
        .isEqualTo(updatedCdInputSetEntity.getProjectIdentifier());
    assertThat(updatedCdInputSetResponse.getIdentifier()).isEqualTo(updatedCdInputSetEntity.getIdentifier());
    assertThat(updatedCdInputSetResponse.getName()).isEqualTo(updatedCdInputSetEntity.getName());
    assertThat(updatedCdInputSetResponse.getDescription()).isEqualTo(updatedCdInputSetEntity.getDescription());

    // Update non existing entity
    updatedCdInputSetEntity.setAccountId("newAccountId");
    assertThatThrownBy(() -> cdInputSetEntityService.update(updatedCdInputSetEntity))
        .isInstanceOf(InvalidRequestException.class);
    updatedCdInputSetEntity.setAccountId(ACCOUNT_ID);

    // Upsert
    CDInputSetEntity upsertedCdInputSetEntity = CDInputSetEntity.builder()
                                                    .accountId(ACCOUNT_ID)
                                                    .identifier("NEW_IDENTIFIER")
                                                    .orgIdentifier(ORG_IDENTIFIER)
                                                    .projectIdentifier(PROJ_IDENTIFIER)
                                                    .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                                    .name("Input Set Upserted")
                                                    .build();

    CDInputSetEntity upsertedCdInputSetResponse = cdInputSetEntityService.upsert(upsertedCdInputSetEntity);
    assertThat(upsertedCdInputSetResponse.getAccountId()).isEqualTo(upsertedCdInputSetEntity.getAccountId());
    assertThat(upsertedCdInputSetResponse.getOrgIdentifier()).isEqualTo(upsertedCdInputSetEntity.getOrgIdentifier());
    assertThat(upsertedCdInputSetResponse.getProjectIdentifier())
        .isEqualTo(upsertedCdInputSetEntity.getProjectIdentifier());
    assertThat(upsertedCdInputSetResponse.getIdentifier()).isEqualTo(upsertedCdInputSetEntity.getIdentifier());
    assertThat(upsertedCdInputSetResponse.getName()).isEqualTo(upsertedCdInputSetEntity.getName());
    assertThat(upsertedCdInputSetResponse.getDescription()).isEqualTo(upsertedCdInputSetEntity.getDescription());

    // List
    Criteria criteriaFromFilter = CDInputSetFilterHelper.createCriteriaForGetList(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false);
    Pageable pageRequest = PageUtils.getPageRequest(0, 10, null);
    Page<CDInputSetEntity> list = cdInputSetEntityService.list(criteriaFromFilter, pageRequest);

    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(2);
    assertThat(CDInputSetElementMapper.writeSummaryResponseDTO(list.getContent().get(0)))
        .isEqualTo(CDInputSetElementMapper.writeSummaryResponseDTO(updatedCdInputSetResponse));

    // Delete
    boolean delete =
        cdInputSetEntityService.delete(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER);
    assertThat(delete).isTrue();

    Optional<CDInputSetEntity> deletedInputSet = cdInputSetEntityService.get(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(deletedInputSet.isPresent()).isFalse();
  }
}