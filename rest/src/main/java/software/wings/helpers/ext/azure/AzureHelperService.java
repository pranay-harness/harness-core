package software.wings.helpers.ext.azure;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import com.google.inject.Singleton;

import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.AccessKeyType;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import com.microsoft.rest.LogLevel;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.AzureConfig;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.utils.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class AzureHelperService {
  private static final Logger logger = LoggerFactory.getLogger(AzureHelperService.class);

  private static final int CONNECT_TIMEOUT = 5; // TODO:: read from config

  public void validateAzureAccountCredential(String clientId, String tenantId, String key) {
    try {
      ApplicationTokenCredentials credentials =
          new ApplicationTokenCredentials(clientId, tenantId, key, AzureEnvironment.AZURE);

      Azure azure = Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();

      azure.getCurrentSubscription().listLocations();

    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
  }

  public Map<String, String> listSubscriptions(AzureConfig azureConfig) {
    Azure azure = getAzureClient(azureConfig);
    Map<String, String> subscriptionMap = new HashMap<>();
    azure.subscriptions().list().forEach(sub -> subscriptionMap.put(sub.subscriptionId(), sub.displayName()));
    return subscriptionMap;
  }

  public List<String> listContainerRegistries(AzureConfig azureConfig, String subscriptionId) {
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    List<String> registries = new ArrayList<>();
    azure.containerRegistries().list().forEach(registry -> registries.add(registry.name()));
    return registries;
  }

  public List<String> listRepositories(AzureConfig azureConfig, String subscriptionId, String registryName) {
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    List<String> registries = new ArrayList<>();
    Registry registry =
        azure.containerRegistries().list().stream().filter(item -> item.name().equals(registryName)).findFirst().get();
    RegistryCredentials credentials = registry.getCredentials();
    AcrRestClient acrRestClient = getAcrRestClient(registry.loginServerUrl());
    try {
      return acrRestClient
          .listRepositories(getAuthHeader(credentials.username(), credentials.accessKeys().get(AccessKeyType.PRIMARY)))
          .execute()
          .body()
          .getRepositories();
    } catch (IOException e) {
      logger.error("Error occurred while getting repositories from subscriptionId/registryName :" + subscriptionId + "/"
              + registryName,
          e);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", e.getMessage());
    }
  }

  public List<String> listRepositoryTags(
      AzureConfig azureConfig, String subscriptionId, String registryName, String repositoryName) {
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    List<String> registries = new ArrayList<>();
    Registry registry =
        azure.containerRegistries().list().stream().filter(item -> item.name().equals(registryName)).findFirst().get();
    RegistryCredentials credentials = registry.getCredentials();
    AcrRestClient acrRestClient = getAcrRestClient(registry.loginServerUrl());
    try {
      return acrRestClient
          .listRepositoryTags(
              getAuthHeader(credentials.username(), credentials.accessKeys().get(AccessKeyType.PRIMARY)),
              repositoryName)
          .execute()
          .body()
          .getTags();
    } catch (IOException e) {
      logger.error("Error occurred while getting repositories from subscriptionId/registryName/repositoryName :"
              + subscriptionId + "/" + registryName + "/" + repositoryName,
          e);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", e.getMessage());
    }
  }

  private Azure getAzureClient(AzureConfig azureConfig) {
    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
          azureConfig.getClientId(), azureConfig.getTenantId(), azureConfig.getKey(), AzureEnvironment.AZURE);

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
    return null;
  }

  private Azure getAzureClient(AzureConfig azureConfig, String subscriptionId) {
    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
          azureConfig.getClientId(), azureConfig.getTenantId(), azureConfig.getKey(), AzureEnvironment.AZURE);

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withSubscription(subscriptionId);
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
    return null;
  }

  private AcrRestClient getAcrRestClient(String registryHostName) {
    String url = getUrl(registryHostName);
    OkHttpClient okHttpClient = new OkHttpClient()
                                    .newBuilder()
                                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .proxy(HttpUtil.checkAndGetNonProxyIfApplicable(url))
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(AcrRestClient.class);
  }

  private String getAuthHeader(String username, String password) {
    return "Basic " + encodeBase64String(format("%s:%s", username, password).getBytes());
  }

  private String getUrl(String acrHostName) {
    return "https://" + acrHostName + (acrHostName.endsWith("/") ? "" : "/");
  }

  private void HandleAzureAuthenticationException(Exception e) {
    Throwable e1 = e;
    while (e1.getCause() != null) {
      e1 = e1.getCause();
      if (e1 instanceof AuthenticationException) {
        throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER)
            .addParam("message", "Invalid Azure credentials." + e1.getMessage());
      }
    }

    throw new WingsException(ErrorCode.UNKNOWN_ERROR).addParam("message", e.getMessage());
  }
}