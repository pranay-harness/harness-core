package io.harness.ccm.graphql.query.instancebilling;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.dao.BatchJobScheduledDataDao;
import io.harness.ccm.commons.entities.batch.BatchJobScheduledData;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class BillingJobQuery {
  @Inject GraphQLUtils graphQLUtils;
  @Inject BatchJobScheduledDataDao batchJobScheduledDataDao;
  private static final String BATCH_JOB_TYPE = "CLUSTER_DATA_TO_BIG_QUERY";

  @GraphQLQuery(name = "billingJobLastProcessedTime")
  public Long billingJobLastProcessedTime(@GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(accountId, BATCH_JOB_TYPE);
    if (null != batchJobScheduledData) {
      return batchJobScheduledData.getEndAt().toEpochMilli();
    }
    return null;
  }
}
