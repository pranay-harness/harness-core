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
  PollingDocument deleteDocumentIfOnlySubscriber(String accountId, String orgId, String projectId,
      PollingType pollingType, PollingInfo pollingInfo, List<String> signatures);
  PollingDocument deleteSubscribersFromExistingPollingDoc(String accountId, String orgId, String projectId,
      PollingType pollingType, PollingInfo pollingInfo, List<String> signatures);
  UpdateResult updateSelectiveEntity(String accountId, String pollDocId, String key, Object value);
}
