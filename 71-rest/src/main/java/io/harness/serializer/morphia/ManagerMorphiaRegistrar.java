package io.harness.serializer.morphia;

import io.harness.mongo.MorphiaRegistrar;
import io.harness.security.SimpleEncryption;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.ContainerServiceElement;
import software.wings.api.EnvStateExecutionData;
import software.wings.api.ForkElement;
import software.wings.api.HttpStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PcfDeploymentInfo;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.ScriptStateExecutionSummary;
import software.wings.api.SelectNodeStepExecutionSummary;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.api.ServiceArtifactElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.api.jira.JiraExecutionData;
import software.wings.api.pcf.PcfDeployExecutionSummary;
import software.wings.api.pcf.PcfDeployStateExecutionData;
import software.wings.api.pcf.PcfRouteSwapExecutionSummary;
import software.wings.api.pcf.PcfRouteUpdateStateExecutionData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.api.pcf.PcfSetupExecutionSummary;
import software.wings.api.pcf.PcfSetupStateExecutionData;
import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.BlueGreenOrchestrationWorkflow;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CanaryWorkflowExecutionAdvisor;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JiraConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SlackConfig;
import software.wings.beans.SmbConfig;
import software.wings.beans.SplunkConfig;
import software.wings.beans.StringValue;
import software.wings.beans.SumoConfig;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.GitSyncErrorAlert;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.beans.alert.ResourceUsageApproachingLimitAlert;
import software.wings.beans.alert.SSOSyncFailedAlert;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.beans.alert.cv.ContinuousVerificationDataCollectionAlert;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.command.AwsLambdaCommandUnit;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.CopyConfigCommandUnit;
import software.wings.beans.command.DownloadArtifactCommandUnit;
import software.wings.beans.command.EcsSetupCommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitSshCommandUnitV2;
import software.wings.beans.command.KubernetesResizeCommandUnit;
import software.wings.beans.command.KubernetesSetupCommandUnit;
import software.wings.beans.command.KubernetesSetupParams;
import software.wings.beans.command.PortCheckClearedCommandUnit;
import software.wings.beans.command.PortCheckListeningCommandUnit;
import software.wings.beans.command.ProcessCheckRunningCommandUnit;
import software.wings.beans.command.ProcessCheckStoppedCommandUnit;
import software.wings.beans.command.ResizeCommandUnit;
import software.wings.beans.command.ResizeCommandUnitExecutionData;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.SetupEnvCommandUnit;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommitAndPushResult;
import software.wings.beans.yaml.GitCommitRequest;
import software.wings.beans.yaml.GitDiffRequest;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.collect.ArtifactCollectionCallback;
import software.wings.delegatetasks.buildsource.BuildSourceCallback;
import software.wings.delegatetasks.buildsource.BuildSourceCleanupCallback;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfDeployCommandResponse;
import software.wings.helpers.ext.pcf.response.PcfSetupCommandResponse;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.WorkflowFilter;
import software.wings.service.impl.DelayEventNotifyData;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.email.EmailNotificationCallBack;
import software.wings.service.impl.event.AccountEntityEvent;
import software.wings.service.impl.event.AlertEvent;
import software.wings.service.impl.event.timeseries.TimeSeriesBatchEventInfo;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;
import software.wings.service.impl.trigger.TriggerServiceImpl.TriggerIdempotentResult;
import software.wings.service.impl.yaml.GitCommandCallback;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionWaitRetryCallback;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateMachineResumeCallback;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.APMVerificationState;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.ArtifactCheckState;
import software.wings.sm.states.ArtifactCollectionState;
import software.wings.sm.states.AwsNodeSelectState;
import software.wings.sm.states.BarrierState;
import software.wings.sm.states.CommandState;
import software.wings.sm.states.DcNodeSelectState;
import software.wings.sm.states.EcsBGUpdateListnerRollbackState;
import software.wings.sm.states.EcsBGUpdateListnerState;
import software.wings.sm.states.EcsBlueGreenServiceSetup;
import software.wings.sm.states.EcsServiceDeploy;
import software.wings.sm.states.EcsServiceRollback;
import software.wings.sm.states.ElkAnalysisState;
import software.wings.sm.states.EnvState;
import software.wings.sm.states.EnvState.EnvExecutionResponseData;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.ForkState.ForkStateExecutionData;
import software.wings.sm.states.HelmDeployState;
import software.wings.sm.states.HelmRollbackState;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;
import software.wings.sm.states.JenkinsState;
import software.wings.sm.states.KubernetesDeploy;
import software.wings.sm.states.KubernetesDeployRollback;
import software.wings.sm.states.KubernetesSetup;
import software.wings.sm.states.KubernetesSetupRollback;
import software.wings.sm.states.KubernetesSwapServiceSelectors;
import software.wings.sm.states.NewRelicState;
import software.wings.sm.states.PhaseStepSubWorkflow;
import software.wings.sm.states.PhaseSubWorkflow;
import software.wings.sm.states.PrometheusState;
import software.wings.sm.states.RepeatState;
import software.wings.sm.states.RepeatState.RepeatStateExecutionData;
import software.wings.sm.states.ShellScriptState;
import software.wings.sm.states.SumoLogicAnalysisState;
import software.wings.sm.states.collaboration.JiraCreateUpdate;
import software.wings.sm.states.k8s.K8sCanaryDeploy;
import software.wings.sm.states.k8s.K8sDelete;
import software.wings.sm.states.k8s.K8sRollingDeploy;
import software.wings.sm.states.k8s.K8sRollingDeployRollback;
import software.wings.sm.states.pcf.MapRouteState;
import software.wings.sm.states.pcf.PcfDeployState;
import software.wings.sm.states.pcf.PcfRollbackState;
import software.wings.sm.states.pcf.PcfSetupState;
import software.wings.sm.states.pcf.PcfSwitchBlueGreenRoutes;
import software.wings.sm.states.pcf.UnmapRouteState;
import software.wings.sm.states.provision.ApplyTerraformProvisionState;
import software.wings.sm.states.provision.DestroyTerraformProvisionState;
import software.wings.sm.states.provision.ShellScriptProvisionState;
import software.wings.sm.states.provision.TerraformRollbackState;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.log.BugsnagCVConfiguration;

