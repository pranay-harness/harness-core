/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.seeddata;

import io.harness.expression.ExpressionEvaluator;

public interface SampleDataProviderConstants {
  String K8S_DELEGATE_NAME = "harness-sample-k8s-delegate";

  String K8S_CLOUD_PROVIDER_NAME = "Harness Sample K8s Cloud Provider";

  String HARNESS_DOCKER_HUB_CONNECTOR = "Harness Docker Hub";
  String HARNESS_DOCKER_HUB_CONNECTOR_URL = "https://registry.hub.docker.com/v2/";

  String DOCKER_TODO_LIST_ARTIFACT_SOURCE_NAME = "harness_todolist";
  String DOCKER_TODO_LIST_IMAGE_NAME = "harness/todolist-sample";

  String HARNESS_SAMPLE_APP = "Harness Sample App";
  String HARNESS_SAMPLE_APP_DESC = "A sample To-Do List application";

  String K8S_SERVICE_NAME = "To-Do List K8s";
  String K8S_SERVICE_DESC = "Sample To-Do List Docker Image";

  String K8S_SERVICE_INFRA_NAME = "To-Do List K8s";
  String K8S_SERVICE_INFRA_DEFAULT_NAMESPACE = "default";

  String K8S_INFRA_NAME = "K8s";

  String K8S_QA_ENVIRONMENT = "qa";
  String K8S_PROD_ENVIRONMENT = "prod";

  String K8S_BASIC_WORKFLOW_NAME = "To-Do List K8s Basic";
  String K8S_ROLLING_WORKFLOW_NAME = "To-Do List K8s Rolling";
  String K8S_CANARY_WORKFLOW_NAME = "To-Do List K8s Canary";

  String K8S_PIPELINE_NAME = "K8s Prod Pipeline";

  String ARTIFACT_VARIABLE_NAME = ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME;
}
