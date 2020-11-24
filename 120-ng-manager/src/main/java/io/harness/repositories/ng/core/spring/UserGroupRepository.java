package io.harness.repositories.ng.core.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entities.UserGroup;
import io.harness.repositories.ng.core.custom.UserGroupRepositoryCustom;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface UserGroupRepository extends PagingAndSortingRepository<UserGroup, String>, UserGroupRepositoryCustom {}
