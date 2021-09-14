/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingInfo;
import io.harness.polling.bean.PollingType;

import com.mongodb.client.result.UpdateResult;
import java.util.List;

@OwnedBy(HarnessTeam.CDC)
public interface PollingRepositoryCustom {
  PollingDocument addSubscribersToExistingPollingDoc(String accountId, String orgId, String projectId,
      PollingType pollingType, PollingInfo pollingInfo, List<String> signatures);
  PollingDocument addSubscribersToExistingPollingDoc(String accountId, String uuId, List<String> signatures);
  PollingDocument deleteDocumentIfOnlySubscriber(String accountId, String orgId, String projectId,
      PollingType pollingType, PollingInfo pollingInfo, List<String> signatures);
  PollingDocument removeDocumentIfOnlySubscriber(String accountId, String pollingDocId, List<String> signatures);
  PollingDocument deleteSubscribersFromExistingPollingDoc(String accountId, String orgId, String projectId,
      PollingType pollingType, PollingInfo pollingInfo, List<String> signatures);
  PollingDocument removeSubscribersFromExistingPollingDoc(
      String accountId, String pollingDocId, List<String> signatures);
  UpdateResult updateSelectiveEntity(String accountId, String pollDocId, String key, Object value);
  PollingDocument findByUuidAndAccountIdAndSignature(String pollingDocId, String accountId, List<String> signature);
}
