package io.harness.ng.core.service.resources;

import static io.harness.rule.OwnerRule.ARCHIT;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.Service.ServiceKeys;

import io.harness.NgManagerTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.service.dto.ServiceRequestDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.mappers.ServiceFilterHelper;
import io.harness.ng.core.service.services.impl.ServiceEntityServiceImpl;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ServiceResourceTest extends NgManagerTest {
  @Mock ServiceEntityServiceImpl serviceEntityService;
  @InjectMocks ServiceResource serviceResource;

  ServiceRequestDTO serviceRequestDTO;
  ServiceResponseDTO serviceResponseDTO;
  ServiceEntity serviceEntity;
  List<NGTag> tags;

  @Before
  public void setUp() {
    tags = Arrays.asList(NGTag.builder().key("k1").value("v1").build());
    serviceRequestDTO = ServiceRequestDTO.builder()
                            .identifier("IDENTIFIER")
                            .orgIdentifier("ORG_ID")
                            .projectIdentifier("PROJECT_ID")
                            .name("Service")
                            .tags(singletonMap("k1", "v1"))
                            .version(0L)
                            .build();

    serviceResponseDTO = ServiceResponseDTO.builder()
                             .accountId("ACCOUNT_ID")
                             .identifier("IDENTIFIER")
                             .orgIdentifier("ORG_ID")
                             .projectIdentifier("PROJECT_ID")
                             .name("Service")
                             .tags(singletonMap("k1", "v1"))
                             .version(0L)
                             .build();
    serviceEntity = ServiceEntity.builder()
                        .accountId("ACCOUNT_ID")
                        .identifier("IDENTIFIER")
                        .orgIdentifier("ORG_ID")
                        .projectIdentifier("PROJECT_ID")
                        .name("Service")
                        .tags(tags)
                        .version(0L)
                        .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGet() {
    doReturn(Optional.of(serviceEntity))
        .when(serviceEntityService)
        .get("ACCOUNT_ID", serviceRequestDTO.getOrgIdentifier(), serviceRequestDTO.getProjectIdentifier(),
            serviceRequestDTO.getIdentifier(), false);

    ServiceResponseDTO serviceResponse =
        serviceResource.get("IDENTIFIER", "ACCOUNT_ID", "ORG_ID", "PROJECT_ID", false).getData();

    assertThat(serviceResponse).isNotNull();
    assertThat(serviceResponse).isEqualTo(serviceResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCreate() {
    doReturn(serviceEntity).when(serviceEntityService).create(serviceEntity);
    ServiceResponseDTO serviceResponse =
        serviceResource.create(serviceEntity.getAccountId(), serviceRequestDTO).getData();
    assertThat(serviceResponse).isEqualTo(serviceResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testDelete() {
    doReturn(true)
        .when(serviceEntityService)
        .delete("ACCOUNT_ID", serviceRequestDTO.getOrgIdentifier(), serviceRequestDTO.getProjectIdentifier(),
            serviceRequestDTO.getIdentifier(), null);

    Boolean data = serviceResource.delete(null, "IDENTIFIER", "ACCOUNT_ID", "ORG_ID", "PROJECT_ID").getData();
    assertThat(data).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testUpdate() {
    doReturn(serviceEntity).when(serviceEntityService).update(serviceEntity);
    ServiceResponseDTO response =
        serviceResource.update("0", serviceEntity.getAccountId(), serviceRequestDTO).getData();
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(serviceResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testUpsert() {
    doReturn(serviceEntity).when(serviceEntityService).upsert(serviceEntity);
    ServiceResponseDTO response =
        serviceResource.upsert("0", serviceEntity.getAccountId(), serviceRequestDTO).getData();
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(serviceResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testListServicesWithDESCSort() {
    Criteria criteria = ServiceFilterHelper.createCriteriaForGetList("", "", "", false);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, ServiceKeys.createdAt));
    final Page<ServiceEntity> serviceList = new PageImpl<>(Collections.singletonList(serviceEntity), pageable, 1);
    doReturn(serviceList).when(serviceEntityService).list(criteria, pageable);

    List<ServiceResponseDTO> content =
        serviceResource.listServicesForProject(0, 10, "", "", "", null).getData().getContent();
    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(serviceResponseDTO);
  }
}
