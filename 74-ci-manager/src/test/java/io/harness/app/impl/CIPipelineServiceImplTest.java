package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.app.dao.repositories.CIPipelineRepository;
import io.harness.app.yaml.YAML;
import io.harness.beans.CIPipeline;
import io.harness.beans.stages.IntegrationStage;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StageElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

public class CIPipelineServiceImplTest extends CIManagerTest {
  @Mock private CIPipelineRepository ciPipelineRepository;
  @InjectMocks @Inject CIPipelineServiceImpl ciPipelineService;

  private YAML yaml;
  private CIPipeline pipeline;

  @Before
  public void setUp() {
    String yamlString = new Scanner(
        Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("pipeline.yml")), "UTF-8")
                            .useDelimiter("\\A")
                            .next();

    yaml = YAML.builder().pipelineYAML(yamlString).build();
    pipeline =
        CIPipeline.builder().identifier("testIdentifier").description("testDescription").uuid("testUUID").build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPipelineFromYAML() {
    ArgumentCaptor<CIPipeline> pipelineCaptor = ArgumentCaptor.forClass(CIPipeline.class);
    when(ciPipelineRepository.save(any(CIPipeline.class))).thenReturn(pipeline);
    when(ciPipelineRepository.findById("testId")).thenReturn(Optional.ofNullable(pipeline));

    ciPipelineService.createPipelineFromYAML(yaml);

    verify(ciPipelineRepository).save(pipelineCaptor.capture());
    CIPipeline ciPipeline = pipelineCaptor.getValue();
    assertThat(ciPipeline).isNotNull();
    assertThat(ciPipeline.getIdentifier()).isEqualTo("cipipeline");

    assertThat(ciPipeline.getStages()).hasSize(1);
    assertThat(ciPipeline.getStages().get(0)).isInstanceOf(StageElement.class);
    StageElement stageElement = (StageElement) ciPipeline.getStages().get(0);

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
    ArgumentCaptor<CIPipeline> pipelineCaptor = ArgumentCaptor.forClass(CIPipeline.class);
    when(ciPipelineRepository.save(any(CIPipeline.class))).thenReturn(pipeline);
    when(ciPipelineRepository.findById("testId")).thenReturn(Optional.ofNullable(pipeline));

    ciPipelineService.createPipeline(pipeline);

    verify(ciPipelineRepository).save(pipelineCaptor.capture());
    CIPipeline ciPipeline = pipelineCaptor.getValue();
    assertThat(ciPipeline.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(ciPipeline.getDescription()).isEqualTo("testDescription");
    assertThat(ciPipeline.getUuid()).isEqualTo("testUUID");
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void readPipeline() {
    ArgumentCaptor<CIPipeline> pipelineCaptor = ArgumentCaptor.forClass(CIPipeline.class);
    when(ciPipelineRepository.save(any(CIPipeline.class))).thenReturn(pipeline);
    when(ciPipelineRepository.findById("testId")).thenReturn(Optional.ofNullable(pipeline));

    CIPipeline ciPipeline = ciPipelineService.readPipeline("testId");

    assertThat(ciPipeline.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(ciPipeline.getDescription()).isEqualTo("testDescription");
    assertThat(ciPipeline.getUuid()).isEqualTo("testUUID");
  }
}