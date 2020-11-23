package io.harness.delegate.beans.connector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import java.util.List;

/*  Like old gen each connector will define its capability, for some connectors capabilities are simple
 *  whereas others like git also include the EncryptedDataDetails in the capability, to get encrypted data
 *  details we need orgIdentifier and projectIdentifier, but this information is not preset at the ConnectorConfig
 *  level, so we will have to explicitly send it. */
public interface ExecutionCapabilityDemanderWithScope {
  List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
