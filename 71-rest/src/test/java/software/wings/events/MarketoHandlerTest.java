package software.wings.events;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.event.handler.impl.MarketoHandler;
import io.harness.event.handler.impl.MarketoHelper;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rktummala on 12/06/18
 */
public class MarketoHandlerTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private EventListener eventListener;
  @Mock private UserService userService;
  @Inject private EventTestHelper eventTestHelper;
  @Inject private MarketoHelper marketoHelper;

  private User user;
  private Account account;

  private MarketoHandler marketoHandler;

  @Before
  public void setup() {
    MarketoConfig marketoConfig = eventTestHelper.initializeMarketoConfig();
    marketoHandler = new MarketoHandler(marketoConfig, eventListener);
    setInternalState(marketoHandler, "userService", userService);
    setInternalState(marketoHandler, "marketoHelper", marketoHelper);
    setInternalState(marketoHelper, "marketoConfig", marketoConfig);
    setInternalState(marketoHelper, "accountService", accountService);
    setInternalState(marketoHelper, "userService", userService);
    when(accountService.get(anyString())).thenReturn(account);
    when(accountService.save(any())).thenReturn(account);
    account = eventTestHelper.createAccount();
    user = eventTestHelper.createUser(account);
  }

  @Test
  @Category(UnitTests.class)
  @Ignore
  //  @Repeat(times = 5, successes = 1)
  public void testCreateLeadAndTriggerCampaign() {
    UserThreadLocal.set(user);
    try {
      EventType eventType = EventType.COMPLETE_USER_REGISTRATION;
      Map<String, String> properties = new HashMap<>();
      properties.put("ACCOUNT_ID", "ACCOUNT_ID");
      properties.put("EMAIL_ID", "admin@harness.io");

      EventData eventData = EventData.builder().properties(properties).build();
      Event event = Event.builder().eventData(eventData).eventType(eventType).build();
      User newUser = User.Builder.anUser().withEmail("admin@harness.io").withAccounts(Arrays.asList(account)).build();
      when(userService.getUserByEmail(anyString())).thenReturn(newUser);
      when(userService.update(any(User.class))).thenReturn(newUser);
      when(accountService.get(anyString())).thenReturn(account);

      marketoHandler.handleEvent(event);
      verify(userService, times(1)).update(any(User.class));
    } finally {
      UserThreadLocal.unset();
    }
  }
}
