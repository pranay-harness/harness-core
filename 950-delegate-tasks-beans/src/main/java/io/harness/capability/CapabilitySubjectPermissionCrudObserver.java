package io.harness.capability;

public interface CapabilitySubjectPermissionCrudObserver {
  void onBlockingPermissionsCreated(String accountId, String delegateId);
}