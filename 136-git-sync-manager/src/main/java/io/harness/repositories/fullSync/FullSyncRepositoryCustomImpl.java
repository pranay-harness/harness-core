/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories.fullSync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class FullSyncRepositoryCustomImpl implements FullSyncRepositoryCustom {
  MongoTemplate mongoTemplate;

  @Override
  public UpdateResult update(Criteria criteria, Update update) {
    Query query = query(criteria);
    return mongoTemplate.updateFirst(query, update, GitFullSyncEntityInfo.class);
  }
}
