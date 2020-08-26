package software.wings.security;

import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.AUDIT_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.CE_ADMIN;
import static software.wings.security.PermissionAttribute.PermissionType.CE_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ALERT_NOTIFICATION_RULES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATION_STACKS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_AUTHENTICATION_SETTINGS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONFIG_AS_CODE;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONNECTORS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATE_PROFILES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DEPLOYMENT_FREEZES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_IP_WHITELIST;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_PIPELINE_GOVERNANCE_STANDARDS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRETS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRET_MANAGERS;
import static software.wings.security.PermissionAttribute.PermissionType.TAG_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_READ;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.AccessDeniedException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.resources.AccountResource;
import software.wings.resources.graphql.GraphQLUtils;
import software.wings.resources.secretsmanagement.SecretsResourceNG;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WhitelistService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

public class AuthRuleFilterTest extends WingsBaseTest {
  private ResourceInfo resourceInfo = mock(ResourceInfo.class);
  @Mock HttpServletRequest httpServletRequest;
  @Mock AuthService authService;
  @Mock AuthHandler authHandler;
  @Mock AccountService accountService;
  @Mock UserService userService;
  @Mock AppService appService;
  @Mock WhitelistService whitelistService;
  @Mock HarnessUserGroupService harnessUserGroupService;
  @Mock GraphQLUtils graphQLUtils;
  @Mock ContainerRequestContext requestContext;
  @Mock UriInfo uriInfo;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Inject @InjectMocks AuthRuleFilter authRuleFilter;

  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String APP_ID = "APP_ID";
  private static final String PATH = "PATH";
  private static final String USER_ID = "USER_ID";
  private static final String USERNAME = "USERNAME";

