package io.harness.event.handler.impl.account;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.segment.analytics.messages.GroupMessage;
import com.segment.analytics.messages.IdentifyMessage;
import io.harness.event.handler.EventHandler;
import io.harness.event.handler.impl.segment.SegmentHelper;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.User;
import software.wings.beans.instance.dashboard.InstanceStatsUtils;
import software.wings.service.impl.AuthServiceImpl.Keys;
import software.wings.service.impl.event.AccountEntityEvent;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.security.SecretManagerConfigService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@NoArgsConstructor
@Singleton
public class AccountChangeHandler implements EventHandler {
  @Inject private SegmentHelper segmentHelper;
  @Inject private InstanceStatService instanceStatService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private UserService userService;

  public AccountChangeHandler(EventListener eventListener) {
    eventListener.registerEventHandler(this, Sets.newHashSet(EventType.ACCOUNT_ENTITY_CHANGE));
  }

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
    DummySystemUser user = new DummySystemUser(accountId, accountName);

    Builder<String, Object> identityTraits = ImmutableMap.builder();

    IdentifyMessage.Builder identity =
        IdentifyMessage.builder()
            .userId(user.getId())
            .traits(identityTraits.put("name", user.getUserName()).put("email", user.getEmail()).build());
    segmentHelper.enqueue(identity);
  }

  private void enqueueGroup(Account account) {
    String accountId = account.getUuid();
    DummySystemUser user = new DummySystemUser(accountId, account.getAccountName());

    double usage = InstanceStatsUtils.actualUsage(accountId, instanceStatService);
    String env = System.getenv("ENV");
    LicenseInfo licenseInfo = account.getLicenseInfo();
    String accountType = licenseInfo != null ? licenseInfo.getAccountType() : null;

    SecretManagerConfig defaultSecretManager = secretManagerConfigService.getDefaultSecretManager(accountId);

    boolean isGlobal = false;
    if (defaultSecretManager != null) {
      isGlobal = Objects.equals(GLOBAL_ACCOUNT_ID, defaultSecretManager.getAccountId());
    }

    List<User> users = userService.getUsersOfAccount(accountId);
    long count = 0;
    if (isNotEmpty(users)) {
      count = users.stream()
                  .filter(userObj -> {
                    if (userObj.getEmail() != null) {
                      return !userObj.getEmail().endsWith(Keys.HARNESS_EMAIL);
                    }
                    return true;
                  })
                  .count();
    }

    Map<String, Object> groupTraits =
        ImmutableMap.<String, Object>builder()
            .put("name", account.getAccountName())
            .put("company_name", account.getCompanyName())
            .put("account_type", isEmpty(accountType) ? "" : accountType)
            .put("cluster", isEmpty(env) ? "" : env)
            .put("usage_service_instances_30d", usage)
            .put("security_secrets_manager_default",
                defaultSecretManager != null ? defaultSecretManager.getEncryptionType().name() : "LOCAL")
            .put("is_global", isGlobal)
            .put("user_count", count)
            .build();
    logger.info("Enqueuing group event. accountId={} traits={}", accountId, groupTraits);
    // group
    segmentHelper.enqueue(GroupMessage.builder(accountId).userId(user.getId()).traits(groupTraits));
  }
}
