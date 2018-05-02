package software.wings.beans;

import static org.joor.Reflect.on;

import com.google.inject.Injector;

import software.wings.delegatetasks.APMDataCollectionTask;
import software.wings.delegatetasks.AppdynamicsDataCollectionTask;
import software.wings.delegatetasks.BambooTask;
import software.wings.delegatetasks.CloudWatchDataCollectionTask;
import software.wings.delegatetasks.CollaborationProviderTask;
import software.wings.delegatetasks.CommandTask;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.delegatetasks.DynaTraceDataCollectionTask;
import software.wings.delegatetasks.ElkLogzDataCollectionTask;
import software.wings.delegatetasks.GitCommandTask;
import software.wings.delegatetasks.HelmCommandTask;
import software.wings.delegatetasks.HttpTask;
import software.wings.delegatetasks.JenkinsTask;
import software.wings.delegatetasks.KubernetesSteadyStateCheckTask;
import software.wings.delegatetasks.NewRelicDataCollectionTask;
import software.wings.delegatetasks.NewRelicDeploymentMarkerTask;
import software.wings.delegatetasks.PrometheusDataCollectionTask;
import software.wings.delegatetasks.ServiceImplDelegateTask;
import software.wings.delegatetasks.ShellScriptTask;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.delegatetasks.SumoDataCollectionTask;
import software.wings.delegatetasks.collect.artifacts.AmazonS3CollectionTask;
import software.wings.delegatetasks.collect.artifacts.ArtifactoryCollectionTask;
import software.wings.delegatetasks.collect.artifacts.BambooCollectionTask;
import software.wings.delegatetasks.collect.artifacts.JenkinsCollectionTask;
import software.wings.delegatetasks.collect.artifacts.NexusCollectionTask;
import software.wings.delegatetasks.pcf.PcfCommandTask;
import software.wings.delegatetasks.validation.APMValidation;
import software.wings.delegatetasks.validation.AlwaysTrueValidation;
import software.wings.delegatetasks.validation.AppdynamicsValidation;
import software.wings.delegatetasks.validation.ArtifactoryValidation;
import software.wings.delegatetasks.validation.BambooValidation;
import software.wings.delegatetasks.validation.CollaborationProviderTaskValidation;
import software.wings.delegatetasks.validation.CommandValidation;
import software.wings.delegatetasks.validation.ContainerValidation;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.DelegateValidateTask;
import software.wings.delegatetasks.validation.DockerValidation;
import software.wings.delegatetasks.validation.DynaTraceValidation;
import software.wings.delegatetasks.validation.ElkValidation;
import software.wings.delegatetasks.validation.GcrValidation;
import software.wings.delegatetasks.validation.GitValidation;
import software.wings.delegatetasks.validation.HostValidationValidation;
import software.wings.delegatetasks.validation.HttpValidation;
import software.wings.delegatetasks.validation.JenkinsValidation;
import software.wings.delegatetasks.validation.KubernetesSteadyStateCheckValidation;
import software.wings.delegatetasks.validation.LogzValidation;
import software.wings.delegatetasks.validation.NewRelicValidation;
import software.wings.delegatetasks.validation.NexusValidation;
import software.wings.delegatetasks.validation.PrometheusValidation;
import software.wings.delegatetasks.validation.ShellScriptValidation;
import software.wings.delegatetasks.validation.SplunkValidation;
import software.wings.delegatetasks.validation.SumoValidation;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public enum TaskType {
  COMMAND(TaskGroup.COMMAND, CommandTask.class, CommandValidation.class),
  SCRIPT(TaskGroup.SCRIPT, ShellScriptTask.class, ShellScriptValidation.class),
  HTTP(TaskGroup.HTTP, HttpTask.class, HttpValidation.class),
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
  DOCKER_VALIDATE_ARTIFACT_SERVER(TaskGroup.DOCKER, ServiceImplDelegateTask.class, DockerValidation.class),
  DOCKER_VALIDATE_ARTIFACT_STREAM(TaskGroup.DOCKER, ServiceImplDelegateTask.class, DockerValidation.class),
  ECR_GET_BUILDS(TaskGroup.ECR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  ECR_VALIDATE_ARTIFACT_SERVER(TaskGroup.ECR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  ECR_GET_PLANS(TaskGroup.ECR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  ECR_GET_ARTIFACT_PATHS(TaskGroup.ECR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  ECR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ECR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  GCR_GET_BUILDS(TaskGroup.GCR, ServiceImplDelegateTask.class, GcrValidation.class),
  GCR_VALIDATE_ARTIFACT_STREAM(TaskGroup.GCR, ServiceImplDelegateTask.class, GcrValidation.class),
  GCR_GET_PLANS(TaskGroup.GCR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  ACR_GET_BUILDS(TaskGroup.ACR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  ACR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ACR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  ACR_GET_PLANS(TaskGroup.ACR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  ACR_GET_ARTIFACT_PATHS(TaskGroup.ACR, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  NEXUS_GET_JOBS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_GET_PLANS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_GET_ARTIFACT_PATHS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_GET_GROUP_IDS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_GET_BUILDS(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_LAST_SUCCESSFUL_BUILD(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  NEXUS_COLLECTION(TaskGroup.NEXUS, NexusCollectionTask.class, NexusValidation.class),
  NEXUS_VALIDATE_ARTIFACT_SERVER(TaskGroup.NEXUS, ServiceImplDelegateTask.class, NexusValidation.class),
  AMAZON_S3_COLLECTION(TaskGroup.S3, AmazonS3CollectionTask.class, AlwaysTrueValidation.class),
  AMAZON_S3_GET_ARTIFACT_PATHS(TaskGroup.S3, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  AMAZON_S3_LAST_SUCCESSFUL_BUILD(TaskGroup.S3, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  AMAZON_S3_GET_BUILDS(TaskGroup.S3, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  AMAZON_S3_GET_PLANS(TaskGroup.S3, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  APM_VALIDATE_CONNECTOR_TASK(TaskGroup.APM, ServiceImplDelegateTask.class, APMValidation.class),
  APPDYNAMICS_CONFIGURATION_VALIDATE_TASK(
      TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_GET_APP_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_GET_TIER_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class, AppdynamicsValidation.class),
  APPDYNAMICS_COLLECT_METRIC_DATA(
      TaskGroup.APPDYNAMICS, AppdynamicsDataCollectionTask.class, AppdynamicsValidation.class),
  NEWRELIC_VALIDATE_CONFIGURATION_TASK(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_GET_APP_TASK(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_GET_APP_INSTANCES_TASK(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_COLLECT_METRIC_DATA(TaskGroup.NEWRELIC, NewRelicDataCollectionTask.class, NewRelicValidation.class),
  NEWRELIC_GET_METRICES_DATA(TaskGroup.NEWRELIC, ServiceImplDelegateTask.class, NewRelicValidation.class),
  NEWRELIC_POST_DEPLOYMENT_MARKER(TaskGroup.NEWRELIC, NewRelicDeploymentMarkerTask.class, NewRelicValidation.class),
  SPLUNK(TaskGroup.SPLUNK, HttpTask.class, SplunkValidation.class),
  SPLUNK_CONFIGURATION_VALIDATE_TASK(TaskGroup.SPLUNK, ServiceImplDelegateTask.class, SplunkValidation.class),
  SPLUNK_COLLECT_LOG_DATA(TaskGroup.SPLUNK, SplunkDataCollectionTask.class, SplunkValidation.class),
  SUMO_COLLECT_LOG_DATA(TaskGroup.SUMO, SumoDataCollectionTask.class, SumoValidation.class),
  SUMO_VALIDATE_CONFIGURATION_TASK(TaskGroup.SUMO, ServiceImplDelegateTask.class, SumoValidation.class),
  ELK_CONFIGURATION_VALIDATE_TASK(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  ELK_COLLECT_LOG_DATA(TaskGroup.ELK, ElkLogzDataCollectionTask.class, ElkValidation.class),
  ELK_COLLECT_INDICES(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  ELK_GET_LOG_SAMPLE(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  KIBANA_GET_VERSION(TaskGroup.ELK, ServiceImplDelegateTask.class, ElkValidation.class),
  LOGZ_CONFIGURATION_VALIDATE_TASK(TaskGroup.LOGZ, ServiceImplDelegateTask.class, LogzValidation.class),
  LOGZ_COLLECT_LOG_DATA(TaskGroup.LOGZ, ElkLogzDataCollectionTask.class, LogzValidation.class),
  LOGZ_GET_LOG_SAMPLE(TaskGroup.LOGZ, ServiceImplDelegateTask.class, LogzValidation.class),
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
  KMS_ENCRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  KMS_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  GIT_COMMAND(TaskGroup.GIT, GitCommandTask.class, GitValidation.class),
  VAULT_ENCRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  VAULT_DECRYPT(TaskGroup.KMS, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  HOST_VALIDATION(TaskGroup.HOST_VALIDATION, ServiceImplDelegateTask.class, HostValidationValidation.class),
  CONTAINER_ACTIVE_SERVICE_COUNTS(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  CONTAINER_INFO(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  CONTROLLER_NAMES_WITH_LABELS(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  AMI_GET_BUILDS(TaskGroup.AMI, ServiceImplDelegateTask.class, AlwaysTrueValidation.class),
  CONTAINER_CONNECTION_VALIDATION(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  FETCH_CONTAINER_INFO(TaskGroup.CONTAINER, ServiceImplDelegateTask.class, ContainerValidation.class),
  DYNA_TRACE_VALIDATE_CONFIGURATION_TASK(
      TaskGroup.DYNA_TRACE, ServiceImplDelegateTask.class, DynaTraceValidation.class),
  DYNA_TRACE_METRIC_DATA_COLLECTION_TASK(
      TaskGroup.DYNA_TRACE, DynaTraceDataCollectionTask.class, DynaTraceValidation.class),
  HELM_COMMAND_TASK(TaskGroup.HELM, HelmCommandTask.class, AlwaysTrueValidation.class),
  KUBERNETES_STEADY_STATE_CHECK_TASK(
      TaskGroup.CONTAINER, KubernetesSteadyStateCheckTask.class, KubernetesSteadyStateCheckValidation.class),
  PCF_COMMAND_TASK(TaskGroup.PCF, PcfCommandTask.class, AlwaysTrueValidation.class),
  COLLABORATION_PROVIDER_TASK(
      TaskGroup.COLLABORATION_PROVIDER, CollaborationProviderTask.class, CollaborationProviderTaskValidation.class),
  PROMETHEUS_VALIDATE_CONFIGURATION_TASK(
      TaskGroup.PROMETHEUS, ServiceImplDelegateTask.class, PrometheusValidation.class),
  PROMETHEUS_METRIC_DATA_COLLECTION_TASK(
      TaskGroup.PROMETHEUS, PrometheusDataCollectionTask.class, PrometheusValidation.class),
  CLOUD_WATCH_COLLECT_METRIC_DATA(
      TaskGroup.CLOUD_WATCH, CloudWatchDataCollectionTask.class, AlwaysTrueValidation.class),
  APM_METRIC_DATA_COLLECTION_TASK(TaskGroup.APM, APMDataCollectionTask.class, APMValidation.class);

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

  public DelegateRunnableTask getDelegateRunnableTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> postExecute, Supplier<Boolean> preExecute) {
    return on(delegateRunnableTaskClass).create(delegateId, delegateTask, postExecute, preExecute).get();
  }

  public DelegateValidateTask getDelegateValidateTask(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    return on(delegateValidateTaskClass).create(delegateId, delegateTask, postExecute).get();
  }

  public List<String> getCriteria(DelegateTask delegateTask, Injector injector) {
    DelegateValidateTask delegateValidateTask = on(delegateValidateTaskClass).create(null, delegateTask, null).get();
    injector.injectMembers(delegateValidateTask);
    return delegateValidateTask.getCriteria();
  }
}
