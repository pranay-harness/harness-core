/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.entitysetupusage;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
@OwnedBy(DX)
public interface EntitySetupUsageRepository
    extends PagingAndSortingRepository<EntitySetupUsage, String>, EntitySetupUsageCustomRepository {
  long deleteByReferredEntityFQNAndReferredEntityTypeAndReferredByEntityFQNAndReferredByEntityTypeAndAccountIdentifier(
      String referredEntityFQN, String referredEntityType, String referredByEntityFQN, String referredByEntityType,
      String accountIdentifier);

  long deleteByReferredByEntityFQNAndReferredByEntityTypeAndAccountIdentifier(
      String referredByEntityFQN, String referredByEntityType, String accountIdentifier);

  boolean existsByReferredEntityFQNAndReferredEntityTypeAndAccountIdentifier(
      String referredEntityFQN, String referredEntityType, String accountIdentifier);

  long deleteAllByAccountIdentifierAndReferredByEntityFQNAndReferredByEntityTypeAndReferredEntityType(
      String accountIdentifier, String referredByEntityFQN, String referredByEntityType, String referredEntityType);

  long deleteAllByReferredByEntityType(String referredByEntityType);
}
