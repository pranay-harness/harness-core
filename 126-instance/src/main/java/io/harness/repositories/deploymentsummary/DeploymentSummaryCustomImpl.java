/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.deploymentsummary;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.DeploymentSummary.DeploymentSummaryKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Singleton
@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class DeploymentSummaryCustomImpl implements DeploymentSummaryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Optional<DeploymentSummary> fetchNthRecordFromNow(int N, String instanceSyncKey) {
    Criteria criteria = Criteria.where(DeploymentSummaryKeys.instanceSyncKey).is(instanceSyncKey);
    Query query = new Query().addCriteria(criteria);
    query.with(Sort.by(Sort.Direction.DESC, DeploymentSummaryKeys.createdAt));
    query.skip((long) N - 1);
    query.limit(1);
    List<DeploymentSummary> deploymentSummaryList = mongoTemplate.find(query, DeploymentSummary.class);
    if (deploymentSummaryList.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(deploymentSummaryList.get(0));
  }
}
