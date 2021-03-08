package software.wings.service.impl.trigger;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SUJAY;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.UnauthorizedException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AuthService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TriggerAuthHandlerTest extends WingsBaseTest {
  @Mock private AuthService authService;
  @InjectMocks @Inject private TriggerAuthHandler triggerAuthHandler;

  User user;

  @Before
  public void setUp() {
    Map<String, AppPermissionSummaryForUI> appPermissionMap = new HashMap<>();
    appPermissionMap.put(APP_ID, AppPermissionSummaryForUI.builder().build());
    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder().appPermissionMap(appPermissionMap).build();
    UserRequestContext userRequestContext =
        UserRequestContext.builder().userPermissionInfo(userPermissionInfo).accountId(ACCOUNT_ID).build();
    user = User.Builder.anUser().uuid(generateUuid()).userRequestContext(userRequestContext).build();
  }

  @Test
  @Owner(developers = SUJAY)
  @Category(UnitTests.class)
  public void testAuthorizeAppAccessWithId() {
    UserThreadLocal.set(user);
    triggerAuthHandler.authorizeAppAccess(singletonList(APP_ID), ACCOUNT_ID);
    verify(authService, never()).getUserPermissionInfo(ACCOUNT_ID, user, false);
    UserThreadLocal.unset();
  }

  @Test
  @Owner(developers = SUJAY)
  @Category(UnitTests.class)
  public void testAuthorizeAppAccessWithIdNull() {
    UserThreadLocal.set(user);
    assertThatThrownBy(() -> { triggerAuthHandler.authorizeAppAccess(singletonList(""), ACCOUNT_ID); })
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage("User Not authorized");
    UserThreadLocal.unset();
  }

  @Test
  @Owner(developers = SUJAY)
  @Category(UnitTests.class)
  public void testAuthorizeAppAccessWithUserRequestContextNull() {
    UserThreadLocal.set(user);
    UserPermissionInfo userPermissionInfo = user.getUserRequestContext().getUserPermissionInfo();
    user.setUserRequestContext(null);
    when(authService.getUserPermissionInfo(ACCOUNT_ID, user, false)).thenReturn(userPermissionInfo);
    triggerAuthHandler.authorizeAppAccess(singletonList(APP_ID), ACCOUNT_ID);
    verify(authService).getUserPermissionInfo(ACCOUNT_ID, user, false);
    UserThreadLocal.unset();
  }
}
