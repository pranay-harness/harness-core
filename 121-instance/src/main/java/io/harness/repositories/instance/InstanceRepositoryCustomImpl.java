package io.harness.repositories.instance;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instance.Instance;
import io.harness.entities.instance.Instance.InstanceKeys;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.constants.InstanceSyncConstants;

import com.google.inject.Inject;
import groovy.util.logging.Slf4j;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InstanceRepositoryCustomImpl implements InstanceRepositoryCustom {
  private MongoTemplate mongoTemplate;

  @Override
  public List<Instance> getActiveInstancesByAccount(String accountIdentifier, long timestamp) {
    Criteria criteria = Criteria.where(InstanceKeys.accountIdentifier).is(accountIdentifier);
    if (timestamp > 0) {
      criteria = criteria.andOperator(
          Criteria.where(InstanceKeys.createdAt)
              .lte(timestamp)
              .andOperator(Criteria.where(InstanceKeys.isDeleted)
                               .is(false)
                               .orOperator(Criteria.where(InstanceKeys.deletedAt).gte(timestamp))));
    } else {
      criteria = criteria.andOperator(Criteria.where(InstanceKeys.isDeleted).is(false));
    }

    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.find(query, Instance.class);
  }

  @Override
  public List<Instance> getInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId) {
    // TODO
    return null;
  }

  /*
    Returns instances that are active for all services at a given timestamp for specified accountIdentifier,
    projectIdentifier and orgIdentifier
  */
  @Override
  public List<Instance> getActiveInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs) {
    Criteria criteria =
        getCriteriaForActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, timestampInMs);

    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.find(query, Instance.class);
  }

  /*
    Returns instances that are active at a given timestamp for specified accountIdentifier, projectIdentifier,
    orgIdentifier and serviceId
  */
  @Override
  public List<Instance> getActiveInstancesByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs) {
    Criteria criteria =
        getCriteriaForActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, timestampInMs)
            .where(InstanceKeys.serviceId)
            .is(serviceId);
    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.find(query, Instance.class);
  }

  @Override
  public AggregationResults<EnvBuildInstanceCount> getEnvBuildInstanceCountByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs) {
    Criteria criteria =
        getCriteriaForActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, timestampInMs)
            .where(InstanceKeys.serviceId)
            .is(serviceId);

    MatchOperation matchStage = Aggregation.match(criteria);
    GroupOperation groupEnvId =
        group(InstanceKeys.envId, InstanceKeys.envName, InstanceSyncConstants.primaryArtifactTag)
            .count()
            .as(InstanceSyncConstants.count);
    return mongoTemplate.aggregate(newAggregation(matchStage, groupEnvId), Instance.class, EnvBuildInstanceCount.class);
  }

  /*
    Create criteria to query for all active service instances for given accountIdentifier, orgIdentifier,
    projectIdentifier
  */
  private Criteria getCriteriaForActiveInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs) {
    Criteria baseCriteria = Criteria.where(InstanceKeys.accountIdentifier)
                                .is(accountIdentifier)
                                .where(InstanceKeys.orgIdentifier)
                                .is(orgIdentifier)
                                .where(InstanceKeys.projectIdentifier)
                                .is(projectIdentifier);

    Criteria filterCreatedAt = Criteria.where(InstanceKeys.createdAt).lte(timestampInMs);
    Criteria filterDeletedAt = Criteria.where(InstanceKeys.deletedAt).gte(timestampInMs);
    Criteria filterNotDeleted = Criteria.where(InstanceKeys.isDeleted).is(false);

    return baseCriteria.andOperator(filterCreatedAt.orOperator(filterNotDeleted, filterDeletedAt));
  }
}
