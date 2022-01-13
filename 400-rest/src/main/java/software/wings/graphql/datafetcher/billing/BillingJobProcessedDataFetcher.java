/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.dao.BatchJobScheduledDataDao;
import io.harness.ccm.commons.entities.batch.BatchJobScheduledData;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.graphql.schema.type.aggregation.billing.QLBatchLastProcessedData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BillingJobProcessedDataFetcher
    extends AbstractObjectDataFetcher<QLBatchLastProcessedData, QLNoOpQueryParameters> {
  @Inject private BatchJobScheduledDataDao batchJobScheduledDataDao;
  private static final String BATCH_JOB_TYPE = "CLUSTER_DATA_TO_BIG_QUERY";

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
