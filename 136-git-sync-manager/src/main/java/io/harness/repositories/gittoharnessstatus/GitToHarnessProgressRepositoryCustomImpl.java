/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.gittoharnessstatus;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitToHarnessProgress;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitToHarnessProgressRepositoryCustomImpl implements GitToHarnessProgressRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public GitToHarnessProgress findAndModify(Criteria criteria, Update update) {
    return mongoTemplate.findAndModify(
        query(criteria), update, FindAndModifyOptions.options().returnNew(true), GitToHarnessProgress.class);
  }
}
