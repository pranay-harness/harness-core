package io.harness.app.impl;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.app.yaml.YAML;
import io.harness.beans.CIPipeline;
import io.harness.beans.stages.IntegrationStage;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.core.Execution;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.dl.WingsPersistence;

import java.util.Scanner;

public class CIPipelineServiceImplTest extends CIManagerTest {
  @Mock private WingsPersistence wingsPersistence;
  @InjectMocks @Inject CIPipelineServiceImpl ciPipelineService;

  private YAML yaml;
  private CIPipeline pipeline;

  @Before
  public void setUp() {
    String yamlString = new Scanner(CIPipelineServiceImplTest.class.getResourceAsStream("pipeline.yml"), "UTF-8")
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
    when(wingsPersistence.save(any(CIPipeline.class))).thenReturn("testId");
    when(wingsPersistence.get(CIPipeline.class, "testId")).thenReturn(pipeline);

    ciPipelineService.createPipelineFromYAML(yaml);

    verify(wingsPersistence).save(pipelineCaptor.capture());
    CIPipeline ciPipeline = pipelineCaptor.getValue();
    assertThat(ciPipeline).isNotNull();
    assertThat(ciPipeline.getIdentifier()).isEqualTo("cipipeline");

    assertThat(ciPipeline.getStages()).hasSize(1);
    assertThat(ciPipeline.getStages().get(0)).isInstanceOf(IntegrationStage.class);
    IntegrationStage integrationStage = (IntegrationStage) ciPipeline.getStages().get(0);
    assertThat(integrationStage.getIdentifier()).isEqualTo("master-build-upload");
    assertThat(integrationStage.getArtifact()).isNotNull();
    assertThat(integrationStage.getConnector()).isNotNull();
    assertThat(integrationStage.getInfrastructure()).isNotNull();
    assertThat(integrationStage.getContainer()).isNotNull();

    Execution execution = integrationStage.getExecution();
    assertThat(execution).isNotNull();
    assertThat(execution.getSteps()).hasSize(5);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createPipeline() {
    ArgumentCaptor<CIPipeline> pipelineCaptor = ArgumentCaptor.forClass(CIPipeline.class);
    when(wingsPersistence.save(any(CIPipeline.class))).thenReturn("testId");
    when(wingsPersistence.get(CIPipeline.class, "testId")).thenReturn(pipeline);

    ciPipelineService.createPipeline(pipeline);

    verify(wingsPersistence).save(pipelineCaptor.capture());
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
    when(wingsPersistence.save(any(CIPipeline.class))).thenReturn("testId");
    when(wingsPersistence.get(CIPipeline.class, "testId")).thenReturn(pipeline);

    CIPipeline ciPipeline = ciPipelineService.readPipeline("testId");

    assertThat(ciPipeline.getIdentifier()).isEqualTo("testIdentifier");
    assertThat(ciPipeline.getDescription()).isEqualTo("testDescription");
    assertThat(ciPipeline.getUuid()).isEqualTo("testUUID");
  }
}