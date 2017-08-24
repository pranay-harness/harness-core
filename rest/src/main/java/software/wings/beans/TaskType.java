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

/**
 * Created by peeyushaggarwal on 12/8/16.
 */
public enum TaskType {
  HTTP(HttpTask.class),
  SPLUNK(HttpTask.class),
  APP_DYNAMICS(HttpTask.class),
  JENKINS(JenkinsTask.class),
  JENKINS_COLLECTION(JenkinsCollectionTask.class),
  BAMBOO_COLLECTION(BambooCollectionTask.class),
  COMMAND(CommandTask.class),
  JENKINS_GET_BUILDS(ServiceImplDelegateTask.class),
  JENKINS_GET_JOBS(ServiceImplDelegateTask.class),
  JENKINS_GET_ARTIFACT_PATHS(ServiceImplDelegateTask.class),
  JENKINS_LAST_SUCCESSFUL_BUILD(ServiceImplDelegateTask.class),
  JENKINS_GET_PLANS(ServiceImplDelegateTask.class),
  JENKINS_VALIDATE_ARTIFACT_SERVER(ServiceImplDelegateTask.class),
  BAMBOO_GET_BUILDS(ServiceImplDelegateTask.class),
  BAMBOO_GET_JOBS(ServiceImplDelegateTask.class),
  BAMBOO_GET_ARTIFACT_PATHS(ServiceImplDelegateTask.class),
  BAMBOO_LAST_SUCCESSFUL_BUILD(ServiceImplDelegateTask.class),
  BAMBOO_GET_PLANS(ServiceImplDelegateTask.class),
  BAMBOO_VALIDATE_ARTIFACT_SERVER(ServiceImplDelegateTask.class),
  DOCKER_GET_BUILDS(ServiceImplDelegateTask.class),
  DOCKER_VALIDATE_ARTIFACT_SERVER(ServiceImplDelegateTask.class),
  DOCKER_VALIDATE_ARTIFACT_STREAM(ServiceImplDelegateTask.class),
  ECR_GET_BUILDS(ServiceImplDelegateTask.class),
  ECR_VALIDATE_ARTIFACT_SERVER(ServiceImplDelegateTask.class),
  ECR_GET_PLANS(ServiceImplDelegateTask.class),
  ECR_GET_ARTIFACT_PATHS(ServiceImplDelegateTask.class),
  ECR_VALIDATE_ARTIFACT_STREAM(ServiceImplDelegateTask.class),
  GCR_GET_BUILDS(ServiceImplDelegateTask.class),
  GCR_VALIDATE_ARTIFACT_STREAM(ServiceImplDelegateTask.class),
  GCR_GET_PLANS(ServiceImplDelegateTask.class),
  NEXUS_GET_JOBS(ServiceImplDelegateTask.class),
  NEXUS_GET_PLANS(ServiceImplDelegateTask.class),
  NEXUS_GET_ARTIFACT_PATHS(ServiceImplDelegateTask.class),
  NEXUS_GET_GROUP_IDS(ServiceImplDelegateTask.class),
  NEXUS_GET_ARTIFACT_NAMES(ServiceImplDelegateTask.class),
  NEXUS_GET_BUILDS(ServiceImplDelegateTask.class),
  NEXUS_LAST_SUCCESSFUL_BUILD(ServiceImplDelegateTask.class),
  NEXUS_COLLECTION(NexusCollectionTask.class),
  NEXUS_VALIDATE_ARTIFACT_SERVER(ServiceImplDelegateTask.class),
  AMAZON_S3_COLLECTION(AmazonS3CollectionTask.class),
  AMAZON_S3_GET_ARTIFACT_PATHS(ServiceImplDelegateTask.class),
  AMAZON_S3_LAST_SUCCESSFUL_BUILD(ServiceImplDelegateTask.class),
  AMAZON_S3_GET_ARTIFACT_NAMES(ServiceImplDelegateTask.class),
  AMAZON_S3_GET_PLANS(ServiceImplDelegateTask.class),
  AMAZON_S3_VALIDATE_ARTIFACT_SERVER(ServiceImplDelegateTask.class),
  APPDYNAMICS_CONFIGURATION_VALIDATE_TASK(ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_BUSINESS_TRANSACTION_TASK(ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_APP_TASK(ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_TIER_TASK(ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_NODES_TASK(ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_METRICES_OF_BT(ServiceImplDelegateTask.class),
  APPDYNAMICS_GET_METRICES_DATA(ServiceImplDelegateTask.class),
  APPDYNAMICS_COLLECT_METRIC_DATA(AppdynamicsDataCollectionTask.class),
  SPLUNK_CONFIGURATION_VALIDATE_TASK(ServiceImplDelegateTask.class),
  SPLUNK_COLLECT_LOG_DATA(SplunkDataCollectionTask.class),
  ELK_CONFIGURATION_VALIDATE_TASK(ServiceImplDelegateTask.class),
  ELK_COLLECT_LOG_DATA(ElkLogzDataCollectionTask.class),
  LOGZ_CONFIGURATION_VALIDATE_TASK(ServiceImplDelegateTask.class),
  LOGZ_COLLECT_LOG_DATA(ElkLogzDataCollectionTask.class),
  ARTIFACTORY_GET_BUILDS(ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_JOBS(ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_PLANS(ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_ARTIFACTORY_PATHS(ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_GROUP_IDS(ServiceImplDelegateTask.class),
  ARTIFACTORY_GET_ARTIFACT_NAMES(ServiceImplDelegateTask.class),
  ARTIFACTORY_LAST_SUCCSSFUL_BUILD(ServiceImplDelegateTask.class),
  ARTIFACTORY_COLLECTION(ArtifactoryCollectionTask.class),
  ARTIFACTORY_VALIDATE_ARTIFACT_SERVER(ServiceImplDelegateTask.class),
  ARTIFACTORY_VALIDATE_ARTIFACT_STREAM(ServiceImplDelegateTask.class),
  HOST_VALIDATION(ServiceImplDelegateTask.class);

  private Class<? extends DelegateRunnableTask<?>> delegateRunnableTaskClass;

  TaskType(Class<? extends DelegateRunnableTask<?>> delegateRunnableTaskClass) {
    this.delegateRunnableTaskClass = delegateRunnableTaskClass;
  }

  public DelegateRunnableTask getDelegateRunnableTask(String delegateId, DelegateTask delegateTask,
      Consumer<? extends NotifyResponseData> postExecute, Supplier<Boolean> preExecute) {
    return on(delegateRunnableTaskClass).create(delegateId, delegateTask, postExecute, preExecute).get();
  }
}
