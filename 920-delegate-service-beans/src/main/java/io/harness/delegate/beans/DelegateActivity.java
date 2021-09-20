package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DEL) public enum DelegateActivity { ACTIVE, DISCONNECTED, WAITING_FOR_APPROVAL, OTHER, INACTIVE }
