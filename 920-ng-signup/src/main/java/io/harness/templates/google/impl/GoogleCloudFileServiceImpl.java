/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.templates.google.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.templates.google.DownloadResult;
import io.harness.templates.google.GoogleCloudFileService;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@OwnedBy(HarnessTeam.GTM)
public class GoogleCloudFileServiceImpl implements GoogleCloudFileService {
  private Storage storage;
  private static final String GOOGLE_APPLICATION_TEMPLATE_DATA_CREDENTIALS_PATH =
      "GOOGLE_APPLICATION_TEMPLATE_DATA_CREDENTIALS";

  @Override
  public DownloadResult downloadFile(String objectName, String bucketName) {
    if (storage == null) {
      throw new IllegalStateException(
          "Google cloud storage hasn't been initialized yet. Please call initialize firstly");
    }
    Blob blob = storage.get(BlobId.of(bucketName, objectName));
    if (blob != null) {
      return DownloadResult.builder()
          .fileName(blob.getName())
          .updateTime(blob.getUpdateTime())
          .content(blob.getContent())
          .build();
    }
    return DownloadResult.NULL_RESULT;
  }

  @Override
  public void initialize(String projectId) {
    String googleCredentialsPath = System.getenv(GOOGLE_APPLICATION_TEMPLATE_DATA_CREDENTIALS_PATH);
    if (isEmpty(googleCredentialsPath) || !new File(googleCredentialsPath).exists()) {
      throw new IllegalArgumentException("Invalid credentials found at " + googleCredentialsPath);
    }

    if (isEmpty(projectId)) {
      throw new IllegalArgumentException("Invalid projectId");
    }

    try (FileInputStream credentialStream = new FileInputStream(googleCredentialsPath)) {
      GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream);
      storage = StorageOptions.newBuilder().setCredentials(credentials).setProjectId(projectId).build().getService();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to initialize gcp storage", e);
    }
  }
}
