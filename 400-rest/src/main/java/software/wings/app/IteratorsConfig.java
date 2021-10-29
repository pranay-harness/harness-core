package software.wings.app;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class IteratorsConfig {
  IteratorConfig resourceConstraintBackupIteratorConfig;
  IteratorConfig delegateCapabilitiesRecordIteratorConfig;
  IteratorConfig settingAttributeValidateConnectivityIteratorConfig;
  IteratorConfig instanceSyncIteratorConfig;
  IteratorConfig artifactCollectionIteratorConfig;
  IteratorConfig eventDeliveryIteratorConfig;
  IteratorConfig workflowExecutionMonitorIteratorConfig;
  IteratorConfig blockingCapabilityPermissionsRecordHandlerIteratorConfig;
  IteratorConfig vaultSecretManagerRenewalIteratorConfig;
  IteratorConfig delegateTaskExpiryCheckIteratorConfig;
  IteratorConfig perpetualTaskRebalanceIteratorConfig;
  IteratorConfig perpetualTaskAssignmentIteratorConfig;
}
