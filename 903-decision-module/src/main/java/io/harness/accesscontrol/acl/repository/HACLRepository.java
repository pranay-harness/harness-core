package io.harness.accesscontrol.acl.repository;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.HACL;
import io.harness.annotation.HarnessRepo;

import java.util.List;

@HarnessRepo
public interface HACLRepository extends ACLBaseRepository<HACL>, ACLRepositoryCustom {
  List<ACL> getByAclQueryStringIn(List<String> aclQueries);
}
