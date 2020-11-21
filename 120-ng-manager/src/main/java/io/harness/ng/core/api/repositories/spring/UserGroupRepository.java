package io.harness.ng.core.api.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.api.repositories.custom.UserGroupRepositoryCustom;
import io.harness.ng.core.entities.UserGroup;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface UserGroupRepository extends PagingAndSortingRepository<UserGroup, String>, UserGroupRepositoryCustom {}
