/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.gcp.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eraro.ErrorCode.INVALID_CLOUD_PROVIDER;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.gcp.client.GcpClient;

import com.fasterxml.jackson.core.JsonParseException;
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
@OwnedBy(CDP)
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
      throw NestedExceptionUtils.hintWithExplanationException(
          "Provide a valid Service Account Key", "Invalid Service Account Key", e);
    } catch (IOException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Set the GOOGLE_APPLICATION_CREDENTIALS environment variable or make sure that the delegate is running on Google Environments",
          "Missing Google Cloud Platform Credentials", e);
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
      log.error("Security registeredException getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google container service", e);
      throw convertIntoHintException(e);
    }
  }

  private RuntimeException convertIntoHintException(Exception ex) {
    if (ex instanceof JsonParseException) {
      return NestedExceptionUtils.hintWithExplanationException(
          "Provide a valid json service account key", "Service account Key is not a valid json", ex);
    } else if (ex instanceof IOException) {
      return NestedExceptionUtils.hintWithExplanationException("Provide a valid service account key provided by Google",
          "Invalid Spec for the Service Account Key File", ex);
    }
    return new InvalidRequestException(ex.getMessage(), ex);
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
