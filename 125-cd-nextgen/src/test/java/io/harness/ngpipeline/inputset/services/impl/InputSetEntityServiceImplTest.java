package io.harness.ngpipeline.inputset.services.impl;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.common.EntityReferenceHelper;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.resource.InputSetListType;
import io.harness.ngpipeline.inputset.mappers.InputSetElementMapper;
import io.harness.ngpipeline.inputset.mappers.InputSetFilterHelper;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.rule.Owner;
import io.harness.utils.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class InputSetEntityServiceImplTest extends CDNGBaseTest {
  @Mock EntitySetupUsageClient entitySetupUsageClient;
  @Inject InputSetEntityServiceImpl inputSetEntityService;

  @Before
  public void setUp() {
    Reflect.on(inputSetEntityService).set("entitySetupUsageClient", entitySetupUsageClient);
  }

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
  public void testServiceLayerOnInputSet() {
    String ORG_IDENTIFIER = "orgId";
    String PROJ_IDENTIFIER = "projId";
    String PIPELINE_IDENTIFIER = "pipeline_identifier";
    String IDENTIFIER = "identifier";
    String ACCOUNT_ID = "account_id";

    InputSetEntity inputSetEntity = InputSetEntity.builder().build();
    inputSetEntity.setAccountId(ACCOUNT_ID);
    inputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    inputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    inputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    inputSetEntity.setIdentifier(IDENTIFIER);
    inputSetEntity.setName("Input Set");

    // Create
    BaseInputSetEntity createdInputSet = inputSetEntityService.create(inputSetEntity);
    assertThat(createdInputSet).isNotNull();
    assertThat(createdInputSet.getAccountId()).isEqualTo(inputSetEntity.getAccountId());
    assertThat(createdInputSet.getOrgIdentifier()).isEqualTo(inputSetEntity.getOrgIdentifier());
    assertThat(createdInputSet.getProjectIdentifier()).isEqualTo(inputSetEntity.getProjectIdentifier());
    assertThat(createdInputSet.getIdentifier()).isEqualTo(inputSetEntity.getIdentifier());
    assertThat(createdInputSet.getName()).isEqualTo(inputSetEntity.getName());

    // Get
    Optional<BaseInputSetEntity> getInputSet =
        inputSetEntityService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER, false);
    assertThat(getInputSet).isPresent();
    assertThat(getInputSet.get()).isEqualTo(createdInputSet);

    // Update
    InputSetEntity updatedInputSetEntity = InputSetEntity.builder().build();
    updatedInputSetEntity.setAccountId(ACCOUNT_ID);
    updatedInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    updatedInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    updatedInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    updatedInputSetEntity.setIdentifier(IDENTIFIER);
    updatedInputSetEntity.setName("Input Set Updated");

    BaseInputSetEntity updatedInputSetResponse = inputSetEntityService.update(updatedInputSetEntity);
    assertThat(updatedInputSetResponse.getAccountId()).isEqualTo(updatedInputSetEntity.getAccountId());
    assertThat(updatedInputSetResponse.getOrgIdentifier()).isEqualTo(updatedInputSetEntity.getOrgIdentifier());
    assertThat(updatedInputSetResponse.getProjectIdentifier()).isEqualTo(updatedInputSetEntity.getProjectIdentifier());
    assertThat(updatedInputSetResponse.getIdentifier()).isEqualTo(updatedInputSetEntity.getIdentifier());
    assertThat(updatedInputSetResponse.getName()).isEqualTo(updatedInputSetEntity.getName());
    assertThat(updatedInputSetResponse.getDescription()).isEqualTo(updatedInputSetEntity.getDescription());

    // Update non existing entity
    updatedInputSetEntity.setAccountId("newAccountId");
    assertThatThrownBy(() -> inputSetEntityService.update(updatedInputSetEntity))
        .isInstanceOf(InvalidRequestException.class);
    updatedInputSetEntity.setAccountId(ACCOUNT_ID);

    // Delete
    Call<ResponseDTO<Page<EntitySetupUsageDTO>>> request = mock(Call.class);
    try {
      when(request.execute()).thenReturn(Response.success(ResponseDTO.newResponse(Page.empty())));
    } catch (IOException ex) {
      logger.info("Encountered exception ", ex);
    }
    doReturn(request)
        .when(entitySetupUsageClient)
        .listAllEntityUsage(0, 10, ACCOUNT_ID,
            EntityReferenceHelper.createFQN(
                Arrays.asList(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER)),
            "");
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

    BaseInputSetEntity updatedInputSetResponse = inputSetEntityService.update(updatedOverlayInputSetEntity);
    assertThat(updatedInputSetResponse.getAccountId()).isEqualTo(updatedOverlayInputSetEntity.getAccountId());
    assertThat(updatedInputSetResponse.getOrgIdentifier()).isEqualTo(updatedOverlayInputSetEntity.getOrgIdentifier());
    assertThat(updatedInputSetResponse.getProjectIdentifier())
        .isEqualTo(updatedOverlayInputSetEntity.getProjectIdentifier());
    assertThat(updatedInputSetResponse.getIdentifier()).isEqualTo(updatedOverlayInputSetEntity.getIdentifier());
    assertThat(updatedInputSetResponse.getName()).isEqualTo(updatedOverlayInputSetEntity.getName());
    assertThat(updatedInputSetResponse.getDescription()).isEqualTo(updatedOverlayInputSetEntity.getDescription());

    // Update non existing entity
    updatedOverlayInputSetEntity.setAccountId("newAccountId");
    assertThatThrownBy(() -> inputSetEntityService.update(updatedOverlayInputSetEntity))
        .isInstanceOf(InvalidRequestException.class);
    updatedOverlayInputSetEntity.setAccountId(ACCOUNT_ID);

    // Delete
    Call<ResponseDTO<Page<EntitySetupUsageDTO>>> request = mock(Call.class);
    try {
      when(request.execute()).thenReturn(Response.success(ResponseDTO.newResponse(Page.empty())));
    } catch (IOException ex) {
      logger.info("Encountered exception ", ex);
    }
    doReturn(request)
        .when(entitySetupUsageClient)
        .listAllEntityUsage(0, 10, ACCOUNT_ID,
            EntityReferenceHelper.createFQN(
                Arrays.asList(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, IDENTIFIER)),
            "");

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
    final String IDENTIFIER = "Identifier";
    final String IDENTIFIER_2 = "Identifier2";
    final String OVERLAY_IDENTIFIER = "overlayIdentifier";
    final String ACCOUNT_ID = "account_id";

    InputSetEntity inputSetEntity = InputSetEntity.builder().build();
    inputSetEntity.setAccountId(ACCOUNT_ID);
    inputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    inputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    inputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    inputSetEntity.setIdentifier(IDENTIFIER);
    inputSetEntity.setName("Input Set");

    OverlayInputSetEntity overlayInputSetEntity = OverlayInputSetEntity.builder().build();
    overlayInputSetEntity.setAccountId(ACCOUNT_ID);
    overlayInputSetEntity.setOrgIdentifier(ORG_IDENTIFIER);
    overlayInputSetEntity.setProjectIdentifier(PROJ_IDENTIFIER);
    overlayInputSetEntity.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    overlayInputSetEntity.setIdentifier(OVERLAY_IDENTIFIER);
    overlayInputSetEntity.setName("Input Set");

    BaseInputSetEntity createdInputSet = inputSetEntityService.create(inputSetEntity);
    BaseInputSetEntity createdOverlayInputSet = inputSetEntityService.create(overlayInputSetEntity);

    Criteria criteriaFromFilter = InputSetFilterHelper.createCriteriaForGetList(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, InputSetListType.ALL, "", false);
    Pageable pageRequest = PageUtils.getPageRequest(0, 10, null);
    Page<BaseInputSetEntity> list = inputSetEntityService.list(criteriaFromFilter, pageRequest);

    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(2);
    assertThat(InputSetElementMapper.writeSummaryResponseDTO(list.getContent().get(0)))
        .isEqualTo(InputSetElementMapper.writeSummaryResponseDTO(createdInputSet));
    assertThat(InputSetElementMapper.writeSummaryResponseDTO(list.getContent().get(1)))
        .isEqualTo(InputSetElementMapper.writeSummaryResponseDTO(createdOverlayInputSet));

    // Add another entity.
    InputSetEntity inputSetEntity2 = InputSetEntity.builder().build();
    inputSetEntity2.setAccountId(ACCOUNT_ID);
    inputSetEntity2.setOrgIdentifier(ORG_IDENTIFIER);
    inputSetEntity2.setProjectIdentifier(PROJ_IDENTIFIER);
    inputSetEntity2.setPipelineIdentifier(PIPELINE_IDENTIFIER);
    inputSetEntity2.setIdentifier(IDENTIFIER_2);
    inputSetEntity2.setName("Input Set");

    BaseInputSetEntity createdInputSet2 = inputSetEntityService.create(inputSetEntity2);
    List<BaseInputSetEntity> givenInputSetList = inputSetEntityService.getGivenInputSetList(ACCOUNT_ID, ORG_IDENTIFIER,
        PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, Stream.of(IDENTIFIER_2, OVERLAY_IDENTIFIER).collect(Collectors.toSet()));
    assertThat(givenInputSetList).containsExactlyInAnyOrder(createdInputSet2, overlayInputSetEntity);
  }
}