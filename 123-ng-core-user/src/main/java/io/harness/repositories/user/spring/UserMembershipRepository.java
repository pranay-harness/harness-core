/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.user.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.repositories.user.custom.UserMembershipRepositoryCustom;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface UserMembershipRepository
    extends PagingAndSortingRepository<UserMembership, String>, UserMembershipRepositoryCustom {}
