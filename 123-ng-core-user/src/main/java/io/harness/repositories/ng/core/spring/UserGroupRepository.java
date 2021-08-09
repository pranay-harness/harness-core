package io.harness.repositories.ng.core.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.repositories.ng.core.custom.UserGroupRepositoryCustom;

import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(PL)
@HarnessRepo
public interface UserGroupRepository extends PagingAndSortingRepository<UserGroup, String>, UserGroupRepositoryCustom {}
