package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.app.beans.dto.CIPipelineFilterDTO;
import io.harness.beans.ParameterField;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.TestIntelligenceStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.mappers.PipelineDtoMapper;
import io.harness.ngpipeline.pipeline.repository.spring.NgPipelineRepository;
import io.harness.ngpipeline.pipeline.service.NGPipelineServiceImpl;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public class CIPipelineServiceImplTest extends CIManagerTest {
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
  public void createPipelineFromYAML() {
    ArgumentCaptor<NgPipelineEntity> pipelineCaptor = ArgumentCaptor.forClass(NgPipelineEntity.class);
    when(ngPipelineRepository.save(any(NgPipelineEntity.class))).thenReturn(pipeline);
    when(ngPipelineRepository.findById("testId")).thenReturn(Optional.ofNullable(pipeline));

    ngPipelineService.create(PipelineDtoMapper.toPipelineEntity(ACCOUNT_ID, ORG_ID, PROJECT_ID, inputYaml));

    verify(ngPipelineRepository).save(pipelineCaptor.capture());
    NgPipelineEntity ngPipelineEntity = pipelineCaptor.getValue();
    assertThat(ngPipelineEntity).isNotNull();
    assertThat(ngPipelineEntity.getIdentifier()).isEqualTo("cipipeline");

    assertThat(ngPipelineEntity.getNgPipeline().getStages()).hasSize(1);
    assertThat(ngPipelineEntity.getNgPipeline().getStages().get(0)).isInstanceOf(StageElement.class);
    StageElement stageElement = (StageElement) ngPipelineEntity.getNgPipeline().getStages().get(0);

    IntegrationStage integrationStage = (IntegrationStage) stageElement.getStageType();
    assertThat(integrationStage.getIdentifier()).isEqualTo("masterBuildUpload");
    assertThat(integrationStage.getGitConnector()).isNotNull();
    assertThat(integrationStage.getInfrastructure()).isNotNull();
    assertThat(integrationStage.getContainer()).isNotNull();
    assertThat(integrationStage.getCustomVariables()).isNotNull();

    ExecutionElement execution = integrationStage.getExecution();
    assertThat(execution).isNotNull();

    // Assert testIntelligence spec
    StepElement stepElement = (StepElement) execution.getSteps().get(4);
    assertThat(stepElement.getType()).isEqualTo("testIntelligence");
    assertThat(stepElement.getIdentifier()).isEqualTo("runUnitTestsIntelligently");
    TestIntelligenceStepInfo testIntelligenceStepInfo = (TestIntelligenceStepInfo) stepElement.getStepSpecType();
    assertThat(testIntelligenceStepInfo.getGoals()).isEqualTo("echo \"Running test\"");
    assertThat(testIntelligenceStepInfo.getBuildTool()).isEqualTo("maven");
    assertThat(testIntelligenceStepInfo.getLanguage()).isEqualTo("java");

    assertThat(execution.getSteps()).hasSize(6);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPipeline() {
    ArgumentCaptor<NgPipelineEntity> pipelineCaptor = ArgumentCaptor.forClass(NgPipelineEntity.class);
    when(ngPipelineRepository.save(any(NgPipelineEntity.class))).thenReturn(pipeline);
    when(ngPipelineRepository.findById("testId")).thenReturn(Optional.ofNullable(pipeline));

    ngPipelineService.create(PipelineDtoMapper.toPipelineEntity(ACCOUNT_ID, ORG_ID, PROJECT_ID, inputYaml));

    verify(ngPipelineRepository).save(pipelineCaptor.capture());
    NgPipelineEntity pipelineEntity = pipelineCaptor.getValue();
    assertThat(pipelineEntity.getIdentifier()).isEqualTo("cipipeline");
    assertThat(pipelineEntity.getNgPipeline().getDescription().getValue()).isEqualTo("testDescription");
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
