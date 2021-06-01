package io.harness.delegate.task.citasks.cik8handler.k8java;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.k8s.model.KubernetesConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceBuilder;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServicePortBuilder;
import io.kubernetes.client.openapi.models.V1Status;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIK8JavaClientHandler {
  @Inject private ApiClientFactory apiClientFactory;

  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 3;

  public V1Pod createOrReplacePodWithRetries(KubernetesConfig kubernetesConfig, V1Pod pod, String namespace) {
    RetryPolicy<Object> retryPolicy =
        getRetryPolicy("[Retrying failed to create pod; attempt: {}", "Failing pod creation after retrying {} times");
    return Failsafe.with(retryPolicy).get(() -> createOrReplacePod(kubernetesConfig, pod, namespace));
  }

  public V1Pod createOrReplacePod(KubernetesConfig kubernetesConfig, V1Pod pod, String namespace) throws ApiException {
    ApiClient apiClient = apiClientFactory.getClient(kubernetesConfig);
    V1Pod podGet = null;
    try {
      podGet = getPod(apiClient, pod.getMetadata().getName(), namespace);
    } catch (ApiException ex) {
      if (ex.getCode() != 404) {
        log.info("CreateOrReplace Pod: Pod get failed with err: %s", ex);
      }
    }

    if (podGet != null) {
      try {
        deletePod(apiClient, pod.getMetadata().getName(), namespace);
      } catch (ApiException ex) {
        log.info("CreateOrReplace Pod: Pod delete failed with err: %s", ex);
      }
    }

    return createPod(apiClient, pod, namespace);
  }

  private V1Pod createPod(ApiClient apiClient, V1Pod pod, String namespace) throws ApiException {
    CoreV1Api coreV1Api = new CoreV1Api(apiClient);
    return coreV1Api.createNamespacedPod(namespace, pod, null, null, null);
  }

  public V1Status deletePod(ApiClient apiClient, String podName, String namespace) throws ApiException {
    CoreV1Api coreV1Api = new CoreV1Api(apiClient);
    return coreV1Api.deleteNamespacedPod(podName, namespace, null, null, null, null, null, null);
  }

  public V1Pod getPod(ApiClient apiClient, String podName, String namespace) throws ApiException {
    CoreV1Api coreV1Api = new CoreV1Api(apiClient);
    return coreV1Api.readNamespacedPod(podName, namespace, null, null, null);
  }

  public void createService(KubernetesConfig kubernetesConfig, String namespace, String serviceName,
      Map<String, String> selectorMap, List<Integer> ports) throws ApiException {
    List<V1ServicePort> svcPorts = new ArrayList<>();
    for (int idx = 0; idx < ports.size(); idx++) {
      String name = String.format("%d-%d", ports.get(idx), idx);
      V1ServicePort servicePort = new V1ServicePortBuilder().withName(name).withPort(ports.get(idx)).build();
      svcPorts.add(servicePort);
    }

    V1Service svc = new V1ServiceBuilder()
                        .withNewMetadata()
                        .withName(serviceName)
                        .endMetadata()
                        .withNewSpec()
                        .withSelector(selectorMap)
                        .withPorts(svcPorts)
                        .endSpec()
                        .build();
    ApiClient apiClient = apiClientFactory.getClient(kubernetesConfig);
    CoreV1Api coreV1Api = new CoreV1Api(apiClient);
    coreV1Api.createNamespacedService(namespace, svc, null, null, null);
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
