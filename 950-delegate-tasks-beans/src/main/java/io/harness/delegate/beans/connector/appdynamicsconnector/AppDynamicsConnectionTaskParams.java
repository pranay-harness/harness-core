package io.harness.delegate.beans.connector.appdynamicsconnector;

import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppDynamicsConnectionTaskParams implements TaskParameters {
  private AppDynamicsConnectorDTO appDynamicsConnectorDTO;
  private List<EncryptedDataDetail> encryptionDetails;
}