  @Before
  public void setUp() throws IOException {
    initMocks(this);
    UserThreadLocal.set(mockUser(true));
    when(authHandler.getAllAccountPermissions())
        .thenReturn(Sets.newHashSet(USER_PERMISSION_MANAGEMENT, ACCOUNT_MANAGEMENT, APPLICATION_CREATE_DELETE,
            TEMPLATE_MANAGEMENT, USER_PERMISSION_READ, AUDIT_VIEWER, TAG_MANAGEMENT, CE_ADMIN, CE_VIEWER,
            MANAGE_CLOUD_PROVIDERS, MANAGE_CONNECTORS, MANAGE_APPLICATION_STACKS, MANAGE_DELEGATES,
            MANAGE_ALERT_NOTIFICATION_RULES, MANAGE_DELEGATE_PROFILES, MANAGE_CONFIG_AS_CODE, MANAGE_SECRETS,
            MANAGE_SECRET_MANAGERS, MANAGE_AUTHENTICATION_SETTINGS, MANAGE_IP_WHITELIST, MANAGE_DEPLOYMENT_FREEZES,
            MANAGE_PIPELINE_GOVERNANCE_STANDARDS));
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testIsAccountLevelPermission() {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.AUDIT_VIEWER, Action.READ);
    PermissionAttribute permissionAttribute1 = new PermissionAttribute(PermissionType.APP, Action.ALL);

    assertThat(authRuleFilter.isAccountLevelPermissions(Arrays.asList(permissionAttribute, permissionAttribute1)))
        .isTrue();

    permissionAttribute = new PermissionAttribute(PermissionType.APP, Action.READ);
    assertThat(authRuleFilter.isAccountLevelPermissions(Arrays.asList(permissionAttribute))).isFalse();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFilterInAuthRuleFilter() {
    Set<Action> actions = new HashSet<>();
    actions.add(Action.DEFAULT);
    when(resourceInfo.getResourceClass()).thenReturn(getMockResourceClass());
    when(resourceInfo.getResourceMethod()).thenReturn(getMockResourceMethod());
    when(requestContext.getMethod()).thenReturn("GET");
    mockUriInfo(PATH, uriInfo);
    when(harnessUserGroupService.isHarnessSupportUser(USER_ID)).thenReturn(true);
    when(harnessUserGroupService.isHarnessSupportEnabledForAccount(ACCOUNT_ID)).thenReturn(true);
    when(whitelistService.isValidIPAddress(anyString(), anyString())).thenReturn(true);
    when(authService.getUserPermissionInfo(anyString(), any(), anyBoolean())).thenReturn(mockUserPermissionInfo());
    authRuleFilter.filter(requestContext);
    assertThat(requestContext.getMethod()).isEqualTo("GET");
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testHarnessUserGraphql() {
    testHarnessUserMethod("graphql", "POST", false);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testHarnessUserPOST() {
    testHarnessUserMethod("/api/services", "POST", true);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testHarnessUserPUT() {
    testHarnessUserMethod("/api/services", "PUT", true);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testHarnessUserDELETE() {
    testHarnessUserMethod("/api/services", "DELETE", true);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testHarnessUserGET() {
    testHarnessUserMethod("/api/services", "GET", false);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testFilter_For_NextGenRequest() {
    Class clazz = SecretsResourceNG.class;
    when(resourceInfo.getResourceClass()).thenReturn(clazz);
    when(resourceInfo.getResourceMethod()).thenReturn(getNgMockResourceMethod());
    boolean isNextGenRequest = authRuleFilter.isNextGenManagerRequest(requestContext);
    assertThat(isNextGenRequest).isFalse();

    when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION))
        .thenReturn(AuthenticationFilter.NEXT_GEN_MANAGER_PREFIX);
    isNextGenRequest = authRuleFilter.isNextGenManagerRequest(requestContext);
    assertThat(isNextGenRequest).isTrue();
  }

  private void testHarnessUserMethod(String url, String method, boolean exception) {
    Set<Action> actions = new HashSet<>();
    actions.add(Action.READ);
    when(resourceInfo.getResourceClass()).thenReturn(getMockResourceClass());
    when(resourceInfo.getResourceMethod()).thenReturn(getMockResourceMethod());
    when(requestContext.getMethod()).thenReturn(method);
    mockUriInfo(url, uriInfo);
    when(harnessUserGroupService.isHarnessSupportUser(USER_ID)).thenReturn(true);
    when(harnessUserGroupService.isHarnessSupportEnabledForAccount(ACCOUNT_ID)).thenReturn(true);
    when(whitelistService.isValidIPAddress(anyString(), anyString())).thenReturn(true);
    when(authService.getUserPermissionInfo(anyString(), any(), anyBoolean())).thenReturn(mockUserPermissionInfo());
    if (exception) {
      thrown.expect(AccessDeniedException.class);
    }
    authRuleFilter.filter(requestContext);

    if (!exception) {
      assertThat(requestContext.getMethod()).isEqualTo(method);
    }
  }

  private Class getMockResourceClass() {
    return AccountResource.class;
  }

  private Method getMockResourceMethod() {
    Class mockClass = AccountResource.class;
    try {
      return mockClass.getMethod("getAccount", String.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private Method getNgMockResourceMethod() {
    Class mockClass = SecretsResourceNG.class;
    try {
      return mockClass.getMethod("get", String.class, String.class, String.class, String.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private void mockUriInfo(String path, UriInfo uriInfo) {
    URI uri;
    try {
      uri = new URI(path);
    } catch (Exception e) {
      uri = null;
    }
    MultivaluedMap<String, String> parameters = mockParameters();
    when(uriInfo.getAbsolutePath()).thenReturn(uri);
    when(uriInfo.getPath()).thenReturn(path);
    when(uriInfo.getQueryParameters()).thenReturn(parameters);
    when(uriInfo.getPathParameters()).thenReturn(parameters);
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
  }

  private MultivaluedMap<String, String> mockParameters() {
    MultivaluedMap<String, String> mockMap = new MultivaluedHashMap<>();
    mockMap.add("accountId", ACCOUNT_ID);
    mockMap.add("appId", APP_ID);
    return mockMap;
  }

  private User mockUser(boolean harnessSupportUser) {
    User dummyUser = new User();
    Account dummyAccount = new Account();
    dummyAccount.setUuid(ACCOUNT_ID);
    dummyUser.setUuid(USER_ID);
    dummyUser.setName(USERNAME);
    dummyUser.setAccounts(Arrays.asList(dummyAccount));
    return dummyUser;
  }

  private UserPermissionInfo mockUserPermissionInfo() {
    Map<String, AppPermissionSummaryForUI> appPermissions = new HashMap<>();
    appPermissions.put(APP_ID, null);
    return UserPermissionInfo.builder().accountId(ACCOUNT_ID).appPermissionMap(appPermissions).build();
  }
}
