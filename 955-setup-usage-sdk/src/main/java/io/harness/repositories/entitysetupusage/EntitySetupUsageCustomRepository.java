/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.entitysetupusage;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(DX)
public interface EntitySetupUsageCustomRepository {
  Page<EntitySetupUsage> findAll(Criteria criteria, Pageable pageable);

  long countAll(Criteria criteria);

  Boolean exists(Criteria criteria);

  long delete(Criteria criteria);
}
