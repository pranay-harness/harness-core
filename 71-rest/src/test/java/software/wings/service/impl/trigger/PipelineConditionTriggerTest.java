package software.wings.service.impl.trigger;

import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Before;
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

  @Before
  public void setUp() {
    on(pipelineTriggerProcessor).set("pipelineService", pipelineService);
    on(deploymentTriggerServiceHelper).set("pipelineService", pipelineService);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(pipelineService.fetchPipelineName(APP_ID, PIPELINE_ID)).thenReturn(PIPELINE_NAME);
    when(pipelineService.fetchPipelineName(APP_ID, "PIPELINE_ID1")).thenReturn("PIPELINE_NAME1");
    when(pipelineService.fetchPipelineName(APP_ID, "InvalidPipelineId"))
        .thenThrow(new TriggerException("Pipeline does not exis", null));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldSavePipelineTriggerNoArtifactSelections() {
    Pipeline pipeline = buildPipeline();
    Pipeline conditionPipeline = buildPipeline();
    conditionPipeline.setUuid("PIPELINE_ID1");
    conditionPipeline.setName("PIPELINE_NAME1");
    setPipelineStages(pipeline);
    pipeline.getPipelineVariables().add(aVariable().name("MyVar").build());
    when(pipelineService.fetchPipelineName(APP_ID, PIPELINE_ID)).thenReturn(PIPELINE_NAME);
    when(pipelineService.fetchPipelineName(APP_ID, "PIPELINE_ID1")).thenReturn("PIPELINE_NAME1");

    DeploymentTrigger trigger = deploymentTriggerService.save(pipelineTrigger, false);

    DeploymentTrigger savedTrigger = deploymentTriggerService.get(APP_ID, trigger.getUuid(), false);
    assertThat(savedTrigger.getCondition()).isInstanceOf(PipelineCondition.class);
    PipelineCondition pipelineCondition = (PipelineCondition) savedTrigger.getCondition();

    assertThat(pipelineCondition.getPipelineName()).isEqualTo("PIPELINE_NAME1");
    assertThat(pipelineCondition.getPipelineId()).isEqualTo("PIPELINE_ID1");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowConditionPipelineNotExistException() {
    pipelineTrigger.setCondition(PipelineCondition.builder().pipelineId("InvalidPipelineId").build());
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> deploymentTriggerService.save(pipelineTrigger, false));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowConditionPipelineExceptionForNull() {
    pipelineTrigger.setCondition(PipelineCondition.builder().pipelineId(null).build());
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> deploymentTriggerService.save(pipelineTrigger, false));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowActionPipelineNotExistException() {
    pipelineTrigger.setAction(
        PipelineAction.builder().pipelineId("InvalidPipelineId").triggerArgs(TriggerArgs.builder().build()).build());
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> deploymentTriggerService.save(pipelineTrigger, false));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForSameActionAndCondition() {
    pipelineTrigger.setAction(
        PipelineAction.builder().pipelineId("PIPELINE_ID1").triggerArgs(TriggerArgs.builder().build()).build());
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> deploymentTriggerService.save(pipelineTrigger, false));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowActionPipelineExceptionForNull() {
    pipelineTrigger.setAction(
        PipelineAction.builder().pipelineId(null).triggerArgs(TriggerArgs.builder().build()).build());
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> deploymentTriggerService.save(pipelineTrigger, false));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowConditionPipelineInvalidException() {
    pipelineTrigger.setCondition(PipelineCondition.builder().pipelineId("InvalidPipelineId").build());
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> pipelineTriggerProcessor.transformTriggerConditionRead(pipelineTrigger));
  }
}
