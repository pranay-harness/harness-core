/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.gitBranches;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitBranch;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitBranchesRepositoryCustomImpl implements GitBranchesRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<GitBranch> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    // Commenting as it might be overkill
    //    query.collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()));
    List<GitBranch> projects = mongoTemplate.find(query, GitBranch.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), GitBranch.class));
  }

  @Override
  public GitBranch update(Query query, Update update) {
    return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), GitBranch.class);
  }

  @Override
  public GitBranch findOne(Criteria criteria) {
    return mongoTemplate.findOne(query(criteria), GitBranch.class);
  }

  @Override
  public DeleteResult delete(Criteria criteria) {
    return mongoTemplate.remove(query(criteria), GitBranch.class);
  }
}
