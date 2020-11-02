package io.harness.batch.processing.service.impl;

import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getCredentials;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@Slf4j
public class GoogleCloudStorageServiceImpl {
  private static final String GOOGLE_CREDENTIALS_PATH = "GOOGLE_CREDENTIALS_PATH";
  private BatchMainConfig config;

  @Autowired
  public GoogleCloudStorageServiceImpl(BatchMainConfig config) {
    this.config = config;
  }

  public void uploadObject(String objectName, String filePath) throws IOException {
    BillingDataPipelineConfig dataPipelineConfig = config.getBillingDataPipelineConfig();
    ServiceAccountCredentials credentials = getCredentials(GOOGLE_CREDENTIALS_PATH);

    String projectId = dataPipelineConfig.getGcpProjectId();
    String bucketName = dataPipelineConfig.getClusterDataGcsBucketName();
    Storage storage =
        StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
    BlobId blobId = BlobId.of(bucketName, objectName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));
    log.info("File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);
  }
}
