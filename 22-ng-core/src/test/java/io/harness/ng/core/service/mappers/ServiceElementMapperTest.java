package io.harness.ng.core.service.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.service.dto.ServiceRequestDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceElementMapperTest extends CategoryTest {
  ServiceRequestDTO serviceRequestDTO;
  ServiceResponseDTO serviceResponseDTO;
  ServiceEntity responseServiceEntity;
  ServiceEntity requestServiceEntity;

  @Before
  public void setUp() {
    serviceRequestDTO = ServiceRequestDTO.builder()
                            .identifier("IDENTIFIER")
                            .orgIdentifier("ORG_ID")
                            .projectIdentifier("PROJECT_ID")
                            .name("Service")
                            .build();

    serviceResponseDTO = ServiceResponseDTO.builder()
                             .accountId("ACCOUNT_ID")
                             .identifier("IDENTIFIER")
                             .orgIdentifier("ORG_ID")
                             .projectIdentifier("PROJECT_ID")
                             .name("Service")
                             .deleted(false)
                             .build();
    responseServiceEntity = ServiceEntity.builder()
                                .id("UUID")
                                .accountId("ACCOUNT_ID")
                                .identifier("IDENTIFIER")
                                .orgIdentifier("ORG_ID")
                                .projectIdentifier("PROJECT_ID")
                                .name("Service")
                                .deleted(false)
                                .build();

    requestServiceEntity = ServiceEntity.builder()
                               .accountId("ACCOUNT_ID")
                               .identifier("IDENTIFIER")
                               .orgIdentifier("ORG_ID")
                               .projectIdentifier("PROJECT_ID")
                               .name("Service")
                               .deleted(false)
                               .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testToServiceEntity() {
    ServiceEntity mappedService = ServiceElementMapper.toServiceEntity("ACCOUNT_ID", serviceRequestDTO);
    assertThat(mappedService).isNotNull();
    assertThat(mappedService).isEqualTo(requestServiceEntity);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testWriteDTO() {
    ServiceResponseDTO serviceResponseDTO = ServiceElementMapper.writeDTO(responseServiceEntity);
    assertThat(serviceResponseDTO).isNotNull();
    assertThat(serviceResponseDTO).isEqualTo(serviceResponseDTO);
  }
}