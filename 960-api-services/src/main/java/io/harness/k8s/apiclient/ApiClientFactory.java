/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.apiclient;

import io.harness.k8s.model.KubernetesConfig;

import io.kubernetes.client.openapi.ApiClient;

public interface ApiClientFactory {
  ApiClient getClient(KubernetesConfig kubernetesConfig);
}
