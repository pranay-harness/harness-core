/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */
package io.harness.repositories.remediation_tracker;

import static io.harness.annotations.dev.HarnessTeam.SSCA;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ssca.entities.remediation_tracker.RemediationTrackerEntity;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(SSCA)
public interface RemediationTrackerRepository
    extends CrudRepository<RemediationTrackerEntity, String>, RemediationTrackerRepositoryCustom {
  Optional<RemediationTrackerEntity> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndUuid(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String uuid);
}
