package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.app.beans.dto.CIPipelineFilterDTO;
import io.harness.category.element.UnitTests;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.service.NGPipelineServiceImpl;
import io.harness.pms.yaml.ParameterField;
import io.harness.repositories.pipeline.spring.NgPipelineRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public class CIPipelineServiceImplTest extends CIManagerTestBase {
  @Mock private NgPipelineRepository ngPipelineRepository;
  @Mock EntitySetupUsageClient entitySetupUsageClient;
  @Inject NGPipelineServiceImpl ngPipelineService;
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String ORG_ID = "ORG_ID";
  private final String PROJECT_ID = "PROJECT_ID";
  private final String TAG = "foo";
  private String inputYaml;
  private NgPipelineEntity pipeline;

  @Before
  public void setUp() {
    Reflect.on(ngPipelineService).set("ngPipelineRepository", ngPipelineRepository);
    Reflect.on(ngPipelineService).set("entitySetupUsageClient", entitySetupUsageClient);
    inputYaml = new Scanner(
        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("pipeline.yml")), "UTF-8")
                    .useDelimiter("\\A")
                    .next();

    pipeline =
        NgPipelineEntity.builder()
            .identifier("testIdentifier")
            .ngPipeline(NgPipeline.builder().description(ParameterField.createValueField("testDescription")).build())
            .uuid("testUUID")
            .build();
  }

  private CIPipelineFilterDTO getPipelineFilter() {
    return CIPipelineFilterDTO.builder()
        .accountIdentifier(ACCOUNT_ID)
        .orgIdentifier(ORG_ID)
        .projectIdentifier(PROJECT_ID)
        .tags(Arrays.asList(TAG))
        .build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void readPipeline() {
    when(ngPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
             ACCOUNT_ID, ORG_ID, PROJECT_ID, "testId", true))
        .thenReturn(Optional.of(pipeline));

    NgPipelineEntity ngPipelineEntity = ngPipelineService.getPipeline("testId", ACCOUNT_ID, ORG_ID, PROJECT_ID);

    assertThat(ngPipelineEntity.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(ngPipelineEntity.getNgPipeline().getDescription().getValue()).isEqualTo("testDescription");
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getPipelines() {
    CIPipelineFilterDTO ciPipelineFilterDTO = getPipelineFilter();
    when(ngPipelineRepository.findAll(any(), any())).thenReturn(new PageImpl<>(Arrays.asList(pipeline)));

    List<NgPipelineEntity> pipelineEntities =
        ngPipelineService
            .listPipelines(ciPipelineFilterDTO.getAccountIdentifier(), ciPipelineFilterDTO.getOrgIdentifier(),
                ciPipelineFilterDTO.getProjectIdentifier(), new Criteria(), Pageable.unpaged(), null)
            .getContent();
    assertThat(pipelineEntities).isNotEmpty();

    NgPipelineEntity ngPipelineEntity = pipelineEntities.get(0);
    assertThat(ngPipelineEntity.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(ngPipelineEntity.getNgPipeline().getDescription().getValue()).isEqualTo("testDescription");
    assertThat(ngPipelineEntity.getUuid()).isEqualTo("testUUID");
  }
}