import java.util.Map;

public class ManagerMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Map<String, Class> map) {
    map.put(pkgWings + "api.ApprovalStateExecutionData", ApprovalStateExecutionData.class);
    map.put(pkgWings + "api.ArtifactCollectionExecutionData", ArtifactCollectionExecutionData.class);
    map.put(pkgWings + "api.CanaryWorkflowStandardParams", CanaryWorkflowStandardParams.class);
    map.put(pkgWings + "api.CommandStateExecutionData", CommandStateExecutionData.class);
    map.put(pkgWings + "api.CommandStepExecutionSummary", CommandStepExecutionSummary.class);
    map.put(pkgWings + "api.ContainerDeploymentInfoWithNames", ContainerDeploymentInfoWithNames.class);
    map.put(pkgWings + "api.ContainerServiceElement", ContainerServiceElement.class);
    map.put(pkgWings + "api.EnvStateExecutionData", EnvStateExecutionData.class);
    map.put(pkgWings + "api.ForkElement", ForkElement.class);
    map.put(pkgWings + "api.HttpStateExecutionData", HttpStateExecutionData.class);
    map.put(pkgWings + "api.InstanceElement", InstanceElement.class);
    map.put(pkgWings + "api.InstanceElementListParam", InstanceElementListParam.class);
    map.put(pkgWings + "api.jira.JiraExecutionData", JiraExecutionData.class);
    map.put(pkgWings + "api.pcf.PcfDeployExecutionSummary", PcfDeployExecutionSummary.class);
    map.put(pkgWings + "api.pcf.PcfDeployStateExecutionData", PcfDeployStateExecutionData.class);
    map.put(pkgWings + "api.pcf.PcfRouteSwapExecutionSummary", PcfRouteSwapExecutionSummary.class);
    map.put(pkgWings + "api.pcf.PcfRouteUpdateStateExecutionData", PcfRouteUpdateStateExecutionData.class);
    map.put(pkgWings + "api.pcf.PcfSetupContextElement", PcfSetupContextElement.class);
    map.put(pkgWings + "api.pcf.PcfSetupExecutionSummary", PcfSetupExecutionSummary.class);
    map.put(pkgWings + "api.pcf.PcfSetupStateExecutionData", PcfSetupStateExecutionData.class);
    map.put(pkgWings + "api.PcfDeploymentInfo", PcfDeploymentInfo.class);
    map.put(pkgWings + "api.PhaseElement", PhaseElement.class);
    map.put(pkgWings + "api.PhaseExecutionData", PhaseExecutionData.class);
    map.put(pkgWings + "api.PhaseStepExecutionData", PhaseStepExecutionData.class);
    map.put(pkgWings + "api.ScriptStateExecutionData", ScriptStateExecutionData.class);
    map.put(pkgWings + "api.ScriptStateExecutionSummary", ScriptStateExecutionSummary.class);
    map.put(pkgWings + "api.SelectedNodeExecutionData", SelectedNodeExecutionData.class);
    map.put(pkgWings + "api.SelectNodeStepExecutionSummary", SelectNodeStepExecutionSummary.class);
    map.put(pkgWings + "api.ServiceArtifactElement", ServiceArtifactElement.class);
    map.put(pkgWings + "api.ServiceElement", ServiceElement.class);
    map.put(pkgWings + "api.ServiceInstanceIdsParam", ServiceInstanceIdsParam.class);
    map.put(pkgWings + "api.shellscript.provision.ShellScriptProvisionExecutionData",
        ShellScriptProvisionExecutionData.class);
    map.put(pkgWings + "beans.alert.ArtifactCollectionFailedAlert", ArtifactCollectionFailedAlert.class);
    map.put(pkgWings + "beans.alert.cv.ContinuousVerificationAlertData", ContinuousVerificationAlertData.class);
    map.put(pkgWings + "beans.alert.cv.ContinuousVerificationDataCollectionAlert",
        ContinuousVerificationDataCollectionAlert.class);
    map.put(pkgWings + "beans.alert.DelegatesDownAlert", DelegatesDownAlert.class);
    map.put(pkgWings + "beans.alert.GitSyncErrorAlert", GitSyncErrorAlert.class);
    map.put(pkgWings + "beans.alert.KmsSetupAlert", KmsSetupAlert.class);
    map.put(pkgWings + "beans.alert.NoActiveDelegatesAlert", NoActiveDelegatesAlert.class);
    map.put(pkgWings + "beans.alert.NoEligibleDelegatesAlert", NoEligibleDelegatesAlert.class);
    map.put(pkgWings + "beans.alert.ResourceUsageApproachingLimitAlert", ResourceUsageApproachingLimitAlert.class);
    map.put(pkgWings + "beans.alert.SSOSyncFailedAlert", SSOSyncFailedAlert.class);
    map.put(pkgWings + "beans.APMVerificationConfig", APMVerificationConfig.class);
    map.put(pkgWings + "beans.AppDynamicsConfig", AppDynamicsConfig.class);
    map.put(pkgWings + "beans.artifact.ArtifactFile", ArtifactFile.class);
    map.put(pkgWings + "beans.AwsConfig", AwsConfig.class);
    map.put(pkgWings + "beans.AzureConfig", AzureConfig.class);
    map.put(pkgWings + "beans.BambooConfig", BambooConfig.class);
    map.put(pkgWings + "beans.BasicOrchestrationWorkflow", BasicOrchestrationWorkflow.class);
    map.put(pkgWings + "beans.BlueGreenOrchestrationWorkflow", BlueGreenOrchestrationWorkflow.class);
    map.put(pkgWings + "beans.BugsnagConfig", BugsnagConfig.class);
    map.put(pkgWings + "beans.BuildWorkflow", BuildWorkflow.class);
    map.put(pkgWings + "beans.CanaryOrchestrationWorkflow", CanaryOrchestrationWorkflow.class);
    map.put(pkgWings + "beans.CanaryWorkflowExecutionAdvisor", CanaryWorkflowExecutionAdvisor.class);
    map.put(pkgWings + "beans.command.AwsLambdaCommandUnit", AwsLambdaCommandUnit.class);
    map.put(pkgWings + "beans.command.CleanupSshCommandUnit", CleanupSshCommandUnit.class);
    map.put(pkgWings + "beans.command.Command", Command.class);
    map.put(pkgWings + "beans.command.ContainerSetupCommandUnitExecutionData",
        ContainerSetupCommandUnitExecutionData.class);
    map.put(pkgWings + "beans.command.CopyConfigCommandUnit", CopyConfigCommandUnit.class);
    map.put(pkgWings + "beans.command.DownloadArtifactCommandUnit", DownloadArtifactCommandUnit.class);
    map.put(pkgWings + "beans.command.EcsSetupCommandUnit", EcsSetupCommandUnit.class);
    map.put(pkgWings + "beans.command.ExecCommandUnit", ExecCommandUnit.class);
    map.put(pkgWings + "beans.command.InitSshCommandUnitV2", InitSshCommandUnitV2.class);
    map.put(pkgWings + "beans.command.KubernetesResizeCommandUnit", KubernetesResizeCommandUnit.class);
    map.put(pkgWings + "beans.command.KubernetesSetupCommandUnit", KubernetesSetupCommandUnit.class);
    map.put(pkgWings + "beans.command.KubernetesSetupParams", KubernetesSetupParams.class);
    map.put(pkgWings + "beans.command.PortCheckClearedCommandUnit", PortCheckClearedCommandUnit.class);
    map.put(pkgWings + "beans.command.PortCheckListeningCommandUnit", PortCheckListeningCommandUnit.class);
    map.put(pkgWings + "beans.command.ProcessCheckRunningCommandUnit", ProcessCheckRunningCommandUnit.class);
    map.put(pkgWings + "beans.command.ProcessCheckStoppedCommandUnit", ProcessCheckStoppedCommandUnit.class);
    map.put(pkgWings + "beans.command.ResizeCommandUnit", ResizeCommandUnit.class);
    map.put(pkgWings + "beans.command.ResizeCommandUnitExecutionData", ResizeCommandUnitExecutionData.class);
    map.put(pkgWings + "beans.command.ScpCommandUnit", ScpCommandUnit.class);
    map.put(pkgWings + "beans.command.SetupEnvCommandUnit", SetupEnvCommandUnit.class);
    map.put(pkgWings + "beans.command.ShellExecutionData", ShellExecutionData.class);
    map.put(pkgWings + "beans.config.ArtifactoryConfig", ArtifactoryConfig.class);
    map.put(pkgWings + "beans.config.LogzConfig", LogzConfig.class);
    map.put(pkgWings + "beans.config.NexusConfig", NexusConfig.class);
    map.put(pkgWings + "beans.DatadogConfig", DatadogConfig.class);
    map.put(pkgWings + "beans.DockerConfig", DockerConfig.class);
    map.put(pkgWings + "beans.DynaTraceConfig", DynaTraceConfig.class);
    map.put(pkgWings + "beans.EcrConfig", EcrConfig.class);
    map.put(pkgWings + "beans.ElkConfig", ElkConfig.class);
    map.put(pkgWings + "beans.GcpConfig", GcpConfig.class);
    map.put(pkgWings + "beans.GitConfig", GitConfig.class);
    map.put(pkgWings + "beans.HostConnectionAttributes", HostConnectionAttributes.class);
    map.put(pkgWings + "beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo",
        AutoScalingGroupInstanceInfo.class);
    map.put(pkgWings + "beans.infrastructure.instance.info.Ec2InstanceInfo", Ec2InstanceInfo.class);
    map.put(pkgWings + "beans.infrastructure.instance.info.EcsContainerInfo", EcsContainerInfo.class);
    map.put(pkgWings + "beans.infrastructure.instance.info.K8sPodInfo", K8sPodInfo.class);
    map.put(pkgWings + "beans.infrastructure.instance.info.KubernetesContainerInfo", KubernetesContainerInfo.class);
    map.put(pkgWings + "beans.infrastructure.instance.info.PcfInstanceInfo", PcfInstanceInfo.class);
    map.put(pkgWings + "beans.infrastructure.instance.info.PhysicalHostInstanceInfo", PhysicalHostInstanceInfo.class);
    map.put(pkgWings + "beans.JenkinsConfig", JenkinsConfig.class);
    map.put(pkgWings + "beans.JiraConfig", JiraConfig.class);
    map.put(pkgWings + "beans.KubernetesClusterConfig", KubernetesClusterConfig.class);
    map.put(pkgWings + "beans.MultiServiceOrchestrationWorkflow", MultiServiceOrchestrationWorkflow.class);
    map.put(pkgWings + "beans.NewRelicConfig", NewRelicConfig.class);
    map.put(pkgWings + "beans.PcfConfig", PcfConfig.class);
    map.put(pkgWings + "beans.PhysicalDataCenterConfig", PhysicalDataCenterConfig.class);
    map.put(pkgWings + "beans.PrometheusConfig", PrometheusConfig.class);
    map.put(pkgWings + "beans.ServiceNowConfig", ServiceNowConfig.class);
    map.put(pkgWings + "beans.settings.helm.AmazonS3HelmRepoConfig", AmazonS3HelmRepoConfig.class);
    map.put(pkgWings + "beans.settings.helm.GCSHelmRepoConfig", GCSHelmRepoConfig.class);
    map.put(pkgWings + "beans.settings.helm.HttpHelmRepoConfig", HttpHelmRepoConfig.class);
    map.put(pkgWings + "beans.SlackConfig", SlackConfig.class);
    map.put(pkgWings + "beans.SmbConfig", SmbConfig.class);
    map.put(pkgWings + "beans.SplunkConfig", SplunkConfig.class);
    map.put(pkgWings + "beans.SSHExecutionCredential", SSHExecutionCredential.class);
    map.put(pkgWings + "beans.StringValue", StringValue.class);
    map.put(pkgWings + "beans.SumoConfig", SumoConfig.class);
    map.put(pkgWings + "beans.template.command.HttpTemplate", HttpTemplate.class);
    map.put(pkgWings + "beans.template.command.ShellScriptTemplate", ShellScriptTemplate.class);
    map.put(pkgWings + "beans.template.command.SshCommandTemplate", SshCommandTemplate.class);
    map.put(pkgWings + "beans.trigger.ArtifactCondition", ArtifactCondition.class);
    map.put(pkgWings + "beans.trigger.ScheduledTriggerCondition", ScheduledTriggerCondition.class);
    map.put(pkgWings + "beans.trigger.WebHookTriggerCondition", WebHookTriggerCondition.class);
    map.put(pkgWings + "beans.trigger.WorkflowAction", WorkflowAction.class);
    map.put(pkgWings + "beans.WinRmConnectionAttributes", WinRmConnectionAttributes.class);
    map.put(pkgWings + "beans.yaml.GitCommandExecutionResponse", GitCommandExecutionResponse.class);
    map.put(pkgWings + "beans.yaml.GitCommitAndPushResult", GitCommitAndPushResult.class);
    map.put(pkgWings + "beans.yaml.GitCommitRequest", GitCommitRequest.class);
    map.put(pkgWings + "beans.yaml.GitDiffRequest", GitDiffRequest.class);
    map.put(pkgWings + "beans.yaml.GitDiffResult", GitDiffResult.class);
    map.put(pkgWings + "collect.ArtifactCollectionCallback", ArtifactCollectionCallback.class);
    map.put(pkgWings + "delegatetasks.buildsource.BuildSourceCallback", BuildSourceCallback.class);
    map.put(pkgWings + "delegatetasks.buildsource.BuildSourceCleanupCallback", BuildSourceCleanupCallback.class);
    map.put(pkgWings + "delegatetasks.buildsource.BuildSourceExecutionResponse", BuildSourceExecutionResponse.class);
    map.put(pkgWings + "helpers.ext.external.comm.CollaborationProviderResponse", CollaborationProviderResponse.class);
    map.put(pkgWings + "helpers.ext.mail.SmtpConfig", SmtpConfig.class);
    map.put(pkgWings + "helpers.ext.pcf.request.PcfCommandRouteUpdateRequest", PcfCommandRouteUpdateRequest.class);
    map.put(pkgWings + "helpers.ext.pcf.request.PcfCommandSetupRequest", PcfCommandSetupRequest.class);
    map.put(pkgWings + "helpers.ext.pcf.response.PcfCommandExecutionResponse", PcfCommandExecutionResponse.class);
    map.put(pkgWings + "helpers.ext.pcf.response.PcfDeployCommandResponse", PcfDeployCommandResponse.class);
    map.put(pkgWings + "helpers.ext.pcf.response.PcfSetupCommandResponse", PcfSetupCommandResponse.class);
    map.put(pkgWings + "security.encryption.SimpleEncryption", SimpleEncryption.class);
    map.put(pkgWings + "security.EnvFilter", EnvFilter.class);
    map.put(pkgWings + "security.GenericEntityFilter", GenericEntityFilter.class);
    map.put(pkgWings + "security.WorkflowFilter", WorkflowFilter.class);
    map.put(pkgWings + "service.impl.analysis.DataCollectionCallback", DataCollectionCallback.class);
    map.put(pkgWings + "service.impl.analysis.DataCollectionTaskResult", DataCollectionTaskResult.class);
    map.put(pkgWings + "service.impl.cloudwatch.CloudWatchMetric", CloudWatchMetric.class);
    map.put(pkgWings + "service.impl.DelayEventNotifyData", DelayEventNotifyData.class);
    map.put(pkgWings + "service.impl.email.EmailNotificationCallBack", EmailNotificationCallBack.class);
    map.put(pkgWings + "service.impl.event.AccountEntityEvent", AccountEntityEvent.class);
    map.put(pkgWings + "service.impl.event.AlertEvent", AlertEvent.class);
    map.put(pkgWings + "service.impl.event.timeseries.TimeSeriesBatchEventInfo", TimeSeriesBatchEventInfo.class);
    map.put(pkgWings + "service.impl.event.timeseries.TimeSeriesEventInfo", TimeSeriesEventInfo.class);
    map.put(
        pkgWings + "service.impl.trigger.TriggerServiceImpl$TriggerIdempotentResult", TriggerIdempotentResult.class);
    map.put(pkgWings + "service.impl.WorkflowExecutionUpdate", WorkflowExecutionUpdate.class);
    map.put(pkgWings + "service.impl.yaml.GitCommandCallback", GitCommandCallback.class);
    map.put(pkgWings + "sm.ElementNotifyResponseData", ElementNotifyResponseData.class);
    map.put(pkgWings + "sm.ExecutionWaitRetryCallback", ExecutionWaitRetryCallback.class);
    map.put(pkgWings + "sm.StateExecutionData", StateExecutionData.class);
    map.put(pkgWings + "sm.StateMachineResumeCallback", StateMachineResumeCallback.class);
    map.put(pkgWings + "sm.states.APMVerificationState", APMVerificationState.class);
    map.put(pkgWings + "sm.states.ApprovalState", ApprovalState.class);
    map.put(pkgWings + "sm.states.ArtifactCheckState", ArtifactCheckState.class);
    map.put(pkgWings + "sm.states.ArtifactCollectionState", ArtifactCollectionState.class);
    map.put(pkgWings + "sm.states.AwsNodeSelectState", AwsNodeSelectState.class);
    map.put(pkgWings + "sm.states.BarrierState", BarrierState.class);
    map.put(pkgWings + "sm.states.collaboration.JiraCreateUpdate", JiraCreateUpdate.class);
    map.put(pkgWings + "sm.states.CommandState", CommandState.class);
    map.put(pkgWings + "sm.states.DcNodeSelectState", DcNodeSelectState.class);
    map.put(pkgWings + "sm.states.EcsBGUpdateListnerRollbackState", EcsBGUpdateListnerRollbackState.class);
    map.put(pkgWings + "sm.states.EcsBGUpdateListnerState", EcsBGUpdateListnerState.class);
    map.put(pkgWings + "sm.states.EcsBlueGreenServiceSetup", EcsBlueGreenServiceSetup.class);
    map.put(pkgWings + "sm.states.EcsServiceDeploy", EcsServiceDeploy.class);
    map.put(pkgWings + "sm.states.EcsServiceRollback", EcsServiceRollback.class);
    map.put(pkgWings + "sm.states.ElkAnalysisState", ElkAnalysisState.class);
    map.put(pkgWings + "sm.states.EnvState", EnvState.class);
    map.put(pkgWings + "sm.states.EnvState$EnvExecutionResponseData", EnvExecutionResponseData.class);
    map.put(pkgWings + "sm.states.ForkState", ForkState.class);
    map.put(pkgWings + "sm.states.ForkState$ForkStateExecutionData", ForkStateExecutionData.class);
    map.put(pkgWings + "sm.states.HelmDeployState", HelmDeployState.class);
    map.put(pkgWings + "sm.states.HelmRollbackState", HelmRollbackState.class);
    map.put(pkgWings + "sm.states.HttpState", HttpState.class);
    map.put(pkgWings + "sm.states.HttpState$HttpStateExecutionResponse", HttpStateExecutionResponse.class);
    map.put(pkgWings + "sm.states.JenkinsState", JenkinsState.class);
    map.put(pkgWings + "sm.states.k8s.K8sCanaryDeploy", K8sCanaryDeploy.class);
    map.put(pkgWings + "sm.states.k8s.K8sDelete", K8sDelete.class);
    map.put(pkgWings + "sm.states.k8s.K8sRollingDeploy", K8sRollingDeploy.class);
    map.put(pkgWings + "sm.states.k8s.K8sRollingDeployRollback", K8sRollingDeployRollback.class);
    map.put(pkgWings + "sm.states.KubernetesDeploy", KubernetesDeploy.class);
    map.put(pkgWings + "sm.states.KubernetesDeployRollback", KubernetesDeployRollback.class);
    map.put(pkgWings + "sm.states.KubernetesSetup", KubernetesSetup.class);
    map.put(pkgWings + "sm.states.KubernetesSetupRollback", KubernetesSetupRollback.class);
    map.put(pkgWings + "sm.states.KubernetesSwapServiceSelectors", KubernetesSwapServiceSelectors.class);
    map.put(pkgWings + "sm.states.NewRelicState", NewRelicState.class);
    map.put(pkgWings + "sm.states.pcf.MapRouteState", MapRouteState.class);
    map.put(pkgWings + "sm.states.pcf.PcfDeployState", PcfDeployState.class);
    map.put(pkgWings + "sm.states.pcf.PcfRollbackState", PcfRollbackState.class);
    map.put(pkgWings + "sm.states.pcf.PcfSetupState", PcfSetupState.class);
    map.put(pkgWings + "sm.states.pcf.PcfSwitchBlueGreenRoutes", PcfSwitchBlueGreenRoutes.class);
    map.put(pkgWings + "sm.states.pcf.UnmapRouteState", UnmapRouteState.class);
    map.put(pkgWings + "sm.states.PhaseStepSubWorkflow", PhaseStepSubWorkflow.class);
    map.put(pkgWings + "sm.states.PhaseSubWorkflow", PhaseSubWorkflow.class);
    map.put(pkgWings + "sm.states.PrometheusState", PrometheusState.class);
    map.put(pkgWings + "sm.states.provision.ApplyTerraformProvisionState", ApplyTerraformProvisionState.class);
    map.put(pkgWings + "sm.states.provision.DestroyTerraformProvisionState", DestroyTerraformProvisionState.class);
    map.put(pkgWings + "sm.states.provision.ShellScriptProvisionState", ShellScriptProvisionState.class);
    map.put(pkgWings + "sm.states.provision.TerraformRollbackState", TerraformRollbackState.class);
    map.put(pkgWings + "sm.states.RepeatState", RepeatState.class);
    map.put(pkgWings + "sm.states.RepeatState$RepeatStateExecutionData", RepeatStateExecutionData.class);
    map.put(pkgWings + "sm.states.ShellScriptState", ShellScriptState.class);
    map.put(pkgWings + "sm.states.SumoLogicAnalysisState", SumoLogicAnalysisState.class);
    map.put(pkgWings + "sm.WorkflowStandardParams", WorkflowStandardParams.class);
    map.put(pkgWings + "verification.appdynamics.AppDynamicsCVServiceConfiguration",
        AppDynamicsCVServiceConfiguration.class);
    map.put(pkgWings + "verification.log.BugsnagCVConfiguration", BugsnagCVConfiguration.class);
  }
}
