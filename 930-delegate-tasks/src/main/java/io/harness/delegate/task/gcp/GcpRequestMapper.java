package io.harness.delegate.task.gcp;

import io.harness.delegate.beans.connector.gcp.GcpValidationParams;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;

public class GcpRequestMapper {
  public GcpRequest toGcpRequest(GcpValidationParams gcpValidationParams) {
    return GcpValidationRequest.builder()
        .gcpManualDetailsDTO((GcpManualDetailsDTO) gcpValidationParams.getGcpConnectorDTO().getCredential().getConfig())
        .delegateSelectors(gcpValidationParams.getDelegateSelectors())
        .encryptedDataDetailList(gcpValidationParams.getEncryptionDetails())
        .build();
  }
}
