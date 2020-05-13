package io.harness.ccm;

import static com.hazelcast.util.Preconditions.checkFalse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.IamScopes;
import com.google.api.services.iam.v1.model.Binding;
import com.google.api.services.iam.v1.model.CreateServiceAccountRequest;
import com.google.api.services.iam.v1.model.Policy;
import com.google.api.services.iam.v1.model.ServiceAccount;
import com.google.api.services.iam.v1.model.SetIamPolicyRequest;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

@Slf4j
@Singleton
public class GcpServiceAccountService {
  public static final String CE_GCP_CREDENTIALS_PATH = "CE_GCP_CREDENTIALS_PATH";
  private Iam iamService;

  @Inject private MainConfiguration mainConfiguration;

  private void initService() {
    if (iamService != null) {
      return;
    }
    try {
      ServiceAccountCredentials serviceAccountCredentials = getCredentials(CE_GCP_CREDENTIALS_PATH);
      if (serviceAccountCredentials == null) {
        return;
      }
      GoogleCredential googleCredential = toGoogleCredential(serviceAccountCredentials);
      iamService = new Iam
                       .Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
                           googleCredential)
                       .setApplicationName("service-accounts")
                       .build();
    } catch (IOException | GeneralSecurityException e) {
      logger.error("Unable to initialize service.", e);
    }
  }

  public ServiceAccount create(String serviceAccountId, String displayName) {
    initService();
    ServiceAccount serviceAccount = null;
    try {
      serviceAccount = new ServiceAccount();
      serviceAccount.setDisplayName(displayName);

      CreateServiceAccountRequest request = new CreateServiceAccountRequest();
      request.setAccountId(serviceAccountId);
      request.setServiceAccount(serviceAccount);
      CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();
      String projectId = ceSetUpConfig.getGcpProjectId();
      serviceAccount = iamService.projects().serviceAccounts().create("projects/" + projectId, request).execute();
    } catch (GoogleJsonResponseException e) {
      throw new InvalidRequestException("Google was unable to create a service account.", e);
    } catch (IOException ioe) {
      logger.error("Unable to create service account.", ioe);
      return null;
    }
    return serviceAccount;
  }

  public void setIamPolicy(String serviceAccountEmail) throws IOException {
    initService();
    String resource = format("projects/-/serviceAccounts/%s", serviceAccountEmail);

    SetIamPolicyRequest requestBody = new SetIamPolicyRequest();
    Policy policy = new Policy();
    policy.setEtag("ACAB"); // BwWlUsx6uYk=
    ServiceAccountCredentials serviceAccountCredentials = getCredentials(CE_GCP_CREDENTIALS_PATH);
    String member = serviceAccountCredentials.getClientEmail();
    Binding binding1 = new Binding();
    binding1.setRole("roles/iam.serviceAccountUser");
    binding1.setMembers(singletonList(format("serviceAccount:%s", member)));
    Binding binding2 = new Binding();
    binding2.setRole("roles/iam.serviceAccountTokenCreator");
    binding2.setMembers(singletonList(format("serviceAccount:%s", member)));
    policy.setBindings(Arrays.asList(binding1, binding2));
    requestBody.setPolicy(policy);

    Iam.Projects.ServiceAccounts.SetIamPolicy request =
        iamService.projects().serviceAccounts().setIamPolicy(resource, requestBody);

    request.execute();
  }

  // read the credential path from env variables
  public static ServiceAccountCredentials getCredentials(String googleCredentialPathSystemEnv) {
    String googleCredentialsPath = System.getenv(googleCredentialPathSystemEnv);
    checkFalse(isEmpty(googleCredentialsPath), "Missing environment variable for GCP credentials.");
    File credentialsFile = new File(googleCredentialsPath);
    ServiceAccountCredentials sourceCredentials = null;
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      sourceCredentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      logger.error("Failed to find Google credential file for the CE service account in the specified path.", e);
    } catch (IOException e) {
      logger.error("Failed to get Google credential file for the CE service account.", e);
    }
    return sourceCredentials;
  }

  public static GoogleCredential toGoogleCredential(ServiceAccountCredentials serviceAccountCredentials)
      throws GeneralSecurityException, IOException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    return new GoogleCredential.Builder()
        .setServiceAccountProjectId(serviceAccountCredentials.getProjectId())
        .setServiceAccountId(serviceAccountCredentials.getClientEmail())
        .setServiceAccountPrivateKeyId(serviceAccountCredentials.getPrivateKeyId())
        .setServiceAccountPrivateKey(serviceAccountCredentials.getPrivateKey())
        .setTransport(httpTransport)
        .setJsonFactory(jsonFactory)
        .setServiceAccountScopes(singletonList(IamScopes.CLOUD_PLATFORM))
        .build();
  }
}
