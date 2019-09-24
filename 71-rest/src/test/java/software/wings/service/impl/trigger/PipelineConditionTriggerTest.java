package software.wings.service.impl.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildPipeline;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.setPipelineStages;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Pipeline;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.PipelineCondition;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

public class PipelineConditionTriggerTest extends WingsBaseTest {
  @Mock private PipelineService pipelineService;
  @Mock private AppService appService;

  @Inject @InjectMocks private DeploymentTriggerService deploymentTriggerService;
  @Inject @InjectMocks private PipelineTriggerProcessor pipelineTriggerProcessor;
  @Inject @InjectMocks private DeploymentTriggerServiceHelper deploymentTriggerServiceHelper;

  DeploymentTrigger pipelineTrigger =
      DeploymentTrigger.builder()
          .name("Pipeline Trigger")
          .appId(APP_ID)
          .action(PipelineAction.builder().pipelineId(PIPELINE_ID).triggerArgs(TriggerArgs.builder().build()).build())
          .condition(PipelineCondition.builder().pipelineId("PIPELINE_ID1").build())
          .build();

  @Test
  @Category(UnitTests.class)
  public void shouldSavePipelineTriggerNoArtifactSelections() {
    Pipeline pipeline = buildPipeline();
    Pipeline conditionPipeline = buildPipeline();
    conditionPipeline.setUuid("PIPELINE_ID1");
    conditionPipeline.setName("PIPELINE_NAME1");
    setPipelineStages(pipeline);
    pipeline.getPipelineVariables().add(aVariable().name("MyVar").build());
    on(pipelineTriggerProcessor).set("pipelineService", pipelineService);
    on(deploymentTriggerServiceHelper).set("pipelineService", pipelineService);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(pipelineService.readPipeline(APP_ID, "PIPELINE_ID1", true)).thenReturn(conditionPipeline);
    when(pipelineService.fetchPipelineName(APP_ID, PIPELINE_ID)).thenReturn(PIPELINE_NAME);
    when(pipelineService.fetchPipelineName(APP_ID, "PIPELINE_ID1")).thenReturn("PIPELINE_NAME1");

    DeploymentTrigger trigger = deploymentTriggerService.save(pipelineTrigger, false);

    DeploymentTrigger savedTrigger = deploymentTriggerService.get(APP_ID, trigger.getUuid());
    assertThat(savedTrigger.getCondition()).isInstanceOf(PipelineCondition.class);
    PipelineCondition pipelineCondition = (PipelineCondition) savedTrigger.getCondition();

    assertThat(pipelineCondition.getPipelineName()).isEqualTo("PIPELINE_NAME1");
    assertThat(pipelineCondition.getPipelineId()).isEqualTo("PIPELINE_ID1");
  }
}
