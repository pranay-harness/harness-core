/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.activityhistory;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.activityhistory.entity.NGActivity;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface NGActivityRepository
    extends PagingAndSortingRepository<NGActivity, String>, NGActivityCustomRepository {
  long deleteByReferredEntityFQNAndReferredEntityType(String referredEntityFQN, String referredEntityType);
}
