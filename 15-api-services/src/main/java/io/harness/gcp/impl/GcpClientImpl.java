package io.harness.gcp.impl;

import static io.harness.eraro.ErrorCode.INVALID_CLOUD_PROVIDER;
import static io.harness.exception.WingsException.USER;

import io.harness.eraro.ErrorCode;
import io.harness.exception.GoogleClientException;
import io.harness.exception.WingsException;
import io.harness.gcp.client.GcpClient;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
@Singleton
public class GcpClientImpl implements GcpClient {
  @Override
  public void validateDefaultCredentials() {
    getGkeContainerService();
  }

  @Override
  public Container getGkeContainerService() {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = getDefaultGoogleCredentials();
      return new Container.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      throw new GoogleClientException(
          "Failed To Get Google Container Service", ErrorCode.INCORRECT_DEFAULT_GOOGLE_CREDENTIALS, e);
    } catch (IOException e) {
      throw new GoogleClientException(
          "Missing Google Cloud Platform Credentials", ErrorCode.MISSING_DEFAULT_GOOGLE_CREDENTIALS, e);
    }
  }

  @VisibleForTesting
  GoogleCredential getDefaultGoogleCredentials() throws IOException {
    GoogleCredential credential = GoogleCredential.getApplicationDefault();
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(Collections.singletonList(ContainerScopes.CLOUD_PLATFORM));
    }
    return credential;
  }

  @Override
  public Container getGkeContainerService(char[] serviceAccountKey) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
      GoogleCredential credential = getGoogleCredential(serviceAccountKey);
      return new Container.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  GoogleCredential getGoogleCredential(char[] serviceAccountKey) throws IOException {
    GoogleCredential credential =
        GoogleCredential.fromStream(IOUtils.toInputStream(String.valueOf(serviceAccountKey), Charset.defaultCharset()));
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(Collections.singletonList(ContainerScopes.CLOUD_PLATFORM));
    }
    return credential;
  }
}
