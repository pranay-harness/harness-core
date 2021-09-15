/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.helpers.ext.ami;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 12/14/17.
 */
@OwnedBy(CDC)
public interface AmiService {
  /**
   * Get AMI Images
   * @param awsConfig
   * @param encryptionDetails
   * @param region
   * @param tags
   * @param maxNumberOfBuilds
   * @param platform
   * @return
   */
  List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      Map<String, List<String>> tags, Map<String, String> filterMap, int maxNumberOfBuilds);
}
