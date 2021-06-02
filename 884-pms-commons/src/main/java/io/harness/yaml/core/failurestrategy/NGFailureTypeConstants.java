package io.harness.yaml.core.failurestrategy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public interface NGFailureTypeConstants {
  String ALL_ERRORS = "AllErrors";
  String UNKNOWN = "Unknown";
  String AUTHENTICATION_ERROR = "Authentication";
  String CONNECTIVITY_ERROR = "Connectivity";
  String TIMEOUT_ERROR = "Timeout";
  String AUTHORIZATION_ERROR = "Authorization";
  String VERIFICATION_ERROR = "Verification";
  String DELEGATE_PROVISIONING_ERROR = "DelegateProvisioning";
}
