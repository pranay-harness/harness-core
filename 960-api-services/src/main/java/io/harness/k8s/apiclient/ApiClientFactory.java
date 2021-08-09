package io.harness.k8s.apiclient;

import io.harness.k8s.model.KubernetesConfig;

import io.kubernetes.client.openapi.ApiClient;

public interface ApiClientFactory {
  ApiClient getClient(KubernetesConfig kubernetesConfig);
}
