package io.harness.connector.helper;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;

import com.google.inject.Singleton;

@Singleton
public class HarnessManagedConnectorHelper {
  public boolean isHarnessManagedSecretManager(ConnectorInfoDTO connector) {
    if (connector == null) {
      return false;
    }
    switch (connector.getConnectorType()) {
      case GCP_KMS:
        return ((GcpKmsConnectorDTO) connector.getConnectorConfig()).isHarnessManaged();
      case LOCAL:
        return ((LocalConnectorDTO) connector.getConnectorConfig()).isHarnessManaged();
      default:
        return false;
    }
  }
}
