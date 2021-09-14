/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories.gitFileActivitySummary;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary.GitFileActivitySummaryKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))

@OwnedBy(DX)
public class GitFileActivitySummaryRepositoryCustomImpl implements GitFileActivitySummaryRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public DeleteResult deleteByIds(List<String> ids) {
    Query query = query(Criteria.where(GitFileActivitySummaryKeys.uuid).in(ids));
    return mongoTemplate.remove(query, GitFileActivitySummary.class);
  }
}
