package io.harness.k8s.apiclient;

import static io.harness.k8s.KubernetesHelperService.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import io.harness.k8s.model.KubernetesConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import io.kubernetes.client.util.credentials.UsernamePasswordAuthentication;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

@Singleton
public class ApiClientFactoryImpl implements ApiClientFactory {
  @Override
  public ApiClient getClient(KubernetesConfig kubernetesConfig) {
    return fromKubernetesConfig(kubernetesConfig);
  }

  public static ApiClient fromKubernetesConfig(KubernetesConfig kubernetesConfig) {
    // should we cache the client ?
    return createNewApiClient(kubernetesConfig);
  }

  private static ApiClient createNewApiClient(KubernetesConfig kubernetesConfig) {
    // this is insecure, but doing this for parity with how our fabric8 client behaves.
    ClientBuilder clientBuilder = new ClientBuilder().setVerifyingSsl(false);
    if (isNotBlank(kubernetesConfig.getMasterUrl())) {
      clientBuilder.setBasePath(kubernetesConfig.getMasterUrl());
    }
    if (kubernetesConfig.getCaCert() != null) {
      clientBuilder.setCertificateAuthority(encode(kubernetesConfig.getCaCert()).getBytes(UTF_8));
    }
    if (kubernetesConfig.getServiceAccountToken() != null) {
      clientBuilder.setAuthentication(
          new AccessTokenAuthentication(new String(kubernetesConfig.getServiceAccountToken())));
    } else if (kubernetesConfig.getUsername() != null && kubernetesConfig.getPassword() != null) {
      clientBuilder.setAuthentication(new UsernamePasswordAuthentication(
          new String(kubernetesConfig.getUsername()), new String(kubernetesConfig.getPassword())));
    } else if (kubernetesConfig.getClientCert() != null && kubernetesConfig.getClientKey() != null) {
      clientBuilder.setAuthentication(
          new ClientCertificateAuthentication(new String(kubernetesConfig.getClientCert()).getBytes(UTF_8),
              new String(kubernetesConfig.getClientKey()).getBytes(UTF_8)));
    }
    ApiClient apiClient = clientBuilder.build();
    // don't timeout on client-side
    OkHttpClient httpClient = apiClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
    apiClient.setHttpClient(httpClient);
    return apiClient;
  }
}