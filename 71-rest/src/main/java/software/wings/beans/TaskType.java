package software.wings.beans;

import static org.joor.Reflect.on;

import com.google.inject.Injector;

import io.harness.beans.DelegateTask;
import io.harness.cdng.connector.tasks.KubernetesConnectionDelegateValidation;
import io.harness.cdng.connector.tasks.KubernetesTestConnectionDelegateTask;
import io.harness.cdng.git.tasks.NGGitConnectionValidation;
import io.harness.cdng.k8s.K8sCommandValidationNG;
import io.harness.cdng.secrets.tasks.SSHConfigValidationDelegateTask;
import io.harness.cvng.connectiontask.AppDynamicsTestConnectionDelegateTask;
import io.harness.cvng.connectiontask.SplunkTestConnectionDelegateTask;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.task.DelegateRunnableTask;
import io.harness.delegate.task.artifacts.docker.DockerArtifactTaskNG;
import io.harness.delegate.task.docker.DockerTestConnectionDelegateTask;
import io.harness.delegate.task.gcp.GcpTask;
import io.harness.delegate.task.git.GitFetchTaskNG;
import io.harness.delegate.task.git.NGGitCommandTask;
import io.harness.delegate.task.k8s.K8sTaskNG;
import io.harness.delegate.task.stepstatus.StepStatusTask;
import io.harness.perpetualtask.internal.AssignmentTask;
import software.wings.delegatetasks.APMDataCollectionTask;
import software.wings.delegatetasks.AppdynamicsDataCollectionTask;
import software.wings.delegatetasks.BambooTask;
import software.wings.delegatetasks.BuildSourceTask;
import software.wings.delegatetasks.CloudWatchDataCollectionTask;
import software.wings.delegatetasks.CollaborationProviderTask;
import software.wings.delegatetasks.CommandTask;
import software.wings.delegatetasks.ConnectivityValidationTask;
import software.wings.delegatetasks.DynaTraceDataCollectionTask;
import software.wings.delegatetasks.EcsSteadyStateCheckTask;
import software.wings.delegatetasks.ElkLogzDataCollectionTask;
import software.wings.delegatetasks.GcbTask;
import software.wings.delegatetasks.GitCommandTask;
import software.wings.delegatetasks.GitFetchFilesTask;
import software.wings.delegatetasks.HelmRepoConfigValidationTask;
import software.wings.delegatetasks.HostValidationTask;
import software.wings.delegatetasks.HttpTask;
import software.wings.delegatetasks.JenkinsTask;
import software.wings.delegatetasks.KubernetesSteadyStateCheckTask;
import software.wings.delegatetasks.KubernetesSwapServiceSelectorsTask;
import software.wings.delegatetasks.LogDataCollectionTask;
import software.wings.delegatetasks.NewRelicDataCollectionTask;
import software.wings.delegatetasks.NewRelicDeploymentMarkerTask;
import software.wings.delegatetasks.PerpetualTaskCapabilityCheckTask;
import software.wings.delegatetasks.ServiceImplDelegateTask;
import software.wings.delegatetasks.ShellScriptTask;
import software.wings.delegatetasks.StackDriverDataCollectionTask;
import software.wings.delegatetasks.StackDriverLogDataCollectionTask;
import software.wings.delegatetasks.SumoDataCollectionTask;
import software.wings.delegatetasks.TerraformFetchTargetsTask;
import software.wings.delegatetasks.TerraformInputVariablesObtainTask;
import software.wings.delegatetasks.TerraformProvisionTask;
import software.wings.delegatetasks.TriggerTask;
import software.wings.delegatetasks.aws.AwsAmiAsyncTask;
import software.wings.delegatetasks.aws.AwsAsgTask;
import software.wings.delegatetasks.aws.AwsCFTask;
import software.wings.delegatetasks.aws.AwsCodeDeployTask;
import software.wings.delegatetasks.aws.AwsEc2Task;
import software.wings.delegatetasks.aws.AwsEcrTask;
import software.wings.delegatetasks.aws.AwsEcsTask;
import software.wings.delegatetasks.aws.AwsElbTask;
import software.wings.delegatetasks.aws.AwsIamTask;
import software.wings.delegatetasks.aws.AwsLambdaTask;
import software.wings.delegatetasks.aws.AwsRoute53Task;
import software.wings.delegatetasks.aws.AwsS3Task;
import software.wings.delegatetasks.aws.ecs.EcsCommandTask;
import software.wings.delegatetasks.azure.AzureVMSSTask;
import software.wings.delegatetasks.citasks.CIBuildCommandTask;
import software.wings.delegatetasks.citasks.CICleanupTask;
import software.wings.delegatetasks.citasks.ExecuteCommandTask;
import software.wings.delegatetasks.cloudformation.CloudFormationCommandTask;
import software.wings.delegatetasks.collect.artifacts.AmazonS3CollectionTask;
import software.wings.delegatetasks.collect.artifacts.ArtifactoryCollectionTask;
import software.wings.delegatetasks.collect.artifacts.AzureArtifactsCollectionTask;
import software.wings.delegatetasks.collect.artifacts.BambooCollectionTask;
import software.wings.delegatetasks.collect.artifacts.JenkinsCollectionTask;
import software.wings.delegatetasks.collect.artifacts.NexusCollectionTask;
import software.wings.delegatetasks.container.ContainerDummyTask;
import software.wings.delegatetasks.cv.MetricsDataCollectionTask;
import software.wings.delegatetasks.delegatecapability.CapabilityCheckController;
import software.wings.delegatetasks.helm.HelmCommandTask;
import software.wings.delegatetasks.helm.HelmValuesFetchTask;
import software.wings.delegatetasks.jira.JiraTask;
import software.wings.delegatetasks.jira.ShellScriptApprovalTask;
import software.wings.delegatetasks.k8s.K8sTask;
import software.wings.delegatetasks.pcf.PcfCommandTask;
import software.wings.delegatetasks.servicenow.ServicenowTask;
import software.wings.delegatetasks.shellscript.provisioner.ShellScriptProvisionTask;
import software.wings.delegatetasks.spotinst.SpotInstTask;
import software.wings.delegatetasks.validation.APMValidation;
import software.wings.delegatetasks.validation.AcrValidation;
import software.wings.delegatetasks.validation.AlwaysTrueValidation;
import software.wings.delegatetasks.validation.AppDynamicsNGValidation;
import software.wings.delegatetasks.validation.AppdynamicsValidation;
import software.wings.delegatetasks.validation.ArtifactoryValidation;
import software.wings.delegatetasks.validation.AwsConnectionValidation;
import software.wings.delegatetasks.validation.AzureArtifactsValidation;
import software.wings.delegatetasks.validation.BambooValidation;
import software.wings.delegatetasks.validation.BuildSourceTaskValidation;
import software.wings.delegatetasks.validation.CVDataCollectionValidator;
import software.wings.delegatetasks.validation.CollaborationProviderTaskValidation;
import software.wings.delegatetasks.validation.CommandValidation;
import software.wings.delegatetasks.validation.ConnectivityBasicValidation;
import software.wings.delegatetasks.validation.ContainerValidation;
import software.wings.delegatetasks.validation.CustomArtifactSourceValidation;
import software.wings.delegatetasks.validation.DataCollectionValidator;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.DelegateValidateTask;
import software.wings.delegatetasks.validation.DockerValidation;
import software.wings.delegatetasks.validation.DynaTraceValidation;
import software.wings.delegatetasks.validation.ElkValidation;
import software.wings.delegatetasks.validation.GcbValidation;
import software.wings.delegatetasks.validation.GcpValidationTask;
import software.wings.delegatetasks.validation.GcrValidation;
import software.wings.delegatetasks.validation.GcsValidation;
import software.wings.delegatetasks.validation.GitFetchFilesValidation;
import software.wings.delegatetasks.validation.GitValidation;
import software.wings.delegatetasks.validation.HelmCommandValidation;
import software.wings.delegatetasks.validation.HelmRepoConfigValidation;
import software.wings.delegatetasks.validation.HostValidationValidation;
import software.wings.delegatetasks.validation.HttpValidation;
import software.wings.delegatetasks.validation.InstanaValidation;
import software.wings.delegatetasks.validation.JenkinsValidation;
import software.wings.delegatetasks.validation.JiraValidation;
import software.wings.delegatetasks.validation.K8sCommandValidation;
import software.wings.delegatetasks.validation.KubernetesSteadyStateCheckValidation;
import software.wings.delegatetasks.validation.KubernetesSwapServiceSelectorsValidation;
import software.wings.delegatetasks.validation.LDAPValidation;
import software.wings.delegatetasks.validation.LogzValidation;
import software.wings.delegatetasks.validation.MasterUrlFetchValidation;
import software.wings.delegatetasks.validation.NGDockerValidation;
import software.wings.delegatetasks.validation.NewRelicValidation;
import software.wings.delegatetasks.validation.NexusValidation;
import software.wings.delegatetasks.validation.PCFCommandValidation;
import software.wings.delegatetasks.validation.PrometheusValidation;
import software.wings.delegatetasks.validation.SecretManagerValidation;
import software.wings.delegatetasks.validation.ServicenowValidation;
import software.wings.delegatetasks.validation.SftpValidation;
import software.wings.delegatetasks.validation.ShellScriptValidation;
import software.wings.delegatetasks.validation.SlackValidation;
import software.wings.delegatetasks.validation.SmbValidation;
import software.wings.delegatetasks.validation.SplunkNGValidation;
import software.wings.delegatetasks.validation.SplunkValidation;
import software.wings.delegatetasks.validation.StackDriverValidation;
import software.wings.delegatetasks.validation.SumoValidation;
import software.wings.delegatetasks.validation.TerraformValidation;
import software.wings.delegatetasks.validation.TriggerValidation;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public enum TaskType {
  CAPABILITY_VALIDATION(
      TaskGroup.CAPABILITY_VALIDATION, PerpetualTaskCapabilityCheckTask.class, AlwaysTrueValidation.class),
  COMMAND(TaskGroup.COMMAND, CommandTask.class, CommandValidation.class),
  SCRIPT(TaskGroup.SCRIPT, ShellScriptTask.class, ShellScriptValidation.class),
  HTTP(TaskGroup.HTTP, HttpTask.class, HttpValidation.class),
  GCB(TaskGroup.GCB, GcbTask.class, GcbValidation.class),
  JENKINS(TaskGroup.JENKINS, JenkinsTask.class, JenkinsValidation.class),
  JENKINS_COLLECTION(TaskGroup.JENKINS, JenkinsCollectionTask.class, JenkinsValidation.class),
  JENKINS_GET_BUILDS(TaskGroup.JENKINS, ServiceImplDelegateTask.class, JenkinsValidation.class),
  JENKINS_GET_JOBS(TaskGroup.JENKINS, ServiceImplDelegateTask.class, JenkinsValidation.class),
  JENKINS_GET_JOB(TaskGroup.JENKINS, ServiceImplDelegateTask.class, JenkinsValidation.class),
  JENKINS_GET_ARTIFACT_PATHS(TaskGroup.JENKINS, ServiceImplDelegateTask.class, JenkinsValidation.class),
  JENKINS_LAST_SUCCESSFUL_BUILD(TaskGroup.JENKINS, ServiceImplDelegateTask.class, JenkinsValidation.class),
  JENKINS_GET_PLANS(TaskGroup.JENKINS, ServiceImplDelegateTask.class, JenkinsValidation.class),
  JENKINS_VALIDATE_ARTIFACT_SERVER(TaskGroup.JENKINS, ServiceImplDelegateTask.class, JenkinsValidation.class),
  BAMBOO(TaskGroup.BAMBOO, BambooTask.class, BambooValidation.class),
  BAMBOO_COLLECTION(TaskGroup.BAMBOO, BambooCollectionTask.class, BambooValidation.class),
  BAMBOO_GET_BUILDS(TaskGroup.BAMBOO, ServiceImplDelegateTask.class, BambooValidation.class),
  BAMBOO_GET_JOBS(TaskGroup.BAMBOO, ServiceImplDelegateTask.class, BambooValidation.class),
  BAMBOO_GET_ARTIFACT_PATHS(TaskGroup.BAMBOO, ServiceImplDelegateTask.class, BambooValidation.class),
  BAMBOO_LAST_SUCCESSFUL_BUILD(TaskGroup.BAMBOO, ServiceImplDelegateTask.class, BambooValidation.class),
  BAMBOO_GET_PLANS(TaskGroup.BAMBOO, ServiceImplDelegateTask.class, BambooValidation.class),
  BAMBOO_VALIDATE_ARTIFACT_SERVER(TaskGroup.BAMBOO, ServiceImplDelegateTask.class, BambooValidation.class),
  DOCKER_GET_BUILDS(TaskGroup.DOCKER, ServiceImplDelegateTask.class, DockerValidation.class),
  DOCKER_GET_LABELS(TaskGroup.DOCKER, ServiceImplDelegateTask.class, DockerValidation.class),
  DOCKER_VALIDATE_ARTIFACT_SERVER(TaskGroup.DOCKER, ServiceImplDelegateTask.class, DockerValidation.class),
  DOCKER_VALIDATE_ARTIFACT_STREAM(TaskGroup.DOCKER, ServiceImplDelegateTask.class, DockerValidation.class),
  ECR_GET_BUILDS(TaskGroup.ECR, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  ECR_VALIDATE_ARTIFACT_SERVER(TaskGroup.ECR, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  ECR_GET_PLANS(TaskGroup.ECR, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  ECR_GET_ARTIFACT_PATHS(TaskGroup.ECR, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  ECR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ECR, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  ECR_GET_LABELS(TaskGroup.ECR, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  GCR_GET_BUILDS(TaskGroup.GCR, ServiceImplDelegateTask.class, GcrValidation.class),
  GCR_VALIDATE_ARTIFACT_STREAM(TaskGroup.GCR, ServiceImplDelegateTask.class, GcrValidation.class),
  GCR_GET_PLANS(TaskGroup.GCR, ServiceImplDelegateTask.class, GcrValidation.class),
  ACR_GET_BUILDS(TaskGroup.ACR, ServiceImplDelegateTask.class, AcrValidation.class),
  ACR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ACR, ServiceImplDelegateTask.class, AcrValidation.class),
  ACR_GET_PLANS(TaskGroup.ACR, ServiceImplDelegateTask.class, AcrValidation.class),
  ACR_GET_ARTIFACT_PATHS(TaskGroup.ACR, ServiceImplDelegateTask.class, AcrValidation.class),
  NEXUS_GET_JOBS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_GET_PLANS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_GET_ARTIFACT_PATHS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_GET_GROUP_IDS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_GET_BUILDS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_LAST_SUCCESSFUL_BUILD(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_COLLECTION(TaskGroup.NEXUS, NexusCollectionTask.class, NexusValidation.class),
  NEXUS_VALIDATE_ARTIFACT_SERVER(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_VALIDATE_ARTIFACT_STREAM(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  GCS_GET_ARTIFACT_PATHS(TaskGroup.GCS, ServiceImplDelegateTask.class, GcsValidation.class),
  GCS_GET_BUILDS(TaskGroup.GCS, ServiceImplDelegateTask.class, GcsValidation.class),
  GCS_GET_BUCKETS(TaskGroup.GCS, ServiceImplDelegateTask.class, GcsValidation.class),
  GCS_GET_PLANS(TaskGroup.GCS, ServiceImplDelegateTask.class, GcsValidation.class),
  SFTP_GET_BUILDS(TaskGroup.SFTP, ServiceImplDelegateTask.class, SftpValidation.class),
  SFTP_GET_ARTIFACT_PATHS(TaskGroup.SFTP, ServiceImplDelegateTask.class, SftpValidation.class),
  SFTP_VALIDATE_ARTIFACT_SERVER(TaskGroup.SFTP, ServiceImplDelegateTask.class, SftpValidation.class),
  SMB_GET_BUILDS(TaskGroup.SMB, ServiceImplDelegateTask.class, SmbValidation.class),
  SMB_GET_SMB_PATHS(TaskGroup.SMB, ServiceImplDelegateTask.class, SmbValidation.class),
  SMB_VALIDATE_ARTIFACT_SERVER(TaskGroup.SMB, ServiceImplDelegateTask.class, SmbValidation.class),
  AMAZON_S3_COLLECTION(TaskGroup.S3, AmazonS3CollectionTask.class, AwsConnectionValidation.class),
  AMAZON_S3_GET_ARTIFACT_PATHS(TaskGroup.S3, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  AMAZON_S3_LAST_SUCCESSFUL_BUILD(TaskGroup.S3, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  AMAZON_S3_GET_BUILDS(TaskGroup.S3, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  AMAZON_S3_GET_PLANS(TaskGroup.S3, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  AZURE_ARTIFACTS_VALIDATE_ARTIFACT_SERVER(
      TaskGroup.AZURE_ARTIFACTS, ServiceImplDelegateTask.class, AzureArtifactsValidation.class),
  AZURE_ARTIFACTS_VALIDATE_ARTIFACT_STREAM(
      TaskGroup.AZURE_ARTIFACTS, ServiceImplDelegateTask.class, AzureArtifactsValidation.class),
  AZURE_ARTIFACTS_GET_BUILDS(TaskGroup.AZURE_ARTIFACTS, ServiceImplDelegateTask.class, AzureArtifactsValidation.class),
  AZURE_ARTIFACTS_GET_PROJECTS(
      TaskGroup.AZURE_ARTIFACTS, ServiceImplDelegateTask.class, AzureArtifactsValidation.class),
  AZURE_ARTIFACTS_GET_FEEDS(TaskGroup.AZURE_ARTIFACTS, ServiceImplDelegateTask.class, AzureArtifactsValidation.class),
  AZURE_ARTIFACTS_GET_PACKAGES(
      TaskGroup.AZURE_ARTIFACTS, ServiceImplDelegateTask.class, AzureArtifactsValidation.class),
  AZURE_ARTIFACTS_COLLECTION(
      TaskGroup.AZURE_ARTIFACTS, AzureArtifactsCollectionTask.class, AzureArtifactsValidation.class),
  AZURE_VMSS_COMMAND_TASK(TaskGroup.AZURE_VMSS, AzureVMSSTask.class, AlwaysTrueValidation.class),
  LDAP_TEST_CONN_SETTINGS(TaskGroup.LDAP, ServiceImplDelegateTask.class, LDAPValidation.class),
  LDAP_TEST_USER_SETTINGS(TaskGroup.LDAP, ServiceImplDelegateTask.class, LDAPValidation.class),
  LDAP_TEST_GROUP_SETTINGS(TaskGroup.LDAP, ServiceImplDelegateTask.class, LDAPValidation.class),
  LDAP_VALIDATE_SETTINGS(TaskGroup.LDAP, ServiceImplDelegateTask.class, LDAPValidation.class),
  LDAP_AUTHENTICATION(TaskGroup.LDAP, ServiceImplDelegateTask.class, LDAPValidation.class),
  LDAP_SEARCH_GROUPS(TaskGroup.LDAP, ServiceImplDelegateTask.class, LDAPValidation.class),
  LDAP_FETCH_GROUP(TaskGroup.LDAP, ServiceImplDelegateTask.class, LDAPValidation.class),
  APM_VALIDATE_CONNECTOR_TASK(TaskGroup.APM, ServiceImplDelegateTask.class, APMValidation.class),
  CUSTOM_LOG_VALIDATE_CONNECTOR_TASK(TaskGroup.LOG, ServiceImplDelegateTask.class, APMValidation.class),
  APM_GET_TASK(TaskGroup.APM, ServiceImplDelegateTask.class, APMValidation.class),
  APPDYNAMICS_CONFIGURATION_VALIDATE_TASK(
      TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_NG_CONFIGURATION_VALIDATE_TASK(
      TaskGroup.APPDYNAMICS, AppDynamicsTestConnectionDelegateTask.class, AppDynamicsNGValidation.class),
  APPDYNAMICS_GET_APP_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_GET_APP_TASK_NG(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppDynamicsNGValidation.class),
  APPDYNAMICS_GET_TIER_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_GET_TIER_TASK_NG(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppDynamicsNGValidation.class),
  APPDYNAMICS_GET_TIER_MAP(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_COLLECT_METRIC_DATA(
      TaskGroup.APPDYNAMICS, AppdynamicsDataCollectionTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_COLLECT_METRIC_DATA_V2(
      TaskGroup.APPDYNAMICS, MetricsDataCollectionTask.class, DataCollectionValidator.class),
  APPDYNAMICS_COLLECT_24_7_METRIC_DATA(
      TaskGroup.GUARD_24x7, AppdynamicsDataCollectionTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_METRIC_DATA_FOR_NODE(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_METRIC_PACK_DATA(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppDynamicsNGValidation.class),
  INSTANA_GET_INFRA_METRICS(TaskGroup.INSTANA, ServiceImplDelegateTask.class, InstanaValidation.class),
  INSTANA_GET_TRACE_METRICS(TaskGroup.INSTANA, ServiceImplDelegateTask.class, InstanaValidation.class),
  INSTANA_COLLECT_METRIC_DATA(TaskGroup.INSTANA, MetricsDataCollectionTask.class, DataCollectionValidator.class),
  INSTANA_VALIDATE_CONFIGURATION_TASK(TaskGroup.INSTANA, ServiceImplDelegateTask.class, InstanaValidation.class),
  NEWRELIC_VALIDATE_CONFIGURATION_TASK(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  BUGSNAG_GET_APP_TASK(TaskGroup.LOG, ServiceImplDelegateTask.class, APMValidation.class),
  BUGSNAG_GET_RECORDS(TaskGroup.LOG, ServiceImplDelegateTask.class, APMValidation.class),
  CUSTOM_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7, LogDataCollectionTask.class, APMValidation.class),
  CUSTOM_APM_COLLECT_METRICS_V2(TaskGroup.APM, MetricsDataCollectionTask.class, APMValidation.class),
  NEWRELIC_GET_APP_TASK(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_RESOLVE_APP_TASK(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_RESOLVE_APP_ID_TASK(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_GET_APP_INSTANCES_TASK(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_COLLECT_METRIC_DATA(TaskGroup.NEWRELIC, NewRelicDataCollectionTask.class, NewRelicValidation.class),
  NEWRELIC_COLLECT_METRIC_DATAV2(TaskGroup.NEWRELIC, MetricsDataCollectionTask.class, DataCollectionValidator.class),
  NEWRELIC_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7, NewRelicDataCollectionTask.class, NewRelicValidation.class),
  NEWRELIC_GET_TXNS_WITH_DATA(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_GET_TXNS_WITH_DATA_FOR_NODE(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_POST_DEPLOYMENT_MARKER(TaskGroup.NEWRELIC, NewRelicDeploymentMarkerTask.class, NewRelicValidation.class),
  STACKDRIVER_COLLECT_METRIC_DATA(
      TaskGroup.STACKDRIVER, StackDriverDataCollectionTask.class, StackDriverValidation.class),
  STACKDRIVER_METRIC_DATA_FOR_NODE(TaskGroup.STACKDRIVER, ServiceImplDelegateTask.class, StackDriverValidation.class),
  STACKDRIVER_LOG_DATA_FOR_NODE(TaskGroup.STACKDRIVER, ServiceImplDelegateTask.class, StackDriverValidation.class),
  STACKDRIVER_LIST_REGIONS(TaskGroup.STACKDRIVER, ServiceImplDelegateTask.class, StackDriverValidation.class),
  STACKDRIVER_LIST_FORWARDING_RULES(TaskGroup.STACKDRIVER, ServiceImplDelegateTask.class, StackDriverValidation.class),
  STACKDRIVER_GET_LOG_SAMPLE(TaskGroup.STACKDRIVER, ServiceImplDelegateTask.class, StackDriverValidation.class),
  STACKDRIVER_COLLECT_24_7_METRIC_DATA(
      TaskGroup.GUARD_24x7, StackDriverDataCollectionTask.class, StackDriverValidation.class),
  STACKDRIVER_COLLECT_LOG_DATA(
      TaskGroup.STACKDRIVER, StackDriverLogDataCollectionTask.class, StackDriverValidation.class),
  STACKDRIVER_COLLECT_24_7_LOG_DATA(
      TaskGroup.GUARD_24x7, StackDriverLogDataCollectionTask.class, StackDriverValidation.class),
  SPLUNK(TaskGroup.SPLUNK, HttpTask.class, SplunkValidation.class),
  SPLUNK_CONFIGURATION_VALIDATE_TASK(TaskGroup.SPLUNK, ServiceImplDelegateTask.class, SplunkValidation.class),
  SPLUNK_NG_CONFIGURATION_VALIDATE_TASK(
      TaskGroup.SPLUNK, SplunkTestConnectionDelegateTask.class, SplunkNGValidation.class),
  SPLUNK_GET_HOST_RECORDS(TaskGroup.SPLUNK, ServiceImplDelegateTask.class, SplunkValidation.class),
  SPLUNK_NG_GET_SAVED_SEARCHES(TaskGroup.SPLUNK, ServiceImplDelegateTask.class, SplunkNGValidation.class),
  SPLUNK_NG_VALIDATION_RESPONSE_TASK(TaskGroup.SPLUNK, ServiceImplDelegateTask.class, SplunkNGValidation.class),
  SPLUNK_COLLECT_LOG_DATAV2(
      TaskGroup.SPLUNK, software.wings.delegatetasks.cv.LogDataCollectionTask.class, DataCollectionValidator.class),
  ELK_COLLECT_LOG_DATAV2(
      TaskGroup.ELK, software.wings.delegatetasks.cv.LogDataCollectionTask.class, DataCollectionValidator.class),
  DATA_COLLECTION_NEXT_GEN_VALIDATION(
      TaskGroup.APPDYNAMICS, MetricsDataCollectionTask.class, CVDataCollectionValidator.class),
  SUMO_COLLECT_LOG_DATA(TaskGroup.SUMO, SumoDataCollectionTask.class, SumoValidation.class),
  SUMO_VALIDATE_CONFIGURATION_TASK(TaskGroup.SUMO, ServiceImplDelegateTask.class, SumoValidation.class),
  SUMO_GET_HOST_RECORDS(TaskGroup.SUMO, ServiceImplDelegateTask.class, SumoValidation.class),
  SUMO_GET_LOG_DATA_BY_HOST(TaskGroup.SUMO, ServiceImplDelegateTask.class, SumoValidation.class),
  SUMO_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7, SumoDataCollectionTask.class, SumoValidation.class),
  ELK_CONFIGURATION_VALIDATE_TASK(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  ELK_COLLECT_LOG_DATA(TaskGroup.ELK, ElkLogzDataCollectionTask.class, ElkValidation.class),
  ELK_COLLECT_INDICES(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  ELK_GET_LOG_SAMPLE(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  ELK_GET_HOST_RECORDS(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  KIBANA_GET_VERSION(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  ELK_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7, ElkLogzDataCollectionTask.class, ElkValidation.class),
  LOGZ_CONFIGURATION_VALIDATE_TASK(TaskGroup.LOGZ, ServiceImplDelegateTask.class, LogzValidation.class),
  LOGZ_COLLECT_LOG_DATA(TaskGroup.LOGZ, ElkLogzDataCollectionTask.class, LogzValidation.class),
  LOGZ_GET_LOG_SAMPLE(TaskGroup.LOGZ, ServiceImplDelegateTask.class, LogzValidation.class),
  LOGZ_GET_HOST_RECORDS(TaskGroup.ELK, ServiceImplDelegateTask.class, LogzValidation.class),
  ARTIFACTORY_GET_BUILDS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class, ArtifactoryValidation.class),
  ARTIFACTORY_GET_JOBS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class, ArtifactoryValidation.class),
  ARTIFACTORY_GET_PLANS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class, ArtifactoryValidation.class),
  ARTIFACTORY_GET_ARTIFACTORY_PATHS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class, ArtifactoryValidation.class),
  ARTIFACTORY_GET_GROUP_IDS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class, ArtifactoryValidation.class),
  ARTIFACTORY_LAST_SUCCSSFUL_BUILD(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class, ArtifactoryValidation.class),
  ARTIFACTORY_COLLECTION(TaskGroup.ARTIFACTORY, ArtifactoryCollectionTask.class, ArtifactoryValidation.class),
  ARTIFACTORY_VALIDATE_ARTIFACT_SERVER(
      TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class, ArtifactoryValidation.class),
  ARTIFACTORY_VALIDATE_ARTIFACT_STREAM(
      TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class, ArtifactoryValidation.class),

  // Secret manager validation
  CYBERARK_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  CYBERARK_VALIDATE_CONFIG(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  AZURE_VAULT_ENCRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  AZURE_VAULT_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  AZURE_VAULT_DELETE(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  KMS_ENCRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  KMS_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  GCP_KMS_ENCRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  GCP_KMS_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  VAULT_ENCRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  VAULT_DELETE_SECRET(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  VAULT_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  VAULT_GET_CHANGELOG(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  VAULT_RENEW_TOKEN(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  VAULT_LIST_ENGINES(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  VAULT_APPROLE_LOGIN(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  ASM_ENCRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  ASM_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  ASM_DELETE_SECRET(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  SECRET_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  BATCH_SECRET_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),
  SECRET_DECRYPT_REF(TaskGroup.KMS, ServiceImplDelegateTask.class, SecretManagerValidation.class),

  HOST_VALIDATION(TaskGroup.HOST_VALIDATION, HostValidationTask.class, HostValidationValidation.class),
  CONTAINER_ACTIVE_SERVICE_COUNTS(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  CONTAINER_INFO(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  CONTROLLER_NAMES_WITH_LABELS(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  AMI_GET_BUILDS(TaskGroup.AMI, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  CONTAINER_CE_VALIDATION(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  CONTAINER_CONNECTION_VALIDATION(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  LIST_CLUSTERS(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  CONTAINER_VALIDATION(TaskGroup.CONTAINER, ContainerDummyTask.class, ContainerValidation.class),

  FETCH_MASTER_URL(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, MasterUrlFetchValidation.class),

  DYNA_TRACE_VALIDATE_CONFIGURATION_TASK(
      TaskGroup.DYNA_TRACE, ServiceImplDelegateTask.class, DynaTraceValidation.class),
  DYNA_TRACE_METRIC_DATA_COLLECTION_TASK(
      TaskGroup.DYNA_TRACE, DynaTraceDataCollectionTask.class, DynaTraceValidation.class),
  DYNA_TRACE_GET_TXNS_WITH_DATA_FOR_NODE(
      TaskGroup.DYNA_TRACE, ServiceImplDelegateTask.class, DynaTraceValidation.class),
  DYNATRACE_COLLECT_24_7_METRIC_DATA(
      TaskGroup.GUARD_24x7, DynaTraceDataCollectionTask.class, DynaTraceValidation.class),
  HELM_COMMAND_TASK(TaskGroup.HELM, HelmCommandTask.class, HelmCommandValidation.class),
  KUBERNETES_STEADY_STATE_CHECK_TASK(
      TaskGroup.CONTAINER, KubernetesSteadyStateCheckTask.class, KubernetesSteadyStateCheckValidation.class),
  PCF_COMMAND_TASK(TaskGroup.PCF, PcfCommandTask.class, PCFCommandValidation.class),
  SPOTINST_COMMAND_TASK(TaskGroup.SPOTINST, SpotInstTask.class, AlwaysTrueValidation.class),
  ECS_COMMAND_TASK(TaskGroup.AWS, EcsCommandTask.class, AwsConnectionValidation.class),
  COLLABORATION_PROVIDER_TASK(
      TaskGroup.COLLABORATION_PROVIDER, CollaborationProviderTask.class, CollaborationProviderTaskValidation.class),
  PROMETHEUS_METRIC_DATA_PER_HOST(TaskGroup.PROMETHEUS, ServiceImplDelegateTask.class, PrometheusValidation.class),
  CLOUD_WATCH_COLLECT_METRIC_DATA(
      TaskGroup.CLOUD_WATCH, CloudWatchDataCollectionTask.class, AwsConnectionValidation.class),
  CLOUD_WATCH_METRIC_DATA_FOR_NODE(TaskGroup.CLOUD_WATCH, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  CLOUD_WATCH_GENERIC_METRIC_STATISTICS(
      TaskGroup.CLOUD_WATCH, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  CLOUD_WATCH_GENERIC_METRIC_DATA(TaskGroup.CLOUD_WATCH, ServiceImplDelegateTask.class, AwsConnectionValidation.class),
  CLOUD_WATCH_COLLECT_24_7_METRIC_DATA(
      TaskGroup.GUARD_24x7, CloudWatchDataCollectionTask.class, AwsConnectionValidation.class),
  APM_METRIC_DATA_COLLECTION_TASK(TaskGroup.APM, APMDataCollectionTask.class, APMValidation.class),

  APM_24_7_METRIC_DATA_COLLECTION_TASK(TaskGroup.GUARD_24x7, APMDataCollectionTask.class, APMValidation.class),

  CUSTOM_LOG_COLLECTION_TASK(TaskGroup.LOG, LogDataCollectionTask.class, APMValidation.class),
  CLOUD_FORMATION_TASK(TaskGroup.CLOUD_FORMATION, CloudFormationCommandTask.class, AwsConnectionValidation.class),

  TERRAFORM_PROVISION_TASK(TaskGroup.TERRAFORM, TerraformProvisionTask.class, TerraformValidation.class),
  TERRAFORM_INPUT_VARIABLES_OBTAIN_TASK(
      TaskGroup.TERRAFORM, TerraformInputVariablesObtainTask.class, TerraformValidation.class),
  TERRAFORM_FETCH_TARGETS_TASK(TaskGroup.TERRAFORM, TerraformFetchTargetsTask.class, TerraformValidation.class),
  KUBERNETES_SWAP_SERVICE_SELECTORS_TASK(
      TaskGroup.CONTAINER, KubernetesSwapServiceSelectorsTask.class, KubernetesSwapServiceSelectorsValidation.class),
  ECS_STEADY_STATE_CHECK_TASK(TaskGroup.CONTAINER, EcsSteadyStateCheckTask.class, AwsConnectionValidation.class),
  AWS_ECR_TASK(TaskGroup.AWS, AwsEcrTask.class, AwsConnectionValidation.class),
  AWS_ELB_TASK(TaskGroup.AWS, AwsElbTask.class, AwsConnectionValidation.class),
  AWS_ECS_TASK(TaskGroup.AWS, AwsEcsTask.class, AwsConnectionValidation.class),
  AWS_IAM_TASK(TaskGroup.AWS, AwsIamTask.class, AwsConnectionValidation.class),
  AWS_EC2_TASK(TaskGroup.AWS, AwsEc2Task.class, AwsConnectionValidation.class),
  AWS_ASG_TASK(TaskGroup.AWS, AwsAsgTask.class, AwsConnectionValidation.class),
  AWS_CODE_DEPLOY_TASK(TaskGroup.AWS, AwsCodeDeployTask.class, AwsConnectionValidation.class),
  AWS_LAMBDA_TASK(TaskGroup.AWS, AwsLambdaTask.class, AwsConnectionValidation.class),
  AWS_AMI_ASYNC_TASK(TaskGroup.AWS, AwsAmiAsyncTask.class, AwsConnectionValidation.class),
  AWS_CF_TASK(TaskGroup.AWS, AwsCFTask.class, AwsConnectionValidation.class),
  K8S_COMMAND_TASK(TaskGroup.K8S, K8sTask.class, K8sCommandValidation.class),
  K8S_COMMAND_TASK_NG(TaskGroup.K8S_NG, K8sTaskNG.class, K8sCommandValidationNG.class),
  K8S_WATCH_TASK(TaskGroup.K8S, AssignmentTask.class, K8sCommandValidation.class),
  TRIGGER_TASK(TaskGroup.TRIGGER, TriggerTask.class, TriggerValidation.class),
  JIRA(TaskGroup.JIRA, JiraTask.class, JiraValidation.class),
  CONNECTIVITY_VALIDATION(
      TaskGroup.CONNECTIVITY_VALIDATION, ConnectivityValidationTask.class, ConnectivityBasicValidation.class),
  GIT_COMMAND(TaskGroup.GIT, GitCommandTask.class, GitValidation.class),
  GIT_FETCH_FILES_TASK(TaskGroup.GIT, GitFetchFilesTask.class, GitFetchFilesValidation.class),
  GIT_FETCH_NEXT_GEN_TASK(TaskGroup.GIT, GitFetchTaskNG.class, AlwaysTrueValidation.class),
  BUILD_SOURCE_TASK(TaskGroup.BUILD_SOURCE, BuildSourceTask.class, BuildSourceTaskValidation.class),
  DOCKER_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG, DockerArtifactTaskNG.class, AlwaysTrueValidation.class),
  AWS_ROUTE53_TASK(TaskGroup.AWS, AwsRoute53Task.class, AwsConnectionValidation.class),
  SHELL_SCRIPT_APPROVAL(TaskGroup.SCRIPT, ShellScriptApprovalTask.class, ShellScriptApprovalValidation.class),
  CUSTOM_GET_BUILDS(TaskGroup.CUSTOM, ServiceImplDelegateTask.class, CustomArtifactSourceValidation.class),
  CUSTOM_VALIDATE_ARTIFACT_STREAM(
      TaskGroup.CUSTOM, ServiceImplDelegateTask.class, CustomArtifactSourceValidation.class),
  SHELL_SCRIPT_PROVISION_TASK(
      TaskGroup.SHELL_SCRIPT_PROVISION, ShellScriptProvisionTask.class, AlwaysTrueValidation.class),
  SERVICENOW_ASYNC(TaskGroup.SERVICENOW, ServicenowTask.class, ServicenowValidation.class),
  SERVICENOW_SYNC(TaskGroup.SERVICENOW, ServiceImplDelegateTask.class, ServicenowValidation.class),
  SERVICENOW_VALIDATION(TaskGroup.SERVICENOW, ServiceImplDelegateTask.class, ServicenowValidation.class),
  HELM_REPO_CONFIG_VALIDATION(
      TaskGroup.HELM_REPO_CONFIG_VALIDATION, HelmRepoConfigValidationTask.class, HelmRepoConfigValidation.class),
  HELM_VALUES_FETCH(TaskGroup.HELM_VALUES_FETCH_TASK, HelmValuesFetchTask.class, HelmRepoConfigValidation.class),
  SLACK(TaskGroup.SLACK, ServiceImplDelegateTask.class, SlackValidation.class),
  CI_BUILD(TaskGroup.CI, CIBuildCommandTask.class, AlwaysTrueValidation.class),
  CI_LE_STATUS(TaskGroup.CI, StepStatusTask.class, AlwaysTrueValidation.class),
  EXECUTE_COMMAND(TaskGroup.CI, ExecuteCommandTask.class, AlwaysTrueValidation.class),
  CI_CLEANUP(TaskGroup.CI, CICleanupTask.class, AlwaysTrueValidation.class),
  AWS_S3_TASK(TaskGroup.AWS, AwsS3Task.class, AwsConnectionValidation.class),
  // Add all NG tasks below this.
  GCP_TASK(TaskGroup.GCP, GcpTask.class, GcpValidationTask.class),
  VALIDATE_KUBERNETES_CONFIG(
      TaskGroup.CONTAINER, KubernetesTestConnectionDelegateTask.class, KubernetesConnectionDelegateValidation.class),
  NG_GIT_COMMAND(TaskGroup.GIT, NGGitCommandTask.class, NGGitConnectionValidation.class),
  NG_SSH_VALIDATION(
      TaskGroup.CONNECTIVITY_VALIDATION, SSHConfigValidationDelegateTask.class, AlwaysTrueValidation.class),
  DOCKER_CONNECTIVITY_TEST_TASK(TaskGroup.DOCKER, DockerTestConnectionDelegateTask.class, NGDockerValidation.class);

  private final TaskGroup taskGroup;
  private final Class<? extends DelegateRunnableTask> delegateRunnableTaskClass;
  private final Class<? extends DelegateValidateTask> delegateValidateTaskClass;

  TaskType(TaskGroup taskGroup, Class<? extends DelegateRunnableTask> delegateRunnableTaskClass,
      Class<? extends DelegateValidateTask> delegateValidateTaskClass) {
    this.taskGroup = taskGroup;
    this.delegateRunnableTaskClass = delegateRunnableTaskClass;
    this.delegateValidateTaskClass = delegateValidateTaskClass;
  }

  public TaskGroup getTaskGroup() {
    return taskGroup;
  }

  public DelegateRunnableTask getDelegateRunnableTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    return on(delegateRunnableTaskClass).create(delegateTaskPackage, postExecute, preExecute).get();
  }

  public DelegateValidateTask getDelegateValidateTask(String delegateId, DelegateTaskPackage delegateTaskPackage,
      Consumer<List<DelegateConnectionResult>> postExecute) {
    return on(delegateValidateTaskClass).create(delegateId, delegateTaskPackage, postExecute).get();
  }

  // TODO: This should become default 1, once all tasks are migrated and feature flag is removed.
  public DelegateValidateTask getDelegateValidateTaskVersionForCapabilityFramework(String delegateId,
      DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> postExecute) {
    return on(CapabilityCheckController.class).create(delegateId, delegateTaskPackage, postExecute).get();
  }

  public List<String> getCriteria(DelegateTaskPackage delegateTaskPackage, Injector injector) {
    DelegateValidateTask delegateValidateTask =
        on(delegateValidateTaskClass).create(null, delegateTaskPackage, null).get();
    injector.injectMembers(delegateValidateTask);
    return delegateValidateTask.getCriteria();
  }

  // TODO: This should become default 1, once all tasks are migrated and feature flag is removed.
  public List<String> getCriteriaVersionForCapabilityFramework(DelegateTask delegateTask, Injector injector) {
    DelegateValidateTask delegateValidateTask =
        on(CapabilityCheckController.class).create(null, delegateTask, null).get();
    injector.injectMembers(delegateValidateTask);
    return delegateValidateTask.getCriteria();
  }
}
