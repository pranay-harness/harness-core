package io.harness.accesscontrol.acl.repository;

import io.harness.accesscontrol.acl.models.ACL;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ACLRepositoryCustom {
  List<ACL> findByRole(String scopeIdentifier, String identifier, boolean managed);

  void deleteAll(Criteria criteria);

  List<ACL> findByResourceGroup(String scopeIdentifier, String identifier, boolean managed);
}
