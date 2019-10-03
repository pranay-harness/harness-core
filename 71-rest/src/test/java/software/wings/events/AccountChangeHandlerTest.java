package software.wings.events;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import com.segment.analytics.Analytics;
import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.account.AccountChangeHandler;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.segment.client.SegmentClientBuilder;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.event.AccountEntityEvent;
import software.wings.service.intfc.AccountService;
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
public class AccountChangeHandlerTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private EventListener eventListener;
  @Mock private UserService userService;
  @Mock private SegmentClientBuilder segmentClientBuilder;
  @Mock private MainConfiguration mainConfiguration;
  @Mock private InstanceStatService instanceStatService;
  @Mock private SecretManagerConfigService secretManagerConfigService;
  @Mock private Analytics analytics;
  @Inject private EventTestHelper eventTestHelper;

  private User user;
  private Account account;
  private AccountChangeHandler accountChangeHandler;

  @Before
  public void setup() throws IllegalAccessException {
    SegmentConfig segmentConfig = initializeSegmentConfig();
    accountChangeHandler = new AccountChangeHandler(eventListener);
    FieldUtils.writeField(accountChangeHandler, "userService", userService, true);
    FieldUtils.writeField(accountChangeHandler, "mainConfiguration", mainConfiguration, true);
    FieldUtils.writeField(accountChangeHandler, "segmentClientBuilder", segmentClientBuilder, true);
    FieldUtils.writeField(accountChangeHandler, "instanceStatService", instanceStatService, true);
    FieldUtils.writeField(accountChangeHandler, "secretManagerConfigService", secretManagerConfigService, true);
    when(accountService.get(anyString())).thenReturn(account);
    when(mainConfiguration.getSegmentConfig()).thenReturn(segmentConfig);
    when(accountService.save(any())).thenReturn(account);
    account = eventTestHelper.createAccount();
    user = eventTestHelper.createUser(account);
  }

  private SegmentConfig initializeSegmentConfig() {
    SegmentConfig segmentConfig = new SegmentConfig();
    segmentConfig.setEnabled(true);
    segmentConfig.setUrl("dummy_url");
    segmentConfig.setApiKey("api_key");
    return segmentConfig;
  }

  @Test
  @Category(UnitTests.class)
  public void testAccountGroupMessageToSegment() {
    UserThreadLocal.set(user);
    try {
      EventType eventType = EventType.ACCOUNT_ENTITY_CHANGE;
      Map<String, String> properties = new HashMap<>();
      properties.put("ACCOUNT_ID", "ACCOUNT_ID");
      properties.put("EMAIL_ID", "admin@harness.io");

      AccountEntityEvent accountEntityEvent = new AccountEntityEvent(account);
      EventData eventData = EventData.builder().eventInfo(accountEntityEvent).build();
      Event event = Event.builder().eventData(eventData).eventType(eventType).build();

      User newUser = User.Builder.anUser().withEmail("admin@harness.io").withAccounts(Arrays.asList(account)).build();
      when(userService.getUserByEmail(anyString())).thenReturn(newUser);
      when(userService.update(any(User.class))).thenReturn(newUser);
      when(accountService.get(anyString())).thenReturn(account);
      when(segmentClientBuilder.getInstance()).thenReturn(analytics);
      when(instanceStatService.percentile(anyString(), any(Instant.class), any(Instant.class), anyDouble()))
          .thenReturn(50.0);
      SecretManagerConfig secretManagerConfig = KmsConfig.builder().build();
      when(secretManagerConfigService.getDefaultSecretManager(anyString())).thenReturn(secretManagerConfig);
      when(userService.getUsersOfAccount(anyString())).thenReturn(Arrays.asList(user));
      accountChangeHandler.handleEvent(event);
      verify(analytics, times(2)).enqueue(any());
    } finally {
      UserThreadLocal.unset();
    }
  }
}
