package io.harness.event.usagemetrics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.event.timeseries.processor.EventProcessor;
import lombok.experimental.FieldNameConstants;
import org.assertj.core.api.Assertions;
import software.wings.api.DeploymentTimeSeriesEvent;
import software.wings.beans.EnvSummary;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.PipelineExecution.Builder;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

import java.util.Arrays;
import java.util.Map;

@FieldNameConstants(innerTypeName = "UsageMetricsTestKeys")
public class UsageMetricsTestUtils {
  private String ACCOUNTID;
  private String APPID;
  private String WORKFLOWID;
  private String CLOUDPROVIDER1, CLOUDPROVIDER2;
  private String PIPELINEID;
  private String PIPELINEEXECUTIONID;
  private String SERVICE1;
  private String ENV1;
  private String ENV1NAME;
  private String TRIGGER1;
  private String USER1;
  private String EXECUTIONID;

  public static void validateTimeSeriesEventInfo(DeploymentTimeSeriesEvent timeSeriesEvent, int number) {
    final TimeSeriesEventInfo timeSeriesEventInfo = timeSeriesEvent.getTimeSeriesEventInfo();
    Assertions.assertThat(timeSeriesEventInfo).isNotNull();
    Assertions.assertThat(timeSeriesEventInfo.getAccountId()).isEqualTo(UsageMetricsTestKeys.ACCOUNTID);
    Assertions.assertThat(timeSeriesEventInfo.getStringData().get(EventProcessor.EXECUTIONID))
        .isEqualTo(UsageMetricsTestKeys.EXECUTIONID + number);
    Assertions.assertThat(timeSeriesEventInfo.getLongData().get(EventProcessor.STARTTIME)).isEqualTo(100L);
    Assertions.assertThat(timeSeriesEventInfo.getLongData().get(EventProcessor.ENDTIME)).isEqualTo(200L);
    Assertions.assertThat(timeSeriesEventInfo.getLongData().get(EventProcessor.DURATION)).isEqualTo(100L);
    Assertions.assertThat(timeSeriesEventInfo.getLongData().get(EventProcessor.ROLLBACK_DURATION)).isEqualTo(100L);
    Assertions.assertThat(timeSeriesEventInfo.getStringData().get(EventProcessor.APPID))
        .isEqualTo(UsageMetricsTestKeys.APPID);
    Assertions.assertThat(timeSeriesEventInfo.getStringData().get(EventProcessor.STATUS))
        .isEqualTo(ExecutionStatus.SUCCESS.name());
    Assertions.assertThat(timeSeriesEventInfo.getStringData().get(EventProcessor.PARENT_EXECUTION))
        .contains(UsageMetricsTestKeys.PIPELINEEXECUTIONID);
    Assertions.assertThat(timeSeriesEventInfo.getStringData().get(EventProcessor.PIPELINE))
        .contains(UsageMetricsTestKeys.PIPELINEID);
    Assertions.assertThat(timeSeriesEventInfo.getListData().get(EventProcessor.CLOUD_PROVIDER_LIST))
        .contains(UsageMetricsTestKeys.CLOUDPROVIDER1);
    Assertions.assertThat(timeSeriesEventInfo.getStringData().get(EventProcessor.TRIGGER_ID))
        .contains(UsageMetricsTestKeys.TRIGGER1);
    Assertions.assertThat(timeSeriesEventInfo.getStringData().get(EventProcessor.TRIGGERED_BY))
        .contains(UsageMetricsTestKeys.USER1);
    Assertions.assertThat(timeSeriesEventInfo.getListData().get(EventProcessor.ENV_LIST))
        .contains(UsageMetricsTestKeys.ENV1);
    Assertions.assertThat(timeSeriesEventInfo.getListData().get(EventProcessor.ENVTYPES))
        .contains(EnvironmentType.PROD.name());
    Assertions.assertThat(timeSeriesEventInfo.getListData().get(EventProcessor.SERVICE_LIST))
        .contains(UsageMetricsTestKeys.SERVICE1);
  }

  public static WorkflowExecution generateWorkflowExecution(int number) {
    final Map<String, String> artifactBuildNumber = Maps.newHashMap();
    artifactBuildNumber.put(ArtifactKeys.metadata_buildNo, "123");
    return WorkflowExecution.builder()
        .uuid(UsageMetricsTestKeys.EXECUTIONID + number)
        .accountId(UsageMetricsTestKeys.ACCOUNTID)
        .appId(UsageMetricsTestKeys.APPID)
        .status(ExecutionStatus.SUCCESS)
        .workflowId(UsageMetricsTestKeys.WORKFLOWID)
        .startTs(100L)
        .endTs(200L)
        .cloudProviderIds(Lists.newArrayList(UsageMetricsTestKeys.CLOUDPROVIDER1, UsageMetricsTestKeys.CLOUDPROVIDER2))
        .deployedCloudProviders(
            Lists.newArrayList(UsageMetricsTestKeys.CLOUDPROVIDER1, UsageMetricsTestKeys.CLOUDPROVIDER2))
        .environments(Lists.newArrayList(EnvSummary.builder().environmentType(EnvironmentType.PROD).build()))
        .deployedEnvironments(Lists.newArrayList(
            EnvSummary.builder().environmentType(EnvironmentType.PROD).uuid(UsageMetricsTestKeys.ENV1).build()))
        .pipelineExecution(
            Builder.aPipelineExecution()
                .withPipelineId(UsageMetricsTestKeys.PIPELINEID)
                .withPipelineStageExecutions(
                    Arrays.asList(PipelineStageExecution.builder()
                                      .workflowExecutions(Arrays.asList(
                                          WorkflowExecution.builder()
                                              .envId(UsageMetricsTestKeys.ENV1)
                                              .envType(EnvironmentType.PROD)
                                              .cloudProviderIds(Lists.newArrayList(UsageMetricsTestKeys.CLOUDPROVIDER1))
                                              .serviceIds(Arrays.asList(UsageMetricsTestKeys.SERVICE1))

                                              .build()))
                                      .build()))
                .build())
        .pipelineExecutionId(UsageMetricsTestKeys.PIPELINEEXECUTIONID)
        .artifacts(Lists.newArrayList(Artifact.Builder.anArtifact().withMetadata(artifactBuildNumber).build()))
        .serviceIds(Arrays.asList(UsageMetricsTestKeys.SERVICE1))
        .deployedServices(Arrays.asList(UsageMetricsTestKeys.SERVICE1))
        .triggeredBy(EmbeddedUser.builder().uuid(UsageMetricsTestKeys.USER1).build())
        .deploymentTriggerId(UsageMetricsTestKeys.TRIGGER1)
        .duration(100L)
        .rollbackDuration(100L)
        .build();
  }
}
