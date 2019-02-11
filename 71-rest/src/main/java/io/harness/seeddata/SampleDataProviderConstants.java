package io.harness.seeddata;

public interface SampleDataProviderConstants {
  String K8S_DELEGATE_NAME = "harness-sample-k8s-delegate";

  String K8S_CLOUD_PROVIDER_NAME = "Harness Sample K8s Cloud Provider";

  String HARNESS_DOCKER_HUB_CONNECTOR = "Harness Docker Hub";
  String HARNESS_DOCKER_HUB_CONNECTOR_URL = "https://registry.hub.docker.com/v2/";

  String DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME = "harness_todolist";
  String DOCKER_TODO_LIST_IMAGE_NAME = "harness/todolist-sample";

  String HARNESS_SAMPLE_APP = "Harness Sample App";
  String HARNESS_SAMPLE_APP_V2 = "Harness Sample App V2";
  String HARNESS_SAMPLE_APP_DESC = "A sample To-Do List application";

  String K8S_SERVICE_NAME = "To-Do List K8s";
  String K8S_SERVICE_DESC = "Sample To-Do List Docker Image";

  String K8S_SERVICE_INFRA_NAME = "To-Do List K8s";
  String K8S_SERVICE_INFRA_DEFAULT_NAMESPACE = "default";
  String K8S_SERVICE_INFRA_QA_NAMESPACE = "qa";
  String K8S_SERVICE_INFRA_PROD_NAMESPACE = "prod";

  String K8S_QA_ENVIRONMENT = "QA";
  String K8S_PROD_ENVIRONMENT = "Prod";

  String K8S_BASIC_WORKFLOW_NAME = "To-Do List K8s Basic";
  String K8S_ROLLING_WORKFLOW_NAME = "To-Do List K8s Rolling";
  String K8S_CANARY_WORKFLOW_NAME = "To-Do List K8s Canary";

  String K8S_PIPELINE_NAME = "K8s Prod Pipeline";
}
