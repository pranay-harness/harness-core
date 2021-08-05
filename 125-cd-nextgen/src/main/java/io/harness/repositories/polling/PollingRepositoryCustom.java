package io.harness.repositories.polling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.bean.PollingInfo;

import com.mongodb.client.result.UpdateResult;
import java.util.List;

@OwnedBy(HarnessTeam.CDC)
public interface PollingRepositoryCustom {
  UpdateResult updatePollingInfo(PollingInfo pollingInfo, String pollDocId);
  PollingDocument addSubscribersToExistingPollingDoc(
      String accountId, String orgId, String projectId, PollingInfo pollingInfo, List<String> signatures);
  PollingDocument deleteDocumentIfOnlySubscriber(
      String accountId, String orgId, String projectId, PollingInfo pollingInfo, List<String> signatures);
  PollingDocument deleteSubscribersFromExistingPollingDoc(
      String accountId, String orgId, String projectId, PollingInfo pollingInfo, List<String> signatures);
  PollingDocument findPollingDocBySignature(String accountId, List<String> signature);
  UpdateResult updateSelectiveEntity(String accountId, String pollDocId, String key, Object value);
}
