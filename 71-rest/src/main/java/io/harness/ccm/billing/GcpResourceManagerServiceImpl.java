package io.harness.ccm.billing;

import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.CE_GCP_CREDENTIALS_PATH;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getCredentials;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.toGoogleCredential;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.model.GetIamPolicyRequest;
import com.google.api.services.cloudresourcemanager.model.Policy;
import com.google.api.services.cloudresourcemanager.model.SetIamPolicyRequest;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
@Singleton
public class GcpResourceManagerServiceImpl implements GcpResourceManagerService {
  private static CloudResourceManager createCloudResourceManagerService() throws GeneralSecurityException, IOException {
    ServiceAccountCredentials serviceAccountCredentials = getCredentials(CE_GCP_CREDENTIALS_PATH);
    if (serviceAccountCredentials == null) {
      return null;
    }
    GoogleCredential googleCredential = toGoogleCredential(serviceAccountCredentials);

    return new CloudResourceManager
        .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), googleCredential)
        .setApplicationName("service-accounts")
        .build();
  }

  // Sets a project's policy
  @Override
  public void setPolicy(String projectId, Policy policy) {
    CloudResourceManager service = null;
    try {
      service = createCloudResourceManagerService();
    } catch (IOException | GeneralSecurityException e) {
      logger.error("Unable to initialize service: ", e);
      return;
    }

    try {
      SetIamPolicyRequest request = new SetIamPolicyRequest();
      request.setPolicy(policy);
      Policy response = service.projects().setIamPolicy(projectId, request).execute();
      logger.info("Policy set: " + response.toString());
    } catch (IOException e) {
      logger.error("Unable to set policy: ", e);
    }
  }

  @Override
  public Policy getIamPolicy(String projectId) {
    CloudResourceManager service = null;
    try {
      service = createCloudResourceManagerService();
    } catch (IOException | GeneralSecurityException e) {
      logger.error("Unable to initialize service: \n", e);
      return null;
    }

    Policy policy;
    try {
      GetIamPolicyRequest request = new GetIamPolicyRequest();
      policy = service.projects().getIamPolicy(projectId, request).execute();
      logger.info("Policy retrieved: " + policy.toString());
      return policy;
    } catch (IOException e) {
      logger.error("Unable to get policy: \n", e);
      return null;
    }
  }
}
