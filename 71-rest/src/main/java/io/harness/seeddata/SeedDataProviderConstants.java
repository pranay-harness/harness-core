package io.harness.seeddata;

public interface SeedDataProviderConstants {
  String KUBE_CLUSTER_NAME = "k8s-connection-example";

  String DOCKER_CONNECTOR_NAME = "Docker Hub Artifact Server";
  String DOCKER_HUB_ARTIFACT_SERVER = "https://registry.hub.docker.com/v2/";

  String DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME = "harness_todolist";
  String DOCKER_TODO_LIST_IMAGE_NAME = "harness/todolist_ga";

  String KUBERNETES_APP_NAME = "Sample App";
  String KUBERNETES_APP_DESC = "Sample To-Do List Application";

  String KUBERNETES_SERVICE_NAME = "To-Do List K8s";
  String KUBERNETES_SERVICE_DESC = "To Do List Docker Image";

  String KUBERNETES_SERVICE_INFRA_NAME = "To-Do List K8s";
  String KUBERNETES_SERVICE_INFRA_NAME_SPACE = "default";

  String KUBE_QA_ENVIRONMENT = "QA";
  String KUBE_PROD_ENVIRONMENT = "Prod";

  String KUBE_WORKFLOW_NAME = "To-Do List K8s";

  String KUBE_PIPELINE_NAME = "K8s Deployment";
}
