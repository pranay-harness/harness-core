/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PL)
public interface ServiceAccountCustomRepository {
  long deleteMulti(Criteria criteria);
}
