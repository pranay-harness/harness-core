/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.core.failurestrategy;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
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
