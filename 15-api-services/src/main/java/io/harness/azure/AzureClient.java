package io.harness.azure;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Singleton;

import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.rest.LogLevel;
import io.harness.azure.model.AzureConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AzureClient {
  protected Azure getAzureClient(AzureConfig azureConfig) {
    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureConfig.getClientId(),
          azureConfig.getTenantId(), String.valueOf(azureConfig.getKey()), AzureEnvironment.AZURE);

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  protected Azure getAzureClient(AzureConfig azureConfig, String subscriptionId) {
    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureConfig.getClientId(),
          azureConfig.getTenantId(), String.valueOf(azureConfig.getKey()), AzureEnvironment.AZURE);

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withSubscription(subscriptionId);
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  private void handleAzureAuthenticationException(Exception e) {
    logger.error("HandleAzureAuthenticationException: Exception:" + e);

    Throwable e1 = e;
    while (e1.getCause() != null) {
      e1 = e1.getCause();
      if (e1 instanceof AuthenticationException) {
        throw new InvalidCredentialsException("Invalid Azure credentials." + e1.getMessage(), USER);
      }
    }
    throw new InvalidRequestException("Failed to connect to Azure cluster. " + ExceptionUtils.getMessage(e), USER);
  }
}
