package io.harness.ccm.billing.dao;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.ccm.billing.entities.CloudBillingTransferRun;
import io.harness.ccm.billing.entities.CloudBillingTransferRun.CloudBillingTransferRunKeys;
import io.harness.ccm.billing.entities.TransferJobRunState;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@Singleton
public class CloudBillingTransferRunDao {
  @Inject private HPersistence persistence;

  public CloudBillingTransferRun upsert(CloudBillingTransferRun transferRun) {
    Query<CloudBillingTransferRun> query =
        persistence.createQuery(CloudBillingTransferRun.class, excludeValidate)
            .filter(CloudBillingTransferRunKeys.accountId, transferRun.getAccountId())
            .filter(CloudBillingTransferRunKeys.organizationUuid, transferRun.getOrganizationUuid())
            .filter(CloudBillingTransferRunKeys.transferRunResourceName, transferRun.getTransferRunResourceName());

    UpdateOperations<CloudBillingTransferRun> updateOperations =
        persistence.createUpdateOperations(CloudBillingTransferRun.class);
    if (null != transferRun.getState()) {
      updateOperations.set(CloudBillingTransferRunKeys.state, transferRun.getState());
    }

    if (null != transferRun.getBillingDataPipelineRecordId()) {
      updateOperations.set(
          CloudBillingTransferRunKeys.billingDataPipelineRecordId, transferRun.getBillingDataPipelineRecordId());
    }

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public List<CloudBillingTransferRun> list(String accountId, TransferJobRunState state) {
    return persistence.createQuery(CloudBillingTransferRun.class, excludeAuthority)
        .filter(CloudBillingTransferRunKeys.accountId, accountId)
        .filter(CloudBillingTransferRunKeys.state, state)
        .asList();
  }
}
