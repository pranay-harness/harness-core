package software.wings.graphql.datafetcher.audit;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.impl.segment.SegmentHandler;
import io.harness.event.handler.impl.segment.SegmentHelper;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.Principal;
import software.wings.resources.graphql.TriggeredByType;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.util.HashMap;
import java.util.Map;

@Singleton
@Slf4j
public class ChangeContentHelper {
  @Inject private SegmentHandler segmentHandler;
  @Inject private AccountService accountService;
  @Inject private UserService userService;
  @Inject private SegmentHelper segmentHelper;
  private static final String segmentEvent = "Audit Trail exported";

  public void reportAuditTrailExportToSegment(String accountId, Principal triggeredBy) {
    final Account account = accountService.get(accountId);
    if (account == null || triggeredBy == null) {
      return;
    }
    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("userId", String.format("system-%s", accountId));
        put("groupId", accountId);
      }
    };
    Map<String, Boolean> integrations = new HashMap<String, Boolean>() {
      { put(SegmentHandler.Keys.NATERO, Boolean.TRUE); }
    };

    if (triggeredBy.getTriggeredById() != null) {
      if (triggeredBy.getTriggeredByType() == TriggeredByType.USER) {
        User user = userService.get(triggeredBy.getTriggeredById());
        try {
          segmentHandler.reportTrackEvent(account, segmentEvent, user, properties, integrations);
        } catch (Exception e) {
          logger.error(String.format(
              "Exception while reporting track event for Audit Trail Export for accountId: %s, exception: %s",
              accountId, e.getMessage()));
        }
      } else if (triggeredBy.getTriggeredByType() == TriggeredByType.API_KEY) {
        segmentHelper.reportTrackEvent(String.format("system-%s", accountId), segmentEvent, properties, integrations);
      }
    }
  }
}
