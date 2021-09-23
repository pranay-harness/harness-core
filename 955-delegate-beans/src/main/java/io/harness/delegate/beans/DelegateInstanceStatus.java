package io.harness.delegate.beans;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DEL) public enum DelegateInstanceStatus { ENABLED, WAITING_FOR_APPROVAL, @Deprecated DISABLED, DELETED }
