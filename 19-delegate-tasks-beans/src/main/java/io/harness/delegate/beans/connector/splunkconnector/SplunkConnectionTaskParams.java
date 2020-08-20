package io.harness.delegate.beans.connector.splunkconnector;

import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SplunkConnectionTaskParams implements TaskParameters {
  private SplunkConnectorDTO splunkConnectorDTO;
  private List<EncryptedDataDetail> encryptionDetails;
}
