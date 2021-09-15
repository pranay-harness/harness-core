/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories.gitBranches;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitBranch;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(DX)
public interface GitBranchesRepository
    extends PagingAndSortingRepository<GitBranch, String>, GitBranchesRepositoryCustom {}
