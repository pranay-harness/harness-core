package io.harness.ng.core;

public interface NGAccess extends NGAccountAccess, NGProjectAccess, NGOrgAccess {
  String getIdentifier();
}
