package software.wings.service.impl;

import static com.google.common.collect.Sets.newHashSet;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.EnvFilter.FilterType.SELECTED;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.Maps;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;
import software.wings.settings.UsageRestrictionsReferenceSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rktummala on 06/08/18
 */
@RunWith(MockitoJUnitRunner.class)
public class UsageRestrictionsServiceImplTest {
  @Mock private WingsPersistence mockWingsPersistence;
  @Mock private AppService appService;
  @Mock private AuthHandler authHandler;
  @Mock private Application application;
  @Mock private UserGroupService userGroupService;
  @Mock private EnvironmentService envService;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @Mock private Query<SettingAttribute> query;

  private static Set<Action> readAction = newHashSet(Action.READ);
  private static Set<Action> updateAndReadAction = newHashSet(Action.UPDATE, Action.READ);
  private static Set<Action> allActions =
      newHashSet(Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE, Action.EXECUTE);

  private String SETTING_ATTRIBUTE_ID = "SETTING_ATTRIBUTE_ID";

  private String ENV_ID_1 = "ENV_ID_1";
  private String ENV_ID_2 = "ENV_ID_2";
  private String ENV_ID_3 = "ENV_ID_3";

  private String APP_ID_1 = "APP_ID_1";
  private String APP_ID_2 = "APP_ID_2";
  private String APP_ID_3 = "APP_ID_3";

  @Spy
  @InjectMocks
  private UsageRestrictionsServiceImpl usageRestrictionsService = new UsageRestrictionsServiceImpl(
      authHandler, userGroupService, appService, envService, settingsService, secretManager, mockWingsPersistence);
  @Rule public ExpectedException thrown = ExpectedException.none();

  private PageResponse<SettingAttribute> pageResponse =
      aPageResponse().withResponse(asList(aSettingAttribute().withAppId(APP_ID).build())).build();

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(mockWingsPersistence.createQuery(SettingAttribute.class)).thenReturn(query);

