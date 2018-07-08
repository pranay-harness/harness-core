package software.wings.service;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.EnvFilter.FilterType.SELECTED;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.UsageRestrictionsServiceImpl;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rktummala on 06/08/18
 */
public class UsageRestrictionsServiceImplTest extends WingsBaseTest {
  @Inject @Named("primaryDatastore") private AdvancedDatastore datastore;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;
  @Mock private AuthHandler authHandler;
  @Mock private Application application;
  @Mock private UserGroupService userGroupService;

  @Spy @InjectMocks private UsageRestrictionsService usageRestrictionsService = new UsageRestrictionsServiceImpl();
  @Rule public ExpectedException thrown = ExpectedException.none();

  private Query<SettingAttribute> spyQuery;

  private PageResponse<SettingAttribute> pageResponse =
      aPageResponse().withResponse(asList(aSettingAttribute().withAppId(SETTING_ID).build())).build();

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    spyQuery = spy(datastore.createQuery(SettingAttribute.class));
    when(wingsPersistence.createQuery(SettingAttribute.class)).thenReturn(spyQuery);
    when(wingsPersistence.query(eq(SettingAttribute.class), any(PageRequest.class))).thenReturn(pageResponse);
    when(wingsPersistence.saveAndGet(eq(SettingAttribute.class), any(SettingAttribute.class)))
        .thenAnswer(
            (Answer<SettingAttribute>) invocationOnMock -> (SettingAttribute) invocationOnMock.getArguments()[1]);
    when(appService.get(anyString())).thenReturn(application);
    when(application.getAccountId()).thenReturn("ACCOUNT_ID");
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
  }

  @Test
  public void testGetDefaultUsageRestrictions() {
    try {
      String ENV_ID_1 = "ENV_ID_1";
      String ENV_ID_2 = "ENV_ID_2";
      String ENV_ID_3 = "ENV_ID_3";

      String APP_ID_1 = "APP_ID_1";
      String APP_ID_2 = "APP_ID_2";
      String APP_ID_3 = "APP_ID_3";

      // Scenario 1
      GenericEntityFilter appFilter =
          GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID)).build();
      HashSet<String> envFilters = newHashSet(SELECTED);
      EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).ids(newHashSet(ENV_ID)).build();
      AppEnvRestriction appEnvRestriction =
          AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      UsageRestrictions expected =
          UsageRestrictions.builder().appEnvRestrictions(newHashSet(appEnvRestriction)).build();

      UsageRestrictions defaultUsageRestrictions =
          usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, APP_ID, ENV_ID);
      assertEquals(defaultUsageRestrictions, expected);

      // Scenario 2
      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID)).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      expected = UsageRestrictions.builder().appEnvRestrictions(newHashSet(appEnvRestriction)).build();

      defaultUsageRestrictions = usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, APP_ID, null);
      assertEquals(defaultUsageRestrictions, expected);

      // Scenario 3
      Set<Action> allActions = newHashSet(Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE, Action.EXECUTE);
      setPermissions(asList(APP_ID_1, APP_ID_2, APP_ID_3), asList(ENV_ID_1, ENV_ID_2, ENV_ID_3), allActions, true);
      defaultUsageRestrictions = usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, null, null);
      assertNull(defaultUsageRestrictions);

      // Scenario 4
      setPermissions(asList(APP_ID_1, APP_ID_2, APP_ID_3), asList(ENV_ID_1, ENV_ID_2, ENV_ID_3), allActions, false);
      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      expected = UsageRestrictions.builder().isEditable(true).appEnvRestrictions(newHashSet(appEnvRestriction)).build();

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class)))
          .thenReturn(newHashSet(APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3));
      AppPermission allAppPermission = AppPermission.builder()
                                           .permissionType(ALL_APP_ENTITIES)
                                           .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                           .actions(newHashSet(allActions))
                                           .build();

      List<UserGroup> userGroups = asList(
          UserGroup.builder().accountId(ACCOUNT_ID).appPermissions(newHashSet(asList(allAppPermission))).build());

      when(userGroupService.getUserGroupsByAccountId(anyString(), any(User.class))).thenReturn(userGroups);
      when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean()))
          .thenReturn(aPageResponse().withResponse(userGroups).build());

      defaultUsageRestrictions = usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, null, null);
      assertEquals(defaultUsageRestrictions, expected);

      // Scenario 5
      setPermissions(asList(APP_ID_1), asList(ENV_ID_1), allActions, false);
      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilters = newHashSet(NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      expected = UsageRestrictions.builder().isEditable(true).appEnvRestrictions(newHashSet(appEnvRestriction)).build();

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class)))
          .thenReturn(newHashSet(APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3));
      AppPermission appPermission1 =
          AppPermission.builder()
              .permissionType(ENV)
              .appFilter(
                  GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build())
              .actions(newHashSet(allActions))
              .entityFilter(EnvFilter.builder().filterTypes(newHashSet(NON_PROD)).build())
              .build();

      userGroups =
          asList(UserGroup.builder().accountId(ACCOUNT_ID).appPermissions(newHashSet(asList(appPermission1))).build());

      when(userGroupService.getUserGroupsByAccountId(anyString(), any(User.class))).thenReturn(userGroups);
      when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean()))
          .thenReturn(aPageResponse().withResponse(userGroups).build());

      defaultUsageRestrictions = usageRestrictionsService.getDefaultUsageRestrictions(ACCOUNT_ID, null, null);
      assertEquals(defaultUsageRestrictions, expected);

    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void testHasAccess() {
    try {
      String ENV_ID_1 = "ENV_ID_1";
      String ENV_ID_2 = "ENV_ID_2";
      String ENV_ID_3 = "ENV_ID_3";

      String APP_ID_1 = "APP_ID_1";
      String APP_ID_2 = "APP_ID_2";
      String APP_ID_3 = "APP_ID_3";

      // Scenario 1
      GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      HashSet<String> envFilters = newHashSet(PROD, NON_PROD);
      EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      AppEnvRestriction appEnvRestriction =
          AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      UsageRestrictions usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class)))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));

      boolean hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, null, null);
      assertTrue(hasAccess);

      // Scenario 2
      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class))).thenReturn(newHashSet(APP_ID_1));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));

      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, ENV_ID);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, null, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, null);
      assertFalse(hasAccess);

      // Scenario 3
      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilters = newHashSet(SELECTED);
      envFilter = EnvFilter.builder().filterTypes(envFilters).ids(newHashSet(ENV_ID_1, ENV_ID_2)).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class))).thenReturn(newHashSet(APP_ID_1));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class))).thenReturn(newHashSet(ENV_ID_1, ENV_ID_2));

      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, ENV_ID_1);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, null, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, null);
      assertFalse(hasAccess);

      // Scenario 4
      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class))).thenReturn(newHashSet(APP_ID));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class))).thenReturn(newHashSet(ENV_ID_1));

      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID_1);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, null, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, null);
      assertFalse(hasAccess);

      // Scenario 5
      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class))).thenReturn(newHashSet(APP_ID));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class))).thenReturn(newHashSet(ENV_ID_1));

      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID_1);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, null, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, null);
      assertFalse(hasAccess);

      // Scenario 6
      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class))).thenReturn(newHashSet());
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class))).thenReturn(newHashSet());

      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID_1);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, null);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, null, null);
      assertTrue(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, ENV_ID);
      assertFalse(hasAccess);
      hasAccess = usageRestrictionsService.hasAccess(usageRestrictions, ACCOUNT_ID, APP_ID_1, null);
      assertFalse(hasAccess);

    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void testCheckIfValidUsageRestrictions() {
    try {
      String ENV_ID_1 = "ENV_ID_1";
      String ENV_ID_2 = "ENV_ID_2";
      String ENV_ID_3 = "ENV_ID_3";

      String APP_ID_1 = "APP_ID_1";
      String APP_ID_2 = "APP_ID_2";
      String APP_ID_3 = "APP_ID_3";

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class)))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));
      Set<Action> allActions = newHashSet(Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE, Action.EXECUTE);
      AppPermission allAppPermission = AppPermission.builder()
                                           .permissionType(ALL_APP_ENTITIES)
                                           .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                           .actions(newHashSet(allActions))
                                           .build();

      List<UserGroup> userGroups = asList(
          UserGroup.builder().accountId(ACCOUNT_ID).appPermissions(newHashSet(asList(allAppPermission))).build());

      when(userGroupService.getUserGroupsByAccountId(anyString(), any(User.class))).thenReturn(userGroups);
      when(userGroupService.list(anyString(), any(PageRequest.class), anyBoolean()))
          .thenReturn(aPageResponse().withResponse(userGroups).build());

      setPermissions(asList(APP_ID_1, APP_ID_2, APP_ID_3), asList(ENV_ID_1, ENV_ID_2, ENV_ID_3), allActions, false);

      // Valid Scenarios
      GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      HashSet<String> envFilters = newHashSet(PROD, NON_PROD);
      EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      AppEnvRestriction appEnvRestriction =
          AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      UsageRestrictions usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(SELECTED);
      envFilter = EnvFilter.builder().filterTypes(envFilters).ids(newHashSet(ENV_ID_1)).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);

      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilters = newHashSet(SELECTED);
      envFilter = EnvFilter.builder().filterTypes(envFilters).ids(newHashSet(ENV_ID_1, ENV_ID_2)).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);

      // Invalid scenarios
      appFilter = GenericEntityFilter.builder().build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertTrue(ex.getResponseMessage() != null);
        assertTrue(ex.getResponseMessage().getCode().equals(ErrorCode.INVALID_USAGE_RESTRICTION));
      }

      appFilter = GenericEntityFilter.builder().filterType(FilterType.SELECTED).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertTrue(ex.getResponseMessage() != null);
        assertTrue(ex.getResponseMessage().getCode().equals(ErrorCode.INVALID_USAGE_RESTRICTION));
      }

      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertTrue(ex.getResponseMessage() != null);
        assertTrue(ex.getResponseMessage().getCode().equals(ErrorCode.INVALID_USAGE_RESTRICTION));
      }

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilter = EnvFilter.builder().build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertTrue(ex.getResponseMessage() != null);
        assertTrue(ex.getResponseMessage().getCode().equals(ErrorCode.INVALID_USAGE_RESTRICTION));
      }

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertTrue(ex.getResponseMessage() != null);
        assertTrue(ex.getResponseMessage().getCode().equals(ErrorCode.INVALID_USAGE_RESTRICTION));
      }

      appEnvRestriction = AppEnvRestriction.builder().build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      try {
        usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertTrue(ex.getResponseMessage() != null);
        assertTrue(ex.getResponseMessage().getCode().equals(ErrorCode.INVALID_USAGE_RESTRICTION));
      }

      usageRestrictions = new UsageRestrictions();
      try {
        usageRestrictionsService.validateUsageRestrictions(ACCOUNT_ID, null, usageRestrictions);
      } catch (WingsException ex) {
        assertTrue(ex.getResponseMessage() != null);
        assertTrue(ex.getResponseMessage().getCode().equals(ErrorCode.NOT_ACCOUNT_MGR));
      }
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  public void testCheckIfUserCanUpdateOrDeleteEntity() {
    try {
      String ENV_ID_1 = "ENV_ID_1";
      String ENV_ID_2 = "ENV_ID_2";
      String ENV_ID_3 = "ENV_ID_3";

      String APP_ID_1 = "APP_ID_1";
      String APP_ID_2 = "APP_ID_2";
      String APP_ID_3 = "APP_ID_3";

      when(authHandler.getAppIdsByFilter(anyString(), any(GenericEntityFilter.class)))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(anyString(), any(EnvFilter.class)))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));

      // Scenario 1
      boolean canUserUpdateOrDeleteEntity = usageRestrictionsService.canUserUpdateOrDeleteEntity(ACCOUNT_ID, null);
      assertTrue(canUserUpdateOrDeleteEntity);

      // Scenario 2
      GenericEntityFilter appFilter1 = GenericEntityFilter.builder()
                                           .filterType(FilterType.SELECTED)
                                           .ids(newHashSet(APP_ID_1, APP_ID_2, APP_ID_3))
                                           .build();
      HashSet<String> envFilters1 = newHashSet(SELECTED);
      EnvFilter envFilter1 =
          EnvFilter.builder().filterTypes(envFilters1).ids(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3)).build();
      AppEnvRestriction appEnvRestriction1 =
          AppEnvRestriction.builder().appFilter(appFilter1).envFilter(envFilter1).build();

      UsageRestrictions usageRestrictions1 =
          UsageRestrictions.builder().appEnvRestrictions(newHashSet(appEnvRestriction1)).build();
      doReturn(usageRestrictions1)
          .when(usageRestrictionsService)
          .getUsageRestrictionsFromUserPermissions(ACCOUNT_ID, false);

      GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      HashSet<String> envFilters = newHashSet(PROD, NON_PROD);
      EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      AppEnvRestriction appEnvRestriction =
          AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      UsageRestrictions usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));

      when(authHandler.getAppIdsByFilter(ACCOUNT_ID, appFilter))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(ACCOUNT_ID, envFilter))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));

      when(authHandler.getAppIdsByFilter(ACCOUNT_ID, appFilter1))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(ACCOUNT_ID, envFilter1))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));

      canUserUpdateOrDeleteEntity = usageRestrictionsService.canUserUpdateOrDeleteEntity(ACCOUNT_ID, usageRestrictions);
      assertTrue(canUserUpdateOrDeleteEntity);

      // Scenario 3
      appFilter1 = GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(APP_ID_1)).build();
      envFilters1 = newHashSet(SELECTED);
      envFilter1 = EnvFilter.builder().filterTypes(envFilters1).ids(newHashSet(ENV_ID_1)).build();
      appEnvRestriction1 = AppEnvRestriction.builder().appFilter(appFilter1).envFilter(envFilter1).build();

      usageRestrictions1 = UsageRestrictions.builder().appEnvRestrictions(newHashSet(appEnvRestriction1)).build();
      doReturn(usageRestrictions1)
          .when(usageRestrictionsService)
          .getUsageRestrictionsFromUserPermissions(ACCOUNT_ID, false);

      when(authHandler.getAppIdsByFilter(ACCOUNT_ID, appFilter))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(ACCOUNT_ID, envFilter))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));

      when(authHandler.getAppIdsByFilter(ACCOUNT_ID, appFilter1)).thenReturn(newHashSet(APP_ID_1));
      when(authHandler.getEnvIdsByFilter(ACCOUNT_ID, envFilter1)).thenReturn(newHashSet(ENV_ID_1));

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      canUserUpdateOrDeleteEntity = usageRestrictionsService.canUserUpdateOrDeleteEntity(ACCOUNT_ID, usageRestrictions);
      assertFalse(canUserUpdateOrDeleteEntity);

      // Scenario 4
      appFilter1 = GenericEntityFilter.builder()
                       .filterType(FilterType.SELECTED)
                       .ids(newHashSet(APP_ID_1, APP_ID_2, APP_ID_3))
                       .build();
      envFilters1 = newHashSet(SELECTED);
      envFilter1 = EnvFilter.builder().filterTypes(envFilters1).ids(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3)).build();
      appEnvRestriction1 = AppEnvRestriction.builder().appFilter(appFilter1).envFilter(envFilter1).build();

      usageRestrictions1 = UsageRestrictions.builder().appEnvRestrictions(newHashSet(appEnvRestriction1)).build();
      doReturn(usageRestrictions1)
          .when(usageRestrictionsService)
          .getUsageRestrictionsFromUserPermissions(ACCOUNT_ID, false);

      when(authHandler.getAppIdsByFilter(ACCOUNT_ID, appFilter))
          .thenReturn(newHashSet(APP_ID, APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(ACCOUNT_ID, envFilter))
          .thenReturn(newHashSet(ENV_ID, ENV_ID_1, ENV_ID_2, ENV_ID_3));

      when(authHandler.getAppIdsByFilter(ACCOUNT_ID, appFilter1)).thenReturn(newHashSet(APP_ID_1, APP_ID_2, APP_ID_3));
      when(authHandler.getEnvIdsByFilter(ACCOUNT_ID, envFilter1)).thenReturn(newHashSet(ENV_ID_1, ENV_ID_2, ENV_ID_3));

      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
      envFilters = newHashSet(PROD, NON_PROD);
      envFilter = EnvFilter.builder().filterTypes(envFilters).build();
      appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
      usageRestrictions = new UsageRestrictions();
      usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
      canUserUpdateOrDeleteEntity = usageRestrictionsService.canUserUpdateOrDeleteEntity(ACCOUNT_ID, usageRestrictions);
      assertFalse(canUserUpdateOrDeleteEntity);

    } finally {
      UserThreadLocal.unset();
    }
  }

  private void setPermissions(List<String> appIds, List<String> envIds, Set<Action> actions, boolean isAccountAdmin) {
    User user = User.Builder.anUser().withName(USER_NAME).withUuid(USER_ID).build();
    Map<String, AppPermissionSummaryForUI> appPermissionsMap = Maps.newHashMap();

    Map<String, Set<Action>> envPermissionMap = Maps.newHashMap();
    envIds.forEach(envId -> envPermissionMap.put(envId, actions));

    AppPermissionSummaryForUI appPermissionSummaryForUI =
        AppPermissionSummaryForUI.builder().envPermissions(envPermissionMap).build();

    appIds.forEach(appId -> appPermissionsMap.put(appId, appPermissionSummaryForUI));

    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder()
                                                .accountId(ACCOUNT_ID)
                                                .isRbacEnabled(true)
                                                .appPermissionMap(appPermissionsMap)
                                                .build();
    if (isAccountAdmin) {
      AccountPermissionSummary accountPermissionSummary =
          AccountPermissionSummary.builder().permissions(newHashSet(PermissionType.ACCOUNT_MANAGEMENT)).build();
      userPermissionInfo.setAccountPermissionSummary(accountPermissionSummary);
    }
    user.setUserRequestContext(
        UserRequestContext.builder().accountId(ACCOUNT_ID).userPermissionInfo(userPermissionInfo).build());
    UserThreadLocal.set(user);
  }
}