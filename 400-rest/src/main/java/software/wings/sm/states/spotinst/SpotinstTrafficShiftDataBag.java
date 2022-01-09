/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.SpotInstConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
class SpotinstTrafficShiftDataBag {
  private Application app;
  private Environment env;
  private AwsAmiInfrastructureMapping infrastructureMapping;
  private AwsConfig awsConfig;
  private List<EncryptedDataDetail> awsEncryptedDataDetails;
  private SpotInstConfig spotinstConfig;
  private List<EncryptedDataDetail> spotinstEncryptedDataDetails;
}
