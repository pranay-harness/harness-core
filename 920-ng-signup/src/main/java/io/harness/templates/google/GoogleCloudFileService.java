package io.harness.templates.google;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.GTM)
public interface GoogleCloudFileService {
  DownloadResult downloadFile(String objectName, String bucketName);
  void initialize(String projectId);
}
