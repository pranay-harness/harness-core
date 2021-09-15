/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.gcb;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GcpConfig;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.GcbTrigger;
import software.wings.helpers.ext.gcb.models.RepoSource;

import java.util.List;

@OwnedBy(CDC)
public interface GcbService {
  BuildOperationDetails createBuild(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, GcbBuildDetails buildParams);

  GcbBuildDetails getBuild(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String buildId);

  BuildOperationDetails runTrigger(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String triggerId, RepoSource repoSource);

  String fetchBuildLogs(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String fileName);

  List<GcbTrigger> getAllTriggers(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails);

  GcbBuildDetails cancelBuild(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String buildId);

  String getProjectId(GcpConfig gcpConfig);
}
