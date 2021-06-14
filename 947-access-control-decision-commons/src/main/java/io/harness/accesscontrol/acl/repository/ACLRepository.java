package io.harness.accesscontrol.acl.repository;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(HarnessTeam.PL)
@HarnessRepo
public interface ACLRepository extends PagingAndSortingRepository<ACL, String>, ACLRepositoryCustom {}
