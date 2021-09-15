/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.approval.step.entities.ApprovalInstance;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@OwnedBy(HarnessTeam.CDC)
@HarnessRepo
@Transactional
public interface ApprovalInstanceRepository
    extends CrudRepository<ApprovalInstance, String>, ApprovalInstanceCustomRepository {}
