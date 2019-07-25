package io.harness.event.handler.impl.account;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.segment.analytics.Analytics;
import com.segment.analytics.messages.GroupMessage;
import com.segment.analytics.messages.IdentifyMessage;
import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.segment.client.SegmentClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.instance.dashboard.InstanceStatsUtils;
import software.wings.service.impl.event.AccountEntityEvent;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.util.Map;

@Slf4j
@Singleton
public class AccountChangeHandler implements EventHandler {
  @Inject private SegmentClientBuilder segmentClientBuilder;
  @Inject private InstanceStatService instanceStatService;
  @Inject private MainConfiguration mainConfiguration;

  public AccountChangeHandler(EventListener eventListener) {
    eventListener.registerEventHandler(this, Sets.newHashSet(EventType.ACCOUNT_ENTITY_CHANGE));
  }

  // needed by Guice to allow injection of this class
  public AccountChangeHandler() {}

  @Override
  public void handleEvent(final Event event) {
    EventData eventData = event.getEventData();
    if (null == eventData) {
      logger.error("Unexpected event with null eventData. Type: {}", event.getEventType());
      return;
    }
    AccountEntityEvent accountEntityEvent = (AccountEntityEvent) eventData.getEventInfo();
    if (null == accountEntityEvent || null == accountEntityEvent.getAccount()) {
      logger.error("Unexpected event with null account entity event. Type: {}", event.getEventType());
      return;
    }

    Account account = accountEntityEvent.getAccount();
    if (StringUtils.isEmpty(account.getUuid())) {
      logger.error(
          "No accountId present in account entity event. account={} eventType={}", account, event.getEventType());
      return;
    }

    if (mainConfiguration.getSegmentConfig().isEnabled()) {
      publishAccountEventToSegment(account);
    } else {
      logger.info("Segment is disabled. No events will be sent");
    }
  }

  public void publishAccountEventToSegment(Account account) {
    enqueueIdentity(account.getUuid(), account.getAccountName());
    enqueueGroup(account);
  }

  private void enqueueIdentity(String accountId, String accountName) {
    Analytics analytics = segmentClientBuilder.getInstance();
    DummySystemUser user = new DummySystemUser(accountId, accountName);

    Builder<String, Object> identityTraits = ImmutableMap.builder();

    IdentifyMessage.Builder identity =
        IdentifyMessage.builder()
            .userId(user.getId())
            .traits(identityTraits.put("name", user.getUserName()).put("email", user.getEmail()).build());
    analytics.enqueue(identity);
  }

  private void enqueueGroup(Account account) {
    Analytics analytics = segmentClientBuilder.getInstance();
    String accountId = account.getUuid();
    DummySystemUser user = new DummySystemUser(accountId, account.getAccountName());

    double usage = InstanceStatsUtils.actualUsage(accountId, instanceStatService);
    Map<String, Object> groupTraits = ImmutableMap.<String, Object>builder()
                                          .put("name", account.getAccountName())
                                          .put("company_name", account.getCompanyName())
                                          .put("usage_service_instances_30d", usage)
                                          .build();

    // group
    analytics.enqueue(GroupMessage.builder(accountId).userId(user.getId()).traits(groupTraits));
  }
}
