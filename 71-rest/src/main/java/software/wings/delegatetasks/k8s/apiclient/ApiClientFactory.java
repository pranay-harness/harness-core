package software.wings.delegatetasks.k8s.apiclient;

import io.kubernetes.client.openapi.ApiClient;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

public interface ApiClientFactory { ApiClient getClient(K8sClusterConfig k8sClusterConfig); }
