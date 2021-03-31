package io.harness.delegate.task.gcp.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
<<<<<<< HEAD
@OwnedBy(CDP)
=======
>>>>>>> [CDNG-5543)]: removed delete permission
public abstract class GcpRequest extends ConnectorTaskParams implements ExecutionCapabilityDemander {
  // Below 2 are NG specific.
  private List<EncryptedDataDetail> encryptionDetails;
  private GcpManualDetailsDTO gcpManualDetailsDTO;
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (isNotEmpty(delegateSelectors)) {
      return singletonList(SelectorCapability.builder().selectors(delegateSelectors).build());
    }
    return emptyList();
  }
}