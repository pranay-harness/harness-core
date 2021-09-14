/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.impl;

import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getCredentials;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    String backupBucketName = dataPipelineConfig.getClusterDataGcsBackupBucketName();
    Storage storage =
        StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
    for (String bucket : new String[] {bucketName, backupBucketName}) {
      BlobId blobId = BlobId.of(bucket, objectName);
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
      storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));
      log.info("File " + filePath + " uploaded to bucket " + bucket + " as " + objectName);
    }
  }
}
