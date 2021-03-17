package io.harness.delegate.beans.connector.gcp;

import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GcpCapabilityHelper {
  private static final String GCS_URL = "https://storage.cloud.google.com/";
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      GcpConnectorDTO gcpConnectorDTO, ExpressionEvaluator maskingEvaluator) {
    GcpConnectorCredentialDTO credential = gcpConnectorDTO.getCredential();

    if (credential.getGcpCredentialType() == GcpCredentialType.INHERIT_FROM_DELEGATE) {
      GcpDelegateDetailsDTO config = (GcpDelegateDetailsDTO) credential.getConfig();
      return Collections.singletonList(SelectorCapability.builder().selectors(config.getDelegateSelectors()).build());
    } else if (credential.getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS) {
      return Collections.singletonList(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(GCS_URL, maskingEvaluator));
    } else {
      throw new UnknownEnumTypeException("Gcp Credential Type", String.valueOf(credential.getGcpCredentialType()));
    }
  }
}