    when(mockWingsPersistence.query(eq(SettingAttribute.class), any(PageRequest.class))).thenReturn(pageResponse);
    when(mockWingsPersistence.saveAndGet(eq(SettingAttribute.class), any(SettingAttribute.class)))
        .thenAnswer(
            (Answer<SettingAttribute>) invocationOnMock -> (SettingAttribute) invocationOnMock.getArguments()[1]);
    when(appService.get(anyString())).thenReturn(application);
    when(application.getAccountId()).thenReturn("ACCOUNT_ID");
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
  }

  private void setUserGroupMocks(AppPermission appPermission, List<String> appIds) {
    setUserGroupMocks(asList(appPermission), appIds);
  }

  private void setUserGroupMocks(List<AppPermission> appPermissions, List<String> appIds) {
    List<UserGroup> userGroups =
        asList(UserGroup.builder().accountId(ACCOUNT_ID).appPermissions(newHashSet(appPermissions)).build());
    pageResponse = aPageResponse().withResponse(userGroups).build();
    when(userGroupService.getUserGroupsByAccountId(anyString(), any(User.class))).thenReturn(userGroups);
    when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean())).thenReturn(pageResponse);
    when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class))).thenReturn(newHashSet(appIds));
  }

  private void shouldGetDefaultRestrictionsWithUserAndSelectedAppsAndSelectedEnvs(boolean isAccountAdmin) {
    try {
      List<String> appIds = asList(APP_ID_1);
      List<String> envIds = asList(ENV_ID_1);

      AppPermission appPermission =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(appIds)).build())
              .actions(newHashSet(allActions))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(SELECTED)).ids(newHashSet(envIds)).build())
              .build();
      setUserGroupMocks(appPermission, appIds);
      Set<Action> actions = allActions;

      setPermissions(appIds, envIds, actions, isAccountAdmin);

      UsageRestrictions expected = getUsageRestrictionsForAppIdAndEnvId(APP_ID_1, ENV_ID_1);
      UsageRestrictions defaultUsageRestrictions =
          usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, APP_ID_1, ENV_ID_1);
      assertEquals(expected, defaultUsageRestrictions);

      defaultUsageRestrictions = usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, null, null);
      assertEquals(expected, defaultUsageRestrictions);

      defaultUsageRestrictions = usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, APP_ID_1, null);
      assertEquals(expected, defaultUsageRestrictions);

      expected = getUsageRestrictionsForAppIdAndEnvId(APP_ID, ENV_ID_1);
      defaultUsageRestrictions = usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, APP_ID, ENV_ID_1);
      assertEquals(expected, defaultUsageRestrictions);

      expected = getUsageRestrictionsForAppIdAndEnvId(APP_ID_1, ENV_ID);
      defaultUsageRestrictions = usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, APP_ID_1, ENV_ID);
      assertEquals(expected, defaultUsageRestrictions);

    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void testHasAllEnvAccessOfType() {
    UsageRestrictions usageRestrictions = null;
    GenericEntityFilter appFilter = null;
    EnvFilter envFilter = null;

    assertFalse(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID, EnvFilter.FilterType.NON_PROD));

    usageRestrictions = new UsageRestrictions();
    assertFalse(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID, EnvFilter.FilterType.NON_PROD));

    AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder().appFilter(null).envFilter(null).build();
    usageRestrictions.setAppEnvRestrictions(Collections.singleton(appEnvRestriction));
    assertFalse(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID, EnvFilter.FilterType.NON_PROD));

    appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    envFilter = EnvFilter.builder()
                    .filterTypes(Collections.singleton(EnvFilter.FilterType.NON_PROD))
                    .ids(Collections.singleton(ENV_ID_1))
                    .build();
    appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    usageRestrictions.setAppEnvRestrictions(Collections.singleton(appEnvRestriction));
    assertFalse(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID, EnvFilter.FilterType.PROD));
    assertTrue(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID, EnvFilter.FilterType.NON_PROD));

    appFilter =
        GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Collections.singleton(APP_ID)).build();
    envFilter = EnvFilter.builder()
                    .filterTypes(Collections.singleton(EnvFilter.FilterType.SELECTED))
                    .ids(Collections.singleton(ENV_ID_1))
                    .build();
    appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    usageRestrictions.setAppEnvRestrictions(Collections.singleton(appEnvRestriction));
    assertFalse(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID_1, EnvFilter.FilterType.SELECTED));
    assertFalse(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID, EnvFilter.FilterType.PROD));
    assertTrue(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID, EnvFilter.FilterType.SELECTED));

    // No NPE is expected if envFilter filter types are null;
    appFilter =
        GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Collections.singleton(APP_ID)).build();
    envFilter = EnvFilter.builder().build();
    appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    usageRestrictions.setAppEnvRestrictions(Collections.singleton(appEnvRestriction));
    assertFalse(
        UsageRestrictionsServiceImpl.hasAllEnvAccessOfType(usageRestrictions, APP_ID, EnvFilter.FilterType.SELECTED));
  }

  @Test
  public void shouldGetDefaultRestrictionsWithMultipleUserGroups() {
    try {
      List<String> appIds = asList(APP_ID_1);
      List<String> envIds = asList(ENV_ID_1);

      AppPermission allAppPermissions = AppPermission.builder()
                                            .permissionType(ALL_APP_ENTITIES)
                                            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                            .actions(newHashSet(allActions))
                                            .build();
      AppPermission prodAppPermission = AppPermission.builder()
                                            .permissionType(ENV)
                                            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                            .actions(newHashSet(allActions))
                                            .entityFilter(EnvFilter.builder().filterTypes(newHashSet(PROD)).build())
                                            .build();
      AppPermission nonProdAppPermission =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
              .actions(newHashSet(allActions))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(NON_PROD)).build())
              .build();
      setUserGroupMocks(asList(allAppPermissions, prodAppPermission, nonProdAppPermission), appIds);
      Set<Action> actions = allActions;

      setPermissions(appIds, envIds, actions, false);

      GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      HashSet<String> prodEnvFilters = newHashSet(PROD);
      EnvFilter prodEnvFilter = EnvFilter.builder().filterTypes(prodEnvFilters).build();
      AppEnvRestriction prodAppEnvRestriction =
          AppEnvRestriction.builder().appFilter(appFilter).envFilter(prodEnvFilter).build();

      HashSet<String> nonprodEnvFilters = newHashSet(NON_PROD);
      EnvFilter nonprodEnvFilter = EnvFilter.builder().filterTypes(nonprodEnvFilters).build();
      AppEnvRestriction nonprodAppEnvRestriction =
          AppEnvRestriction.builder().appFilter(appFilter).envFilter(nonprodEnvFilter).build();

      UsageRestrictions expected = UsageRestrictions.builder()
                                       .appEnvRestrictions(newHashSet(prodAppEnvRestriction, nonprodAppEnvRestriction))
                                       .build();
      UsageRestrictions defaultUsageRestrictions =
          usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, null, null);
      assertEquals(expected, defaultUsageRestrictions);
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void shouldGetDefaultRestrictionsWithAdminUserAndSelectedAppsAndSelectedEnvs() {
    shouldGetDefaultRestrictionsWithUserAndSelectedAppsAndSelectedEnvs(true);
  }

  @Test
  public void shouldGetDefaultRestrictionsWithNonAdminUserAndSelectedAppsAndSelectedEnvs() {
    shouldGetDefaultRestrictionsWithUserAndSelectedAppsAndSelectedEnvs(false);
  }

  @Test
  public void shouldHaveAccessWithUserHavingUpdateAccessToApp() {
    try {
      boolean isAccountAdmin = true;
      List<String> appIds = asList(APP_ID_1, APP_ID_2, APP_ID_3);
      AppPermission appPermission =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(
                  GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build())
              .actions(newHashSet(updateAndReadAction))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(SELECTED)).ids(newHashSet(ENV_ID_1)).build())
              .build();
      setUserGroupMocks(appPermission, appIds);

      List<String> envIds = asList(ENV_ID_1, ENV_ID_2, ENV_ID_3);
      Set<Action> actions = updateAndReadAction;

      setPermissions(appIds, envIds, actions, isAccountAdmin);

      UsageRestrictions restrictionsFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForUpdateAction();
      Map<String, Set<String>> appEnvMapFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForUpdateAction();

      GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      Set<String> envFilters = newHashSet(PROD);
      EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      AppEnvRestriction appEnvRestriction =
          AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      UsageRestrictions usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));

      boolean hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID_1, ENV_ID_1,
          usageRestrictions, restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID_1, null, usageRestrictions,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID, ENV_ID_1, usageRestrictions,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID_1, ENV_ID, usageRestrictions,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);

      UsageRestrictions restrictionsFromPermissionsForReadAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForReadAction();
      Map<String, Set<String>> appEnvMapFromPermissionsForReadAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForReadAction();

      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, null, null, usageRestrictions,
          restrictionsFromPermissionsForReadAction, appEnvMapFromPermissionsForReadAction);
      assertTrue(hasAccess);

    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void shouldHaveAccessWithAdminUserAndNoRestrictions() {
    try {
      List<String> appIds = asList(APP_ID_1, APP_ID_2, APP_ID_3);
      AppPermission appPermission =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(
                  GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build())
              .actions(newHashSet(allActions))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(SELECTED)).ids(newHashSet(ENV_ID_1)).build())
              .build();
      setUserGroupMocks(appPermission, appIds);

      List<String> envIds = asList(ENV_ID_1, ENV_ID_2, ENV_ID_3);
      Set<Action> actions = allActions;

      setPermissions(appIds, envIds, actions, true);

      UsageRestrictions restrictionsFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForUpdateAction();
      Map<String, Set<String>> appEnvMapFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForUpdateAction();

      boolean hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, true, APP_ID_1, ENV_ID_1, null,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, true, APP_ID_1, null, null,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, true, APP_ID, ENV_ID_1, null,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, true, APP_ID_1, ENV_ID, null,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);

      UsageRestrictions restrictionsFromPermissionsForReadAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForReadAction();
      Map<String, Set<String>> appEnvMapFromPermissionsForReadAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForReadAction();

      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, true, null, null, null,
          restrictionsFromPermissionsForReadAction, appEnvMapFromPermissionsForReadAction);
      assertTrue(hasAccess);

    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void shouldHaveAccessWithNonAdminUserAndNoRestrictions() {
    try {
      List<String> appIds = asList(APP_ID_1, APP_ID_2, APP_ID_3);
      AppPermission appPermission =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(
                  GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build())
              .actions(newHashSet(allActions))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(SELECTED)).ids(newHashSet(ENV_ID_1)).build())
              .build();
      setUserGroupMocks(appPermission, appIds);

      List<String> envIds = asList(ENV_ID_1, ENV_ID_2, ENV_ID_3);
      Set<Action> actions = allActions;

      setPermissions(appIds, envIds, actions, false);

      UsageRestrictions restrictionsFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForUpdateAction();
      Map<String, Set<String>> appEnvMapFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForUpdateAction();

      boolean hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, false, APP_ID_1, ENV_ID_1, null,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, false, APP_ID_1, null, null,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, false, APP_ID, ENV_ID_1, null,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, false, APP_ID_1, ENV_ID, null,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);

      UsageRestrictions restrictionsFromPermissionsForReadAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForReadAction();
      Map<String, Set<String>> appEnvMapFromPermissionsForReadAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForReadAction();

      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, false, null, null, null,
          restrictionsFromPermissionsForReadAction, appEnvMapFromPermissionsForReadAction);
      assertFalse(hasAccess);

    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void isEditableByNonProdSupportAndAllAppAllEnvRestrictions() {
    try {
      List<String> appIds = asList(APP_ID_1, APP_ID_2, APP_ID_3);
      AppPermission appPermission = AppPermission.builder()
                                        .permissionType(ENV)
                                        .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                        .actions(allActions)
                                        .entityFilter(EnvFilter.builder().filterTypes(newHashSet(NON_PROD)).build())
                                        .build();
      setUserGroupMocks(appPermission, appIds);

      List<String> envIds = new ArrayList<>();
      Set<Action> actions = newHashSet(Action.UPDATE);

      setPermissions(appIds, envIds, actions, false);

      UsageRestrictions entityUsageRestrictions =
          getUsageRestrictionsWithAllAppsAndEnvTypes(newHashSet(PROD, NON_PROD));

      boolean hasEditPermissions =
          usageRestrictionsService.userHasPermissionsToChangeEntity(ACCOUNT_ID, entityUsageRestrictions);
      assertFalse(hasEditPermissions);

    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void shouldHaveAccessWithUserHavingReadAccessToApp() {
    try {
      boolean isAccountAdmin = true;
      List<String> appIds = asList(APP_ID_1, APP_ID_2, APP_ID_3);
      AppPermission appPermission =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(
                  GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build())
              .actions(newHashSet(readAction))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(SELECTED)).ids(newHashSet(ENV_ID_1)).build())
              .build();
      setUserGroupMocks(appPermission, appIds);

      List<String> envIds = asList(ENV_ID_1, ENV_ID_2, ENV_ID_3);
      Set<Action> actions = newHashSet(Action.READ);

      setPermissions(appIds, envIds, actions, isAccountAdmin);

      UsageRestrictions restrictionsFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForUpdateAction();
      Map<String, Set<String>> appEnvMapFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForUpdateAction();

      UsageRestrictions usageRestrictions = getUsageRestrictionsWithAllAppsAndEnvTypes(newHashSet(PROD));

      boolean hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID, ENV_ID,
          usageRestrictions, restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID, null, usageRestrictions,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      checkIfHasAccess(isAccountAdmin, usageRestrictions, restrictionsFromPermissionsForUpdateAction,
          appEnvMapFromPermissionsForUpdateAction);
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void shouldRemoveEnvReferences() {
    UsageRestrictions usageRestrictions = setupUsageRestrictionsForAppEnvReferenceTesting();

    int count = usageRestrictionsService.removeAppEnvReferences(ACCOUNT_ID, APP_ID, ENV_ID);
    assertEquals(0, count);
    assertEquals(1, usageRestrictions.getAppEnvRestrictions().size());
    verifyZeroInteractions(settingsService);
    verifyZeroInteractions(secretManager);

    count = usageRestrictionsService.removeAppEnvReferences(ACCOUNT_ID, APP_ID_1, ENV_ID_1);
    assertEquals(1, count);
    assertEquals(0, usageRestrictions.getAppEnvRestrictions().size());
    verify(settingsService, times(1))
        .updateUsageRestrictionsInternal(eq(SETTING_ATTRIBUTE_ID), any(UsageRestrictions.class));
    verifyZeroInteractions(secretManager);
  }

  @Test
  public void shouldRemoveAppReferences() {
    UsageRestrictions usageRestrictions = setupUsageRestrictionsForAppEnvReferenceTesting();

    int count = usageRestrictionsService.removeAppEnvReferences(ACCOUNT_ID, APP_ID, null);
    assertEquals(0, count);
    assertEquals(1, usageRestrictions.getAppEnvRestrictions().size());
    verifyZeroInteractions(settingsService);
    verifyZeroInteractions(secretManager);

    count = usageRestrictionsService.removeAppEnvReferences(ACCOUNT_ID, APP_ID_1, null);
    assertEquals(1, count);
    assertEquals(0, usageRestrictions.getAppEnvRestrictions().size());
    verify(settingsService, times(1))
        .updateUsageRestrictionsInternal(eq(SETTING_ATTRIBUTE_ID), any(UsageRestrictions.class));
    verifyZeroInteractions(secretManager);
  }

  @Test
  public void shouldPurgeDanglingReferences() {
    UsageRestrictions usageRestrictions = setupUsageRestrictionsForAppEnvReferenceTesting();

    when(appService.exist(APP_ID_1)).thenReturn(false);

    int count = usageRestrictionsService.purgeDanglingAppEnvReferences(ACCOUNT_ID);
    assertEquals(1, count);
    assertEquals(0, usageRestrictions.getAppEnvRestrictions().size());
    verify(settingsService, times(1)).updateUsageRestrictionsInternal(anyString(), any(UsageRestrictions.class));
  }

  @Test
  public void shouldReturnReferenceSummaries() {
    setupUsageRestrictionsForAppEnvReferenceTesting();

    UsageRestrictionsReferenceSummary summary =
        usageRestrictionsService.getReferenceSummaryForApp(ACCOUNT_ID, APP_ID_1);
    assertNotNull(summary);
    assertEquals(1, summary.getTotal());
    assertEquals(1, summary.getNumOfSettings());
    assertEquals(1, summary.getSettings().size());

    summary = usageRestrictionsService.getReferenceSummaryForEnv(ACCOUNT_ID, ENV_ID_1);
    assertNotNull(summary);
    assertEquals(1, summary.getTotal());
    assertEquals(1, summary.getNumOfSettings());
    assertEquals(1, summary.getSettings().size());
  }

  private UsageRestrictions setupUsageRestrictionsForAppEnvReferenceTesting() {
    UsageRestrictions usageRestrictions = getUsageRestrictionsForAppIdAndEnvId(APP_ID_1, ENV_ID_1);
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setUuid(SETTING_ATTRIBUTE_ID);
    settingAttribute.setUsageRestrictions(usageRestrictions);

    Query<SettingAttribute> query1 = mock(Query.class);
    FieldEnd fieldEnd1 = mock(FieldEnd.class);
    MorphiaIterator iterator1 = mock(MorphiaIterator.class);

    when(mockWingsPersistence.createQuery(eq(SettingAttribute.class))).thenReturn(query1);
    when(query1.filter(anyString(), anyObject())).thenReturn(query1);
    when(query1.field(any())).thenReturn(fieldEnd1);
    when(fieldEnd1.exists()).thenReturn(query1);
    when(query1.fetch()).thenReturn(iterator1);
    when(iterator1.hasNext()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);
    when(iterator1.next()).thenReturn(settingAttribute);

    Query<EncryptedData> query2 = mock(Query.class);
    FieldEnd fieldEnd2 = mock(FieldEnd.class);
    MorphiaIterator iterator2 = mock(MorphiaIterator.class);

    when(mockWingsPersistence.createQuery(eq(EncryptedData.class))).thenReturn(query2);
    when(query2.filter(anyString(), anyObject())).thenReturn(query2);
    when(query2.field(any())).thenReturn(fieldEnd2);
    when(fieldEnd2.exists()).thenReturn(query2);
    when(query2.fetch()).thenReturn(iterator2);
    doReturn(false).when(iterator2).hasNext();

    return usageRestrictions;
  }

  private void checkIfHasAccess(boolean isAccountAdmin, UsageRestrictions usageRestrictions,
      UsageRestrictions restrictionsFromPermissionsForUpdateAction,
      Map<String, Set<String>> appEnvMapFromPermissionsForUpdateAction) {
    boolean hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID, ENV_ID,
        usageRestrictions, restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
    assertFalse(hasAccess);
    hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID_1, ENV_ID, usageRestrictions,
        restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
    assertFalse(hasAccess);

    UsageRestrictions restrictionsFromPermissionsForReadAction =
        UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForReadAction();
    Map<String, Set<String>> appEnvMapFromPermissionsForReadAction =
        UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForReadAction();

    hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, null, null, usageRestrictions,
        restrictionsFromPermissionsForReadAction, appEnvMapFromPermissionsForReadAction);
    assertTrue(hasAccess);
  }

  public static UsageRestrictions getUsageRestrictionsForAppIdAndEnvId(String appId, String envId) {
    GenericEntityFilter appFilter =
        GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(appId)).build();
    HashSet<String> envFilters = newHashSet(SELECTED);
    EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).ids(newHashSet(envId)).build();
    AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    return UsageRestrictions.builder().appEnvRestrictions(newHashSet(appEnvRestriction)).build();
  }

  @Test
  public void shouldHaveAccessWithUserHavingNoAccessToApp() {
    try {
      boolean isAccountAdmin = true;
      List<String> appIds = asList(APP_ID_1, APP_ID_2, APP_ID_3);
      AppPermission appPermission =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(
                  GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build())
              .actions(newHashSet(allActions))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(SELECTED)).ids(newHashSet(ENV_ID_1)).build())
              .build();
      setUserGroupMocks(appPermission, appIds);

      List<String> envIds = asList(ENV_ID_1, ENV_ID_2, ENV_ID_3);
      Set<Action> actions = allActions;

      setPermissions(appIds, envIds, actions, isAccountAdmin);

      UsageRestrictions usageRestrictions = getUsageRestrictionsWithAllAppsAndEnvTypes(newHashSet(PROD));

      UsageRestrictions restrictionsFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getUsageRestrictionsForUpdateAction();
      Map<String, Set<String>> appEnvMapFromPermissionsForUpdateAction =
          UserThreadLocal.get().getUserRequestContext().getUserPermissionInfo().getAppEnvMapForUpdateAction();

      boolean hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID, ENV_ID,
          usageRestrictions, restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(ACCOUNT_ID, isAccountAdmin, APP_ID, null, usageRestrictions,
          restrictionsFromPermissionsForUpdateAction, appEnvMapFromPermissionsForUpdateAction);
      assertFalse(hasAccess);
      checkIfHasAccess(isAccountAdmin, usageRestrictions, restrictionsFromPermissionsForUpdateAction,
          appEnvMapFromPermissionsForUpdateAction);
    } finally {
      UserThreadLocal.unset();
    }
  }

  private UsageRestrictions getUsageRestrictionsWithAllAppsAndEnvTypes(Set<String> envFilters) {
    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).build();
    AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    UsageRestrictions usageRestrictions = new UsageRestrictions();
    usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
    return usageRestrictions;
  }

  @Test
  public void testCheckIfValidUsageRestrictions() {
    try {
      List<String> appIds = asList(APP_ID_1, APP_ID_2, APP_ID_3);
      AppPermission appPermission =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(
                  GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build())
              .actions(newHashSet(allActions))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(SELECTED)).ids(newHashSet(ENV_ID_1)).build())
              .build();
      setUserGroupMocks(appPermission, appIds);

      List<String> envIds = asList(ENV_ID_1, ENV_ID_2, ENV_ID_3);
      Set<Action> actions = allActions;

      setPermissions(appIds, envIds, actions, true);

      // Valid Scenarios
      GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      HashSet<String> envFilters = newHashSet(PROD, NON_PROD);
      EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      AppEnvRestriction appEnvRestriction =
          AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      UsageRestrictions usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(SELECTED);
      envFilter = EnvFilter.builder().filterTypes(envFilters).ids(newHashSet(ENV_ID_1)).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilters = newHashSet(SELECTED);
      envFilter = EnvFilter.builder().filterTypes(envFilters).ids(newHashSet(ENV_ID_1, ENV_ID_2)).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);

      // Invalid scenarios
      appFilter = GenericEntityFilter.builder().build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertEquals(ex.getCode(), ErrorCode.INVALID_USAGE_RESTRICTION);
      }

      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertEquals(ex.getCode(), ErrorCode.INVALID_USAGE_RESTRICTION);
      }

      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertEquals(ex.getCode(), ErrorCode.INVALID_USAGE_RESTRICTION);
      }

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilter = EnvFilter.builder().build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertEquals(ex.getCode(), ErrorCode.INVALID_USAGE_RESTRICTION);
      }

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertEquals(ex.getCode(), ErrorCode.INVALID_USAGE_RESTRICTION);
      }

      appEnvRestriction = AppEnvRestriction.builder().build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertEquals(ex.getCode(), ErrorCode.INVALID_USAGE_RESTRICTION);
      }

      usageRestrictions = new UsageRestrictions();
      try {
        usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertEquals(ex.getCode(), ErrorCode.NOT_ACCOUNT_MGR_NOR_HAS_ALL_APP_ACCESS);
      }
    } finally {
      UserThreadLocal.unset();
    }
  }

  private void setPermissions(List<String> appIds, List<String> envIds, Set<Action> actions, boolean isAccountAdmin) {
    UserPermissionInfo userPermissionInfo = getUserPermissionInfo(appIds, envIds, actions);

    User user = User.Builder.anUser().withName(USER_NAME).withUuid(USER_ID).build();

    if (isAccountAdmin) {
      AccountPermissionSummary accountPermissionSummary =
          AccountPermissionSummary.builder().permissions(newHashSet(PermissionType.ACCOUNT_MANAGEMENT)).build();
      userPermissionInfo.setAccountPermissionSummary(accountPermissionSummary);
    }
    user.setUserRequestContext(
        UserRequestContext.builder().accountId(ACCOUNT_ID).userPermissionInfo(userPermissionInfo).build());

    UsageRestrictions restrictionsFromUserPermissionsForUpdate =
        usageRestrictionsService.getUsageRestrictionsFromUserPermissions(
            ACCOUNT_ID, userPermissionInfo, user, Action.UPDATE);
    userPermissionInfo.setUsageRestrictionsForUpdateAction(restrictionsFromUserPermissionsForUpdate);

    UsageRestrictions restrictionsFromUserPermissionsForRead =
        usageRestrictionsService.getUsageRestrictionsFromUserPermissions(
            ACCOUNT_ID, userPermissionInfo, user, Action.READ);
    userPermissionInfo.setUsageRestrictionsForReadAction(restrictionsFromUserPermissionsForRead);

    Map<String, Set<String>> appEnvMapForUpdate =
        usageRestrictionsService.getAppEnvMapFromUserPermissions(ACCOUNT_ID, userPermissionInfo, Action.UPDATE);
    userPermissionInfo.setAppEnvMapForUpdateAction(appEnvMapForUpdate);

    Map<String, Set<String>> appEnvMapForRead =
        usageRestrictionsService.getAppEnvMapFromUserPermissions(ACCOUNT_ID, userPermissionInfo, Action.READ);
    userPermissionInfo.setAppEnvMapForReadAction(appEnvMapForRead);

    UserThreadLocal.set(user);
  }

  private UserPermissionInfo getUserPermissionInfo(List<String> appIds, List<String> envIds, Set<Action> actions) {
    Map<Action, Set<EnvInfo>> envPermissionsInternal = Maps.newHashMap();

    Set<EnvInfo> envInfoSet = envIds.stream()
                                  .map(envId -> EnvInfo.builder().envType("PROD").envId(envId).build())
                                  .collect(Collectors.toSet());
    actions.forEach(action -> envPermissionsInternal.put(action, envInfoSet));

    Map<String, AppPermissionSummary> appPermissionsMapInternal = Maps.newHashMap();
    AppPermissionSummary appPermissionSummaryInternal =
        AppPermissionSummary.builder().envPermissions(envPermissionsInternal).build();

    appIds.forEach(appId -> appPermissionsMapInternal.put(appId, appPermissionSummaryInternal));

    Map<String, AppPermissionSummaryForUI> appPermissionsMap = Maps.newHashMap();

    Map<String, Set<Action>> envPermissionMap = Maps.newHashMap();
    envIds.forEach(envId -> envPermissionMap.put(envId, actions));

    AppPermissionSummaryForUI appPermissionSummaryForUI =
        AppPermissionSummaryForUI.builder().envPermissions(envPermissionMap).build();

    appIds.forEach(appId -> appPermissionsMap.put(appId, appPermissionSummaryForUI));

    return UserPermissionInfo.builder()
        .accountId(ACCOUNT_ID)
        .isRbacEnabled(true)
        .appPermissionMap(appPermissionsMap)
        .appPermissionMapInternal(appPermissionsMapInternal)
        .build();
  }
}