package software.wings.beans;

import static org.joor.Reflect.on;

import software.wings.delegatetasks.collect.artifacts.AmazonS3CollectionTask;
import software.wings.delegatetasks.collect.AppdynamicsDataCollectionTask;
import software.wings.delegatetasks.collect.artifacts.ArtifactoryCollectionTask;
import software.wings.delegatetasks.collect.artifacts.BambooCollectionTask;
import software.wings.delegatetasks.CommandTask;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.delegatetasks.ElkLogzDataCollectionTask;
import software.wings.delegatetasks.HttpTask;
import software.wings.delegatetasks.collect.artifacts.JenkinsCollectionTask;
import software.wings.delegatetasks.JenkinsTask;
import software.wings.delegatetasks.collect.artifacts.NexusCollectionTask;
import software.wings.delegatetasks.ServiceImplDelegateTask;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

public enum TaskType {
  HTTP(TaskGroup.HTTP, HttpTask.class),
  SPLUNK(TaskGroup.SPLUNK, HttpTask.class),
  APP_DYNAMICS(TaskGroup.APPDYNAMICS, HttpTask.class),
  JENKINS(TaskGroup.JENKINS, JenkinsTask.class),
  JENKINS_COLLECTION(TaskGroup.JENKINS, JenkinsCollectionTask.class),
  BAMBOO_COLLECTION(TaskGroup.BAMBOO, BambooCollectionTask.class),
  COMMAND(TaskGroup.COMMAND, CommandTask.class),
  JENKINS_GET_BUILDS(TaskGroup.JENKINS, ServiceImplDelegateTask.class),
  JENKINS_GET_JOBS(TaskGroup.JENKINS, ServiceImplDelegateTask.class),
  JENKINS_GET_ARTIFACT_PATHS(TaskGroup.JENKINS, ServiceImplDelegateTask.class),
  JENKINS_LAST_SUCCESSFUL_BUILD(TaskGroup.JENKINS, ServiceImplDelegateTask.class),
  JENKINS_GET_PLANS(TaskGroup.JENKINS, ServiceImplDelegateTask.class),
  JENKINS_VALIDATE_ARTIFACT_SERVER(TaskGroup.JENKINS, ServiceImplDelegateTask.class),
  BAMBOO_GET_BUILDS(TaskGroup.BAMBOO, ServiceImplDelegateTask.class),
  BAMBOO_GET_JOBS(TaskGroup.BAMBOO, ServiceImplDelegateTask.class),
  BAMBOO_GET_ARTIFACT_PATHS(TaskGroup.BAMBOO, ServiceImplDelegateTask.class),
  BAMBOO_LAST_SUCCESSFUL_BUILD(TaskGroup.BAMBOO, ServiceImplDelegateTask.class),
  BAMBOO_GET_PLANS(TaskGroup.BAMBOO, ServiceImplDelegateTask.class),
  BAMBOO_VALIDATE_ARTIFACT_SERVER(TaskGroup.BAMBOO, ServiceImplDelegateTask.class),
  DOCKER_GET_BUILDS(TaskGroup.DOCKER, ServiceImplDelegateTask.class),
  DOCKER_VALIDATE_ARTIFACT_SERVER(TaskGroup.DOCKER, ServiceImplDelegateTask.class),
  DOCKER_VALIDATE_ARTIFACT_STREAM(TaskGroup.DOCKER, ServiceImplDelegateTask.class),
  ECR_GET_BUILDS(TaskGroup.ECR, ServiceImplDelegateTask.class),
  ECR_VALIDATE_ARTIFACT_SERVER(TaskGroup.ECR, ServiceImplDelegateTask.class),
  ECR_GET_PLANS(TaskGroup.ECR, ServiceImplDelegateTask.class),
  ECR_GET_ARTIFACT_PATHS(TaskGroup.ECR, ServiceImplDelegateTask.class),
  ECR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ECR, ServiceImplDelegateTask.class),
  GCR_GET_BUILDS(TaskGroup.GCR, ServiceImplDelegateTask.class),
  GCR_VALIDATE_ARTIFACT_STREAM(TaskGroup.GCR, ServiceImplDelegateTask.class),
  GCR_GET_PLANS(TaskGroup.GCR, ServiceImplDelegateTask.class),
  NEXUS_GET_JOBS(TaskGroup.NEXUS, ServiceImplDelegateTask.class),
  NEXUS_GET_PLANS(TaskGroup.NEXUS, ServiceImplDelegateTask.class),
  NEXUS_GET_ARTIFACT_PATHS(TaskGroup.NEXUS, ServiceImplDelegateTask.class),
  NEXUS_GET_GROUP_IDS(TaskGroup.NEXUS, ServiceImplDelegateTask.class),
  NEXUS_GET_ARTIFACT_NAMES(TaskGroup.NEXUS, ServiceImplDelegateTask.class),
  NEXUS_GET_BUILDS(TaskGroup.NEXUS, ServiceImplDelegateTask.class),
  NEXUS_LAST_SUCCESSFUL_BUILD(TaskGroup.NEXUS, ServiceImplDelegateTask.class),
  NEXUS_COLLECTION(TaskGroup.NEXUS, NexusCollectionTask.class),
  NEXUS_VALIDATE_ARTIFACT_SERVER(TaskGroup.NEXUS, ServiceImplDelegateTask.class),
  AMAZON_S3_COLLECTION(TaskGroup.S3, AmazonS3CollectionTask.class),
  AMAZON_S3_GET_ARTIFACT_PATHS(TaskGroup.S3, ServiceImplDelegateTask.class),
  AMAZON_S3_LAST_SUCCESSFUL_BUILD(TaskGroup.S3, ServiceImplDelegateTask.class),
  AMAZON_S3_GET_ARTIFACT_NAMES(TaskGroup.S3, ServiceImplDelegateTask.class),
  AMAZON_S3_GET_PLANS(TaskGroup.S3, ServiceImplDelegateTask.class),
  AMAZON_S3_VALIDATE_ARTIFACT_SERVER(TaskGroup.S3, ServiceImplDelegateTask.class),
  APPDYNAMICS_CONFIGURATION_VALIDATE_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_BUSINESS_TRANSACTION_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_APP_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_TIER_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_NODES_TASK(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_METRICES_OF_BT(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_METRICES_DATA(TaskGroup.APPDYNAMICS, ServiceImplDelegateTask.class),
  APPDYNAMICS_COLLECT_METRIC_DATA(TaskGroup.APPDYNAMICS, AppdynamicsDataCollectionTask.class),
  SPLUNK_CONFIGURATION_VALIDATE_TASK(TaskGroup.SPLUNK, ServiceImplDelegateTask.class),
  SPLUNK_COLLECT_LOG_DATA(TaskGroup.SPLUNK, SplunkDataCollectionTask.class),
  ELK_CONFIGURATION_VALIDATE_TASK(TaskGroup.ELK, ServiceImplDelegateTask.class),
  ELK_COLLECT_LOG_DATA(TaskGroup.ELK, ElkLogzDataCollectionTask.class),
  ELK_COLLECT_INDICES(TaskGroup.ELK, ServiceImplDelegateTask.class),
  ELK_GET_LOG_SAMPLE(TaskGroup.ELK, ServiceImplDelegateTask.class),
  LOGZ_CONFIGURATION_VALIDATE_TASK(TaskGroup.LOGZ, ServiceImplDelegateTask.class),
  LOGZ_COLLECT_LOG_DATA(TaskGroup.LOGZ, ElkLogzDataCollectionTask.class),
  LOGZ_GET_LOG_SAMPLE(TaskGroup.LOGZ, ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_BUILDS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_JOBS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_PLANS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_ARTIFACTORY_PATHS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_GROUP_IDS(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_ARTIFACT_NAMES(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  ARTIFACTORY_LAST_SUCCSSFUL_BUILD(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  ARTIFACTORY_COLLECTION(TaskGroup.ARTIFACTORY, ArtifactoryCollectionTask.class),
  ARTIFACTORY_VALIDATE_ARTIFACT_SERVER(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  ARTIFACTORY_VALIDATE_ARTIFACT_STREAM(TaskGroup.ARTIFACTORY, ServiceImplDelegateTask.class),
  HOST_VALIDATION(TaskGroup.HOST_VALIDATION, ServiceImplDelegateTask.class);

  private TaskGroup taskGroup;
  private Class<? extends DelegateRunnableTask<?>> delegateRunnableTaskClass;

  TaskType(TaskGroup taskGroup, Class<? extends DelegateRunnableTask<?>> delegateRunnableTaskClass) {
    this.taskGroup = taskGroup;
    this.delegateRunnableTaskClass = delegateRunnableTaskClass;
  }

  public TaskGroup getTaskGroup() {
    return taskGroup;
  }

  public DelegateRunnableTask getDelegateRunnableTask(String delegateId, DelegateTask delegateTask,
      Consumer<? extends NotifyResponseData> postExecute, Supplier<Boolean> preExecute) {
    return on(delegateRunnableTaskClass).create(delegateId, delegateTask, postExecute, preExecute).get();
  }
}
