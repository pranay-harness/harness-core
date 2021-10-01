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

  @Override
  public Optional<DeploymentSummary> getWithInfraMappingId(String infraMappingId) {
    Criteria criteria = Criteria.where(DeploymentSummaryKeys.infrastructureMappingId).is(infraMappingId);
    Query query = new Query().addCriteria(criteria);
    query.with(Sort.by(Sort.Direction.DESC, DeploymentSummaryKeys.createdAt));
    query.limit(1);
    List<DeploymentSummary> deploymentSummaryList = mongoTemplate.find(query, DeploymentSummary.class);
    if (deploymentSummaryList.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(deploymentSummaryList.get(0));
  }
}
