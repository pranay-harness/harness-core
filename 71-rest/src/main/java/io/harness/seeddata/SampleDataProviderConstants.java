package io.harness.seeddata;

public interface SampleDataProviderConstants {
  String K8S_DELEGATE_NAME = "harness-sample-k8s-delegate";

  String K8S_CLOUD_PROVIDER_NAME = "Harness Sample K8s Cloud Provider";

  String HARNESS_DOCKER_HUB_CONNECTOR = "Harness Docker Hub";
  String HARNESS_DOCKER_HUB_CONNECTOR_URL = "https://registry.hub.docker.com/v2/";

  String DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME = "harness_todolist";
  String DOCKER_TODO_LIST_IMAGE_NAME = "harness/todolist-sample";

  String HARNESS_SAMPLE_APP = "Harness Sample App";
  String HARNESS_SAMPLE_APP_DESC = "A sample To-Do List application";

  String KUBERNETES_SERVICE_NAME = "To-Do List K8s";
  String KUBERNETES_SERVICE_DESC = "Sample To-Do List Docker Image";

  String KUBERNETES_SERVICE_INFRA_NAME = "To-Do List K8s";
  String KUBERNETES_SERVICE_INFRA_NAME_SPACE = "default";

  String KUBE_QA_ENVIRONMENT = "QA";
  String KUBE_PROD_ENVIRONMENT = "Prod";

  String KUBE_WORKFLOW_NAME = "To-Do List K8s";

  String KUBE_PIPELINE_NAME = "K8s Prod Pipeline";
}
