package software.wings.events;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import com.segment.analytics.messages.GroupMessage;
import com.segment.analytics.messages.IdentifyMessage;
import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.account.AccountChangeHandler;
import io.harness.event.handler.impl.segment.SalesforceAccountCheck;
import io.harness.event.handler.impl.segment.SegmentHelper;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.event.AccountEntityEvent;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.instance.stats.InstanceStatService;
import software.wings.service.intfc.security.SecretManagerConfigService;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rktummala on 10/03/19
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AccountChangeHandlerTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private EventListener eventListener;
  @Mock private UserService userService;
  @Mock private MainConfiguration mainConfiguration;
  @Mock private InstanceStatService instanceStatService;
  @Mock private SecretManagerConfigService secretManagerConfigService;
  @Mock private SalesforceAccountCheck salesforceAccountCheck;
  @Mock private FeatureFlagService featureFlagService;
  @Inject private HPersistence hPersistence;
  @Inject private TestUtils eventTestHelper;

  private User user;
  private Account account;
  private AccountChangeHandler accountChangeHandler;
  private SegmentHelper segmentHelper;

  @Before
  public void setup() throws IllegalAccessException {
    SegmentConfig segmentConfig = initializeSegmentConfig();
    MainConfiguration mainConfiguration = mock(MainConfiguration.class);
    FieldUtils.writeField(mainConfiguration, "segmentConfig", segmentConfig, true);
    segmentHelper = spy(new SegmentHelper(mainConfiguration));

    accountChangeHandler = spy(new AccountChangeHandler(eventListener));
    FieldUtils.writeField(accountChangeHandler, "userService", userService, true);
    FieldUtils.writeField(accountChangeHandler, "mainConfiguration", mainConfiguration, true);
    FieldUtils.writeField(accountChangeHandler, "segmentHelper", segmentHelper, true);
    FieldUtils.writeField(accountChangeHandler, "instanceStatService", instanceStatService, true);
    FieldUtils.writeField(accountChangeHandler, "secretManagerConfigService", secretManagerConfigService, true);
    FieldUtils.writeField(accountChangeHandler, "hPersistence", hPersistence, true);
    FieldUtils.writeField(accountChangeHandler, "accountService", accountService, true);
    FieldUtils.writeField(accountChangeHandler, "salesforceAccountCheck", salesforceAccountCheck, true);
    FieldUtils.writeField(accountChangeHandler, "featureFlagService", featureFlagService, true);

    when(accountService.get(anyString())).thenReturn(account);
    when(accountService.getAccountStatus(anyString())).thenReturn(AccountStatus.ACTIVE);
    when(mainConfiguration.getSegmentConfig()).thenReturn(segmentConfig);
    when(accountService.save(any(), eq(false))).thenReturn(account);
    account = eventTestHelper.createAccount();
    user = eventTestHelper.createUser(account);

    Service service = new Service();
    service.setAccountId(account.getUuid());
    wingsPersistence.save(service);
  }

  private SegmentConfig initializeSegmentConfig() {
    SegmentConfig segmentConfig = new SegmentConfig();
    segmentConfig.setEnabled(true);
    segmentConfig.setUrl("dummy_url");
    segmentConfig.setApiKey("api_key");
    return segmentConfig;
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC0_testAccountGroupMessageToSegment() {
    UserThreadLocal.set(user);
    try {
      EventType eventType = EventType.ACCOUNT_ENTITY_CHANGE;
      Map<String, String> properties = new HashMap<>();
      properties.put("ACCOUNT_ID", "ACCOUNT_ID");
      properties.put("EMAIL_ID", "admin@harness.io");

      AccountEntityEvent accountEntityEvent = new AccountEntityEvent(account);
      EventData eventData = EventData.builder().eventInfo(accountEntityEvent).build();
      Event event = Event.builder().eventData(eventData).eventType(eventType).build();

      User newUser = User.Builder.anUser().email("admin@harness.io").accounts(Arrays.asList(account)).build();
      when(userService.getUserByEmail(anyString())).thenReturn(newUser);
      when(featureFlagService.isEnabled(FeatureName.SALESFORCE_INTEGRATION, account.getUuid())).thenReturn(true);
      when(salesforceAccountCheck.isAccountPresentInSalesforce(account)).thenReturn(true);
      when(userService.update(any(User.class))).thenReturn(newUser);
      when(accountService.get(anyString())).thenReturn(account);
      when(accountService.getAccountStatus(anyString())).thenReturn(AccountStatus.ACTIVE);
      when(instanceStatService.percentile(anyString(), any(Instant.class), any(Instant.class), anyDouble()))
          .thenReturn(50.0);
      SecretManagerConfig secretManagerConfig = KmsConfig.builder().build();
      when(secretManagerConfigService.getDefaultSecretManager(anyString())).thenReturn(secretManagerConfig);
      when(userService.getUsersOfAccount(anyString())).thenReturn(Arrays.asList(user));
      accountChangeHandler.handleEvent(event);
      verify(segmentHelper, times(1)).enqueue(any(IdentifyMessage.Builder.class));
      verify(segmentHelper, times(1)).enqueue(any(GroupMessage.Builder.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC1_testAccountGroupMessageToSegment() {
    UserThreadLocal.set(user);
    try {
      EventType eventType = EventType.ACCOUNT_ENTITY_CHANGE;
      Map<String, String> properties = new HashMap<>();
      properties.put("ACCOUNT_ID", "ACCOUNT_ID");
      properties.put("EMAIL_ID", "admin@harness.io");

      AccountEntityEvent accountEntityEvent = new AccountEntityEvent(account);
      EventData eventData = EventData.builder().eventInfo(accountEntityEvent).build();
      Event event = Event.builder().eventData(eventData).eventType(eventType).build();

      User newUser = User.Builder.anUser().email("admin@harness.io").accounts(Arrays.asList(account)).build();
      when(userService.getUserByEmail(anyString())).thenReturn(newUser);
      when(featureFlagService.isEnabled(FeatureName.SALESFORCE_INTEGRATION, account.getUuid())).thenReturn(false);
      when(salesforceAccountCheck.isAccountPresentInSalesforce(account)).thenReturn(true);
      when(userService.update(any(User.class))).thenReturn(newUser);
      when(accountService.get(anyString())).thenReturn(account);
      when(accountService.getAccountStatus(anyString())).thenReturn(AccountStatus.ACTIVE);
      when(instanceStatService.percentile(anyString(), any(Instant.class), any(Instant.class), anyDouble()))
          .thenReturn(50.0);
      SecretManagerConfig secretManagerConfig = KmsConfig.builder().build();
      when(secretManagerConfigService.getDefaultSecretManager(anyString())).thenReturn(secretManagerConfig);
      when(userService.getUsersOfAccount(anyString())).thenReturn(Arrays.asList(user));
      accountChangeHandler.handleEvent(event);
      verify(segmentHelper, times(1)).enqueue(any(IdentifyMessage.Builder.class));
      verify(segmentHelper, times(1)).enqueue(any(GroupMessage.Builder.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC2_testAccountGroupMessageToSegment() {
    UserThreadLocal.set(user);
    try {
      EventType eventType = EventType.ACCOUNT_ENTITY_CHANGE;
      Map<String, String> properties = new HashMap<>();
      properties.put("ACCOUNT_ID", "ACCOUNT_ID");
      properties.put("EMAIL_ID", "admin@harness.io");

      AccountEntityEvent accountEntityEvent = new AccountEntityEvent(account);
      EventData eventData = EventData.builder().eventInfo(accountEntityEvent).build();
      Event event = Event.builder().eventData(eventData).eventType(eventType).build();

      User newUser = User.Builder.anUser().email("admin@harness.io").accounts(Arrays.asList(account)).build();
      when(userService.getUserByEmail(anyString())).thenReturn(newUser);
      when(featureFlagService.isEnabled(FeatureName.SALESFORCE_INTEGRATION, account.getUuid())).thenReturn(true);
      when(salesforceAccountCheck.isAccountPresentInSalesforce(account)).thenReturn(false);
      when(userService.update(any(User.class))).thenReturn(newUser);
      when(accountService.get(anyString())).thenReturn(account);
      when(accountService.getAccountStatus(anyString())).thenReturn(AccountStatus.ACTIVE);
      when(instanceStatService.percentile(anyString(), any(Instant.class), any(Instant.class), anyDouble()))
          .thenReturn(50.0);
      SecretManagerConfig secretManagerConfig = KmsConfig.builder().build();
      when(secretManagerConfigService.getDefaultSecretManager(anyString())).thenReturn(secretManagerConfig);
      when(userService.getUsersOfAccount(anyString())).thenReturn(Arrays.asList(user));
      accountChangeHandler.handleEvent(event);
      verify(segmentHelper, times(1)).enqueue(any(IdentifyMessage.Builder.class));
      verify(segmentHelper, times(1)).enqueue(any(GroupMessage.Builder.class));
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC3_testAccountGroupMessageToSegment() {
    UserThreadLocal.set(user);
    try {
      EventType eventType = EventType.ACCOUNT_ENTITY_CHANGE;
      Map<String, String> properties = new HashMap<>();
      properties.put("ACCOUNT_ID", "ACCOUNT_ID");
      properties.put("EMAIL_ID", "admin@harness.io");

      AccountEntityEvent accountEntityEvent = new AccountEntityEvent(account);
      EventData eventData = EventData.builder().eventInfo(accountEntityEvent).build();
      Event event = Event.builder().eventData(eventData).eventType(eventType).build();

      User newUser = User.Builder.anUser().email("admin@harness.io").accounts(Arrays.asList(account)).build();
      when(userService.getUserByEmail(anyString())).thenReturn(newUser);
      when(featureFlagService.isEnabled(FeatureName.SALESFORCE_INTEGRATION, account.getUuid())).thenReturn(false);
      when(salesforceAccountCheck.isAccountPresentInSalesforce(account)).thenReturn(false);
      when(userService.update(any(User.class))).thenReturn(newUser);
      when(accountService.get(anyString())).thenReturn(account);
      when(accountService.getAccountStatus(anyString())).thenReturn(AccountStatus.ACTIVE);
      when(instanceStatService.percentile(anyString(), any(Instant.class), any(Instant.class), anyDouble()))
          .thenReturn(50.0);
      SecretManagerConfig secretManagerConfig = KmsConfig.builder().build();
      when(secretManagerConfigService.getDefaultSecretManager(anyString())).thenReturn(secretManagerConfig);
      when(userService.getUsersOfAccount(anyString())).thenReturn(Arrays.asList(user));
      accountChangeHandler.handleEvent(event);
      verify(segmentHelper, times(1)).enqueue(any(IdentifyMessage.Builder.class));
      verify(segmentHelper, times(1)).enqueue(any(GroupMessage.Builder.class));
    } finally {
      UserThreadLocal.unset();
    }
  }
}
