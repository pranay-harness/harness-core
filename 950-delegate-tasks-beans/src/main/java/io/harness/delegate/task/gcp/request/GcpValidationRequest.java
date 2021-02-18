package io.harness.delegate.task.gcp.request;

import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class GcpValidationRequest extends GcpRequest implements TaskParameters {
  @Builder
  public GcpValidationRequest(Set<String> delegateSelectors, List<EncryptedDataDetail> encryptedDataDetailList,
      GcpManualDetailsDTO gcpManualDetailsDTO) {
    super(delegateSelectors, RequestType.VALIDATE, encryptedDataDetailList, gcpManualDetailsDTO);
  }
}
