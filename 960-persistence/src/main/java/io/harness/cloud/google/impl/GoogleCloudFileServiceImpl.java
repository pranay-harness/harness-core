package io.harness.cloud.google.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cloud.google.DownloadResult;
import io.harness.cloud.google.GoogleCloudFileService;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.File;

@OwnedBy(HarnessTeam.GTM)
public class GoogleCloudFileServiceImpl implements GoogleCloudFileService {
  private Storage storage;
  private static final String GOOGLE_APPLICATION_CREDENTIALS_PATH = "GOOGLE_APPLICATION_CREDENTIALS";

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
    String googleCredentialsPath = System.getenv(GOOGLE_APPLICATION_CREDENTIALS_PATH);
    if (isEmpty(googleCredentialsPath) || !new File(googleCredentialsPath).exists()) {
      throw new IllegalArgumentException("Invalid credentials found at " + googleCredentialsPath);
    }

    if (isEmpty(projectId)) {
      storage = StorageOptions.getDefaultInstance().getService();
    } else {
      storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }
  }
}
