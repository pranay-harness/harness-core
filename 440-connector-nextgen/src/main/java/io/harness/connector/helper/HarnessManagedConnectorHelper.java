/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.helper;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.awskmsconnector.AwsKmsConnectorDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;

import com.google.inject.Singleton;

@OwnedBy(PL)
@Singleton
public class HarnessManagedConnectorHelper {
  public boolean isHarnessManagedSecretManager(ConnectorInfoDTO connector) {
    if (connector == null) {
      return false;
    }
    switch (connector.getConnectorType()) {
      case GCP_KMS:
        return ((GcpKmsConnectorDTO) connector.getConnectorConfig()).isHarnessManaged();
      case AWS_KMS:
        return ((AwsKmsConnectorDTO) connector.getConnectorConfig()).isHarnessManaged();
      case LOCAL:
        return ((LocalConnectorDTO) connector.getConnectorConfig()).isHarnessManaged();
      default:
        return false;
    }
  }
}
