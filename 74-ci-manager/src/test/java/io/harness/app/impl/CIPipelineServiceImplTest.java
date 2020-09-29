package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.app.beans.dto.CIPipelineFilterDTO;
import io.harness.app.yaml.YAML;
import io.harness.beans.ParameterField;
import io.harness.beans.stages.IntegrationStage;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import io.harness.ngpipeline.repository.PipelineRepository;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StageElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

public class CIPipelineServiceImplTest extends CIManagerTest {
  @Mock private PipelineRepository ciPipelineRepository;
  @InjectMocks @Inject CIPipelineServiceImpl ciPipelineService;
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String ORG_ID = "ORG_ID";
  private final String PROJECT_ID = "PROJECT_ID";
  private final String TAG = "foo";
  private YAML yaml;
  private CDPipelineEntity pipeline;

  @Before
  public void setUp() {
    String yamlString = new Scanner(
        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("pipeline.yml")), "UTF-8")
                            .useDelimiter("\\A")
                            .next();

    yaml = YAML.builder().pipelineYAML(yamlString).build();
    pipeline =
        CDPipelineEntity.builder()
            .identifier("testIdentifier")
            .cdPipeline(CDPipeline.builder().description(ParameterField.createValueField("testDescription")).build())
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
    ArgumentCaptor<CDPipelineEntity> pipelineCaptor = ArgumentCaptor.forClass(CDPipelineEntity.class);
    when(ciPipelineRepository.save(any(CDPipelineEntity.class))).thenReturn(pipeline);
    when(ciPipelineRepository.findById("testId")).thenReturn(Optional.ofNullable(pipeline));

    ciPipelineService.createPipelineFromYAML(yaml, ACCOUNT_ID, ORG_ID, PROJECT_ID);

    verify(ciPipelineRepository).save(pipelineCaptor.capture());
    CDPipelineEntity ciPipelineEntity = pipelineCaptor.getValue();
    assertThat(ciPipelineEntity).isNotNull();
    assertThat(ciPipelineEntity.getIdentifier()).isEqualTo("cipipeline");

    assertThat(ciPipelineEntity.getCdPipeline().getStages()).hasSize(1);
    assertThat(ciPipelineEntity.getCdPipeline().getStages().get(0)).isInstanceOf(StageElement.class);
    StageElement stageElement = (StageElement) ciPipelineEntity.getCdPipeline().getStages().get(0);

    IntegrationStage integrationStage = (IntegrationStage) stageElement.getStageType();
    assertThat(integrationStage.getIdentifier()).isEqualTo("masterBuildUpload");
    assertThat(integrationStage.getGitConnector()).isNotNull();
    assertThat(integrationStage.getInfrastructure()).isNotNull();
    assertThat(integrationStage.getContainer()).isNotNull();
    assertThat(integrationStage.getCustomVariables()).isNotNull();

    ExecutionElement execution = integrationStage.getExecution();
    assertThat(execution).isNotNull();
    assertThat(execution.getSteps()).hasSize(5);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPipeline() {
    ArgumentCaptor<CDPipelineEntity> pipelineCaptor = ArgumentCaptor.forClass(CDPipelineEntity.class);
    when(ciPipelineRepository.save(any(CDPipelineEntity.class))).thenReturn(pipeline);
    when(ciPipelineRepository.findById("testId")).thenReturn(Optional.ofNullable(pipeline));

    ciPipelineService.createPipeline(pipeline);

    verify(ciPipelineRepository).save(pipelineCaptor.capture());
    CDPipelineEntity ciPipeline = pipelineCaptor.getValue();
    assertThat(ciPipeline.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(ciPipeline.getCdPipeline().getDescription().getValue()).isEqualTo("testDescription");
    assertThat(ciPipeline.getUuid()).isEqualTo("testUUID");
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void readPipeline() {
    when(ciPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
             ACCOUNT_ID, ORG_ID, PROJECT_ID, "testId", true))
        .thenReturn(Optional.ofNullable(pipeline));

    CDPipelineEntity ciPipeline = ciPipelineService.readPipeline("testId", ACCOUNT_ID, ORG_ID, PROJECT_ID);

    assertThat(ciPipeline.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(ciPipeline.getCdPipeline().getDescription().getValue()).isEqualTo("testDescription");
    assertThat(ciPipeline.getUuid()).isEqualTo("testUUID");
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getPipelines() {
    CIPipelineFilterDTO ciPipelineFilterDTO = getPipelineFilter();
    when(ciPipelineRepository.findAllWithCriteria(any())).thenReturn(Arrays.asList(pipeline));

    List<CDPipelineEntity> ciPipelineList = ciPipelineService.getPipelines(ciPipelineFilterDTO);
    assertThat(ciPipelineList).isNotEmpty();

    CDPipelineEntity ciPipeline = ciPipelineList.get(0);
    assertThat(ciPipeline.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(ciPipeline.getCdPipeline().getDescription().getValue()).isEqualTo("testDescription");
    assertThat(ciPipeline.getUuid()).isEqualTo("testUUID");
  }
}