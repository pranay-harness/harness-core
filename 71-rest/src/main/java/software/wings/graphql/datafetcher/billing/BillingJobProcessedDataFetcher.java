package software.wings.graphql.datafetcher.billing;

import io.harness.ccm.cluster.dao.BatchJobScheduledDataDao;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.graphql.schema.type.aggregation.billing.QLBatchLastProcessedData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BillingJobProcessedDataFetcher
    extends AbstractObjectDataFetcher<QLBatchLastProcessedData, QLNoOpQueryParameters> {
  @Inject private BatchJobScheduledDataDao batchJobScheduledDataDao;
  private static final String BATCH_JOB_TYPE = "UNALLOCATED_BILLING_HOURLY";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLBatchLastProcessedData fetch(QLNoOpQueryParameters parameters, String accountId) {
    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(accountId, BATCH_JOB_TYPE);
    if (null != batchJobScheduledData) {
      return QLBatchLastProcessedData.builder()
          .lastProcessedTime(batchJobScheduledData.getEndAt().toEpochMilli())
          .build();
    }
    return QLBatchLastProcessedData.builder().build();
  }
}
