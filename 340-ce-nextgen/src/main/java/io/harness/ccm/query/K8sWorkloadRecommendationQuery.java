package io.harness.ccm.query;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.utils.GraphQLUtils;
import io.harness.persistence.HPersistence;

import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation.K8sWorkloadRecommendationKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

@Slf4j
@Singleton
@OwnedBy(CE)
public class K8sWorkloadRecommendationQuery {
  @Inject private HPersistence hPersistence;
  @Inject private GraphQLUtils graphQLUtils;

  @GraphQLQuery(name = "workloadRecommendation", description = "K8s Workload Recommendation")
  public List<K8sWorkloadRecommendation> getK8sWorkloadRecommendation(@Nullable String clusterId,
      @Nullable String namespace, @Nullable String workloadName, @Nullable String workloadType,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountIdentifier = graphQLUtils.getAccountIdentifier(env);
    Query<K8sWorkloadRecommendation> query = hPersistence.createQuery(K8sWorkloadRecommendation.class)
                                                 .filter(K8sWorkloadRecommendationKeys.accountId, accountIdentifier);

    if (clusterId != null) {
      query.filter(K8sWorkloadRecommendationKeys.clusterId, clusterId);
    }
    if (namespace != null) {
      query.filter(K8sWorkloadRecommendationKeys.namespace, namespace);
    }
    if (workloadName != null) {
      query.filter(K8sWorkloadRecommendationKeys.workloadName, workloadName);
    }
    if (workloadType != null) {
      query.filter(K8sWorkloadRecommendationKeys.workloadType, workloadType);
    }

    query.field(K8sWorkloadRecommendationKeys.validRecommendation)
        .equal(Boolean.TRUE)
        .field(K8sWorkloadRecommendationKeys.lastDayCostAvailable)
        .equal(Boolean.TRUE)
        .field(K8sWorkloadRecommendationKeys.numDays)
        .greaterThanOrEq(1)
        .field(K8sWorkloadRecommendationKeys.lastReceivedUtilDataAt)
        .greaterThanOrEq(Instant.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(2)))
        .order(Sort.descending(K8sWorkloadRecommendationKeys.estimatedSavings));

    return query.asList();
  }
}
