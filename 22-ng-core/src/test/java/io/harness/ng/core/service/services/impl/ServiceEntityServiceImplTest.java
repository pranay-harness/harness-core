package io.harness.ng.core.service.services.impl;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGCoreBaseTest;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.mappers.ServiceElementMapper;
import io.harness.ng.core.service.mappers.ServiceFilterHelper;
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

public class ServiceEntityServiceImplTest extends NGCoreBaseTest {
  @Inject ServiceEntityServiceImpl serviceEntityService;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testValidatePresenceOfRequiredFields() {
    assertThatThrownBy(() -> serviceEntityService.validatePresenceOfRequiredFields("", null, "2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("One of the required fields is null.");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testServiceLayer() {
    ServiceEntity serviceEntity = ServiceEntity.builder()
                                      .accountId("ACCOUNT_ID")
                                      .identifier("IDENTIFIER")
                                      .orgIdentifier("ORG_ID")
                                      .projectIdentifier("PROJECT_ID")
                                      .name("Service")
                                      .build();

    // Create operations
    ServiceEntity createdService = serviceEntityService.create(serviceEntity);
    assertThat(createdService).isNotNull();
    assertThat(createdService.getAccountId()).isEqualTo(serviceEntity.getAccountId());
    assertThat(createdService.getOrgIdentifier()).isEqualTo(serviceEntity.getOrgIdentifier());
    assertThat(createdService.getProjectIdentifier()).isEqualTo(serviceEntity.getProjectIdentifier());
    assertThat(createdService.getIdentifier()).isEqualTo(serviceEntity.getIdentifier());
    assertThat(createdService.getName()).isEqualTo(serviceEntity.getName());

    // Get operations
    Optional<ServiceEntity> getService =
        serviceEntityService.get("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "IDENTIFIER", false);
    assertThat(getService).isPresent();
    assertThat(getService.get()).isEqualTo(createdService);

    // Update operations
    ServiceEntity updateServiceRequest = ServiceEntity.builder()
                                             .accountId("ACCOUNT_ID")
                                             .identifier("IDENTIFIER")
                                             .orgIdentifier("ORG_ID")
                                             .projectIdentifier("PROJECT_ID")
                                             .name("UPDATED_SERVICE")
                                             .description("NEW_DESCRIPTION")
                                             .build();
    ServiceEntity updatedServiceResponse = serviceEntityService.update(updateServiceRequest);
    assertThat(updatedServiceResponse.getAccountId()).isEqualTo(updateServiceRequest.getAccountId());
    assertThat(updatedServiceResponse.getOrgIdentifier()).isEqualTo(updateServiceRequest.getOrgIdentifier());
    assertThat(updatedServiceResponse.getProjectIdentifier()).isEqualTo(updateServiceRequest.getProjectIdentifier());
    assertThat(updatedServiceResponse.getIdentifier()).isEqualTo(updateServiceRequest.getIdentifier());
    assertThat(updatedServiceResponse.getName()).isEqualTo(updateServiceRequest.getName());
    assertThat(updatedServiceResponse.getDescription()).isEqualTo(updateServiceRequest.getDescription());

    updateServiceRequest.setAccountId("NEW_ACCOUNT");
    assertThatThrownBy(() -> serviceEntityService.update(updateServiceRequest))
        .isInstanceOf(InvalidRequestException.class);
    updatedServiceResponse.setAccountId("ACCOUNT_ID");

    // Upsert operations
    ServiceEntity upsertServiceRequest = ServiceEntity.builder()
                                             .accountId("ACCOUNT_ID")
                                             .identifier("NEW_IDENTIFIER")
                                             .orgIdentifier("ORG_ID")
                                             .projectIdentifier("NEW_PROJECT")
                                             .name("UPSERTED_SERVICE")
                                             .description("NEW_DESCRIPTION")
                                             .build();
    ServiceEntity upsertService = serviceEntityService.upsert(upsertServiceRequest);
    assertThat(upsertService.getAccountId()).isEqualTo(upsertServiceRequest.getAccountId());
    assertThat(upsertService.getOrgIdentifier()).isEqualTo(upsertServiceRequest.getOrgIdentifier());
    assertThat(upsertService.getProjectIdentifier()).isEqualTo(upsertServiceRequest.getProjectIdentifier());
    assertThat(upsertService.getIdentifier()).isEqualTo(upsertServiceRequest.getIdentifier());
    assertThat(upsertService.getName()).isEqualTo(upsertServiceRequest.getName());
    assertThat(upsertService.getDescription()).isEqualTo(upsertServiceRequest.getDescription());

    // List services operations.
    Criteria criteriaFromServiceFilter =
        ServiceFilterHelper.createCriteriaForGetList("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", false);
    Pageable pageRequest = PageUtils.getPageRequest(0, 10, null);
    Page<ServiceEntity> list = serviceEntityService.list(criteriaFromServiceFilter, pageRequest);
    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(1);
    assertThat(ServiceElementMapper.writeDTO(list.getContent().get(0)))
        .isEqualTo(ServiceElementMapper.writeDTO(updatedServiceResponse));

    criteriaFromServiceFilter = ServiceFilterHelper.createCriteriaForGetList("ACCOUNT_ID", "ORG_ID", null, false);

    list = serviceEntityService.list(criteriaFromServiceFilter, pageRequest);
    assertThat(list.getContent()).isNotNull();
    assertThat(list.getContent().size()).isEqualTo(2);
    List<ServiceResponseDTO> dtoList =
        list.getContent().stream().map(ServiceElementMapper::writeDTO).collect(Collectors.toList());
    assertThat(dtoList).containsOnly(
        ServiceElementMapper.writeDTO(updatedServiceResponse), ServiceElementMapper.writeDTO(upsertService));

    // Delete operations
    boolean delete = serviceEntityService.delete("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "UPDATED_SERVICE");
    assertThat(delete).isTrue();

    Optional<ServiceEntity> deletedService =
        serviceEntityService.get("ACCOUNT_ID", "ORG_ID", "PROJECT_ID", "UPDATED_SERVICE", false);
    assertThat(deletedService.isPresent()).isFalse();
  }
}