package software.wings.service.impl;

import com.google.inject.Singleton;

import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.rest.LogLevel;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;

@Singleton
public class AzureHelperService {
  public void validateAzureAccountCredential(String clientId, String tenantId, String key) {
    try {
      ApplicationTokenCredentials credentials =
          new ApplicationTokenCredentials(clientId, tenantId, key, AzureEnvironment.AZURE);

      Azure azure = Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();

      azure.getCurrentSubscription().listLocations();

    } catch (Exception e) {
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
}
