package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.eraro.ErrorCode.NOT_ACCOUNT_MGR_NOR_HAS_ALL_APP_ACCESS;
import static io.harness.eraro.ErrorCode.USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.RAMA;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VIKAS;
import static io.harness.rule.OwnerRule.VOJIN;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.EnvFilter.FilterType.SELECTED;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
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

import io.harness.CategoryTest;
import io.harness.beans.EncryptedData;
import io.harness.beans.EnvironmentType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Environment;
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
import software.wings.security.UsageRestrictions;
import software.wings.security.UsageRestrictions.AppEnvRestriction;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRestrictionInfo;
import software.wings.security.UserRestrictionInfo.UserRestrictionInfoBuilder;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingVariableTypes;
import software.wings.settings.UsageRestrictionsReferenceSummary;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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

/**
 * @author rktummala on 06/08/18
 */
@RunWith(MockitoJUnitRunner.class)
public class UsageRestrictionsServiceImplTestBase extends CategoryTest {
  @Mock private WingsPersistence mockWingsPersistence;
  @Mock private AppService appService;
  @Mock private AuthHandler authHandler;
  @Mock private Application application;
  @Mock private UserGroupService userGroupService;
  @Mock private UserService userService;
  @Mock private EnvironmentService envService;
  @Mock private SettingsService settingsService;
  @Mock private SettingServiceHelper settingServiceHelper;
  @Mock private SecretManager secretManager;
  @Mock private Query<SettingAttribute> query;

  private static Set<Action> readAction = newHashSet(Action.READ);
  private static Set<Action> updateAndReadAction = newHashSet(Action.UPDATE, Action.READ);
  private static Set<Action> allActions = newHashSet(
      Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE, Action.EXECUTE_WORKFLOW, Action.EXECUTE_PIPELINE);

  private String SETTING_ATTRIBUTE_ID = "SETTING_ATTRIBUTE_ID";

  private String ENV_ID_1 = "ENV_ID_1";
  private String ENV_ID_2 = "ENV_ID_2";
  private String ENV_ID_3 = "ENV_ID_3";

  private String APP_ID_1 = "APP_ID_1";
  private String APP_ID_2 = "APP_ID_2";
  private String APP_ID_3 = "APP_ID_3";

  public static UsageRestrictions getUsageRestrictionsForAppIdAndEnvId(String appId, String envId) {
    GenericEntityFilter appFilter =
        GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(newHashSet(appId)).build();
    HashSet<String> envFilters = newHashSet(SELECTED);
    EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilters).ids(newHashSet(envId)).build();
    AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    return UsageRestrictions.builder().appEnvRestrictions(newHashSet(appEnvRestriction)).build();
  }

  public static UserPermissionInfo getUserPermissionInfo(
      List<String> appIds, List<String> envIds, Set<Action> actions) {
    Map<Action, Set<EnvInfo>> envPermissionsInternal = new HashMap<>();

    Set<EnvInfo> envInfoSet = envIds.stream()
                                  .map(envId -> EnvInfo.builder().envType("PROD").envId(envId).build())
                                  .collect(Collectors.toSet());
    actions.forEach(action -> envPermissionsInternal.put(action, envInfoSet));

    Map<String, AppPermissionSummary> appPermissionsMapInternal = new HashMap<>();
    AppPermissionSummary appPermissionSummaryInternal =
        AppPermissionSummary.builder().envPermissions(envPermissionsInternal).build();

    appIds.forEach(appId -> appPermissionsMapInternal.put(appId, appPermissionSummaryInternal));

    Map<String, AppPermissionSummaryForUI> appPermissionsMap = new HashMap<>();

    Map<String, Set<Action>> envPermissionMap = new HashMap<>();
    envIds.forEach(envId -> envPermissionMap.put(envId, actions));

    AppPermissionSummaryForUI appPermissionSummaryForUI =
        AppPermissionSummaryForUI.builder().envPermissions(envPermissionMap).build();

    appIds.forEach(appId -> appPermissionsMap.put(appId, appPermissionSummaryForUI));

    return UserPermissionInfo.builder()
        .accountId(ACCOUNT_ID)
        .appPermissionMap(appPermissionsMap)
        .appPermissionMapInternal(appPermissionsMapInternal)
        .build();
  }
}
