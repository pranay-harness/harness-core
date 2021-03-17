package io.harness.accesscontrol.acl.repository;

import io.harness.accesscontrol.Principal;

public interface ACLRepositoryCustom {
  void deleteByPrincipal(Principal principal);
}
