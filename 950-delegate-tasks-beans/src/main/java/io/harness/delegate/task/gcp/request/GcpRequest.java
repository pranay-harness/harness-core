package io.harness.delegate.task.gcp.request;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class GcpRequest implements ExecutionCapabilityDemander {
  private static final String GCS_URL = "https://storage.cloud.google.com/";

  public enum RequestType { VALIDATE; }

  private String delegateSelector;
  @NotNull private RequestType requestType;
  // Below 2 are NG specific.
  private List<EncryptedDataDetail> encryptionDetails;
  private GcpManualDetailsDTO gcpManualDetailsDTO;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (isNotBlank(delegateSelector)) {
      return Collections.singletonList(
          SelectorCapability.builder().selectors(ImmutableSet.of(delegateSelector)).build());
    }
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(GCS_URL));
  }
}
