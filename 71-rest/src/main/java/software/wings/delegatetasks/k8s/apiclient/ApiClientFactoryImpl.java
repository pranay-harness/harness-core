package software.wings.delegatetasks.k8s.apiclient;

import static io.harness.k8s.KubernetesHelperService.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.k8s.model.KubernetesConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AuthorizationV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import io.kubernetes.client.util.credentials.ClientCertificateAuthentication;
import io.kubernetes.client.util.credentials.UsernamePasswordAuthentication;
import okhttp3.OkHttpClient;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;

import java.util.concurrent.TimeUnit;

@Singleton
public class ApiClientFactoryImpl implements ApiClientFactory {
  private final ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  @Inject
  public ApiClientFactoryImpl(ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper) {
    this.containerDeploymentDelegateHelper = containerDeploymentDelegateHelper;
  }

  @Override
  public ApiClient getClient(K8sClusterConfig k8sClusterConfig) {
    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig);
    return fromKubernetesConfig(kubernetesConfig);
  }

  public static AuthorizationV1Api getAuthorizationClient(KubernetesConfig kubernetesConfig) {
    return new AuthorizationV1Api(fromKubernetesConfig(kubernetesConfig));
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
    } else if (isNotBlank(kubernetesConfig.getUsername()) && kubernetesConfig.getPassword() != null) {
      clientBuilder.setAuthentication(new UsernamePasswordAuthentication(
          kubernetesConfig.getUsername(), new String(kubernetesConfig.getPassword())));
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