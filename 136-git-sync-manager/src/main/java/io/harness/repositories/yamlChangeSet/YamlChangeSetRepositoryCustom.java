/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.yamlChangeSet;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
public interface YamlChangeSetRepositoryCustom {
  UpdateResult updateYamlChangeSetStatus(YamlChangeSetStatus status, String yamlChangeSetId);

  UpdateResult updateYamlChangeSetStatusAndCutoffTime(
      YamlChangeSetStatus status, String yamlChangeSetId, long cutOffTime);

  UpdateResult updateYamlChangeSetsStatus(
      YamlChangeSetStatus oldStatus, YamlChangeSetStatus newStatus, String accountId);

  UpdateResult updateYamlChangeSetsToNewStatusWithMessageCodeAndQueuedAtLessThan(
      YamlChangeSetStatus oldStatus, YamlChangeSetStatus newStatus, long cutOffCreatedAt, String message);

  UpdateResult update(Query query, Update update);

  Page<YamlChangeSet> findAll(Criteria criteria, Pageable pageable);

  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);

  List<String> findDistinctAccountIdByStatusIn(List<YamlChangeSetStatus> yamlChangeSetStatuses);
}
