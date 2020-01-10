package software.wings.graphql.datafetcher.audit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.event.handler.impl.segment.SegmentHandler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AccountThreadLocal;
import software.wings.security.UserThreadLocal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ChangeContentHelper {
  @Inject private SegmentHandler segmentHandler;

  public void reportAuditTrailExportToSegment() {
    final String accountId = AccountThreadLocal.get();
    List<Account> accounts = UserThreadLocal.get().getAccounts();
    if (EmptyPredicate.isNotEmpty(accounts)) {
      accounts.stream().filter(account -> accountId.equals(account.getUuid())).collect(Collectors.toList());
      if (EmptyPredicate.isNotEmpty(accounts)) {
        Account account = accounts.get(0);
        User user = UserThreadLocal.get().getPublicUser();
        Map<String, String> properties = new HashMap<String, String>() {
          {
            put("userId", String.format("system-%s", accountId));
            put("groupId", accountId);
          }
        };
        Map<String, Boolean> integrations = new HashMap<String, Boolean>() {
          { put(SegmentHandler.Keys.NATERO, Boolean.TRUE); }
        };
        try {
          segmentHandler.reportTrackEvent(account, "Audit Trail exported", user, properties, integrations);
        } catch (Exception e) {
          logger.error(String.format(
              "Exception while reporting track event for Audit Trail Export for accountId: %s, exception: %s",
              accountId, e.getMessage()));
        }
      }
    }
  }
}
