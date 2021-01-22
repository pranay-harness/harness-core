package io.harness.ng.core.environment.resources;

import static io.harness.rule.OwnerRule.ARCHIT;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentRequestDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.environment.mappers.EnvironmentFilterHelper;
import io.harness.ng.core.environment.services.impl.EnvironmentServiceImpl;
import io.harness.rule.Owner;

import software.wings.beans.Environment.EnvironmentKeys;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

public class EnvironmentResourceTest extends CategoryTest {
  @Mock EnvironmentServiceImpl environmentService;
  EnvironmentResource environmentResource;

  EnvironmentRequestDTO environmentRequestDTO;
  EnvironmentResponseDTO environmentResponseDTO;
  Environment environmentEntity;
  List<NGTag> tags;

  @Before
  public void setUp() {
    tags = Arrays.asList(NGTag.builder().key("k1").value("v1").build());
    MockitoAnnotations.initMocks(this);
    environmentResource = new EnvironmentResource(environmentService);
    environmentRequestDTO = EnvironmentRequestDTO.builder()
                                .identifier("IDENTIFIER")
                                .orgIdentifier("ORG_ID")
                                .projectIdentifier("PROJECT_ID")
                                .name("ENV")
                                .type(EnvironmentType.PreProduction)
                                .tags(singletonMap("k1", "v1"))
                                .version(0L)
                                .build();

    environmentResponseDTO = EnvironmentResponseDTO.builder()
                                 .accountId("ACCOUNT_ID")
                                 .identifier("IDENTIFIER")
                                 .orgIdentifier("ORG_ID")
                                 .projectIdentifier("PROJECT_ID")
                                 .name("ENV")
                                 .type(EnvironmentType.PreProduction)
                                 .tags(singletonMap("k1", "v1"))
                                 .version(0L)
                                 .build();

    environmentEntity = Environment.builder()
                            .accountId("ACCOUNT_ID")
                            .identifier("IDENTIFIER")
                            .orgIdentifier("ORG_ID")
                            .projectIdentifier("PROJECT_ID")
                            .name("ENV")
                            .type(EnvironmentType.PreProduction)
                            .tags(tags)
                            .version(0L)
                            .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGet() {
    doReturn(Optional.of(environmentEntity))
        .when(environmentService)
        .get("ACCOUNT_ID", environmentRequestDTO.getOrgIdentifier(), environmentRequestDTO.getProjectIdentifier(),
            environmentRequestDTO.getIdentifier(), false);

    EnvironmentResponseDTO envResponse =
        environmentResource.get("IDENTIFIER", "ACCOUNT_ID", "ORG_ID", "PROJECT_ID", false).getData();

    assertThat(envResponse).isNotNull();
    assertThat(envResponse).isEqualTo(environmentResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCreate() {
    doReturn(environmentEntity).when(environmentService).create(environmentEntity);
    EnvironmentResponseDTO envResponse =
        environmentResource.create(environmentEntity.getAccountId(), environmentRequestDTO).getData();
    assertThat(envResponse).isEqualTo(environmentResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testDelete() {
    doReturn(true)
        .when(environmentService)
        .delete("ACCOUNT_ID", environmentRequestDTO.getOrgIdentifier(), environmentRequestDTO.getProjectIdentifier(),
            environmentRequestDTO.getIdentifier(), null);

    Boolean data = environmentResource.delete(null, "IDENTIFIER", "ACCOUNT_ID", "ORG_ID", "PROJECT_ID").getData();
    assertThat(data).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testUpdate() {
    doReturn(environmentEntity).when(environmentService).update(environmentEntity);
    EnvironmentResponseDTO response =
        environmentResource.update("0", environmentEntity.getAccountId(), environmentRequestDTO).getData();
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(environmentResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testUpsert() {
    doReturn(environmentEntity).when(environmentService).upsert(environmentEntity);
    EnvironmentResponseDTO response =
        environmentResource.upsert("0", environmentEntity.getAccountId(), environmentRequestDTO).getData();
    assertThat(response).isNotNull();
    assertThat(response).isEqualTo(environmentResponseDTO);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testListEnvironmentsWithDESCSort() {
    Criteria criteria = EnvironmentFilterHelper.createCriteriaForGetList("", "", "", false);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, EnvironmentKeys.createdAt));
    final Page<Environment> environments = new PageImpl<>(Collections.singletonList(environmentEntity), pageable, 1);
    doReturn(environments).when(environmentService).list(criteria, pageable);

    List<EnvironmentResponseDTO> content =
        environmentResource.listEnvironmentsForProject(0, 10, "", "", "", null, null).getData().getContent();
    assertThat(content).isNotNull();
    assertThat(content.size()).isEqualTo(1);
    assertThat(content.get(0)).isEqualTo(environmentResponseDTO);
  }
}
