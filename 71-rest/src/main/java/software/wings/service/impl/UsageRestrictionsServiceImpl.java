package software.wings.service.impl;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.emptySet;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.EntityReference;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.User;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.restrictions.AppRestrictionsSummary;
import software.wings.beans.security.restrictions.RestrictionsSummary;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.EnvFilter;
import software.wings.security.EnvFilter.EnvFilterBuilder;
import software.wings.security.EnvFilter.FilterType;
import software.wings.security.Filter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserRestrictionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.WorkflowFilter;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.RestrictionsAndAppEnvMap.RestrictionsAndAppEnvMapBuilder;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;
import software.wings.settings.UsageRestrictionsReferenceSummary;
import software.wings.settings.UsageRestrictionsReferenceSummary.IdNameReference;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * @author rktummala on 06/10/18
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class UsageRestrictionsServiceImpl implements UsageRestrictionsService {
  @Inject private AuthHandler authHandler;
  @Inject private UserGroupService userGroupService;
  @Inject private UserService userService;
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;

  @Inject
  public UsageRestrictionsServiceImpl(AuthHandler authHandler, UserGroupService userGroupService, AppService appService,
      EnvironmentService environmentService, SettingsService settingsService, SecretManager secretManager,
      WingsPersistence wingsPersistence, UserService userService) {
    this.authHandler = authHandler;
    this.userGroupService = userGroupService;
    this.appService = appService;
    this.environmentService = environmentService;
    this.settingsService = settingsService;
    this.secretManager = secretManager;
    this.wingsPersistence = wingsPersistence;
    this.userService = userService;
  }

  @Override
  public boolean hasAccess(String accountId, boolean isAccountAdmin, String appIdFromRequest, String envIdFromRequest,
      UsageRestrictions entityUsageRestrictions, UsageRestrictions restrictionsFromUserPermissions,
      Map<String, Set<String>> appEnvMapFromPermissions, Map<String, List<Base>> appIdEnvMap) {
    boolean hasNoRestrictions = hasNoRestrictions(entityUsageRestrictions);
    if (isNotEmpty(appIdFromRequest) && !appIdFromRequest.equals(GLOBAL_APP_ID)) {
      if (hasNoRestrictions) {
        return false;
      }

      Map<String, Set<String>> appEnvMapFromEntityRestrictions =
          getAppEnvMap(entityUsageRestrictions.getAppEnvRestrictions(), appIdEnvMap);
      if (isNotEmpty(envIdFromRequest)) {
        // Restrict it to both app and env
        Set<String> envIds = appEnvMapFromEntityRestrictions.get(appIdFromRequest);

        if (isEmpty(envIds)) {
          return false;
        }

        return envIds.contains(envIdFromRequest);
      } else {
        // Restrict it to app
        return appEnvMapFromEntityRestrictions.containsKey(appIdFromRequest);
      }
    } else {
      User user = UserThreadLocal.get();

      if (user == null) {
        return true;
      }

      if (hasNoRestrictions) {
        return isAccountAdmin;
      }

      Map<String, Set<String>> appEnvMapFromEntityRestrictions =
          getAppEnvMap(entityUsageRestrictions.getAppEnvRestrictions(), appIdEnvMap);
      return hasAccess(isAccountAdmin, appEnvMapFromEntityRestrictions, entityUsageRestrictions,
          appEnvMapFromPermissions, restrictionsFromUserPermissions);
    }
  }

  @Override
  public boolean hasNoRestrictions(UsageRestrictions usageRestrictions) {
    // Observed some entities having empty usage restrictions. Covering that case.
    // Could have been due to a ui bug at some point.
    return usageRestrictions == null || isEmpty(usageRestrictions.getAppEnvRestrictions());
  }

  private boolean hasAllEnvAccess(UsageRestrictions usageRestrictions) {
    return hasAllEnvAccessOfType(usageRestrictions, FilterType.PROD)
        && hasAllEnvAccessOfType(usageRestrictions, FilterType.NON_PROD);
  }

  private boolean hasAccess(boolean isAccountAdmin, Map<String, Set<String>> appEnvMapFromEntityRestrictions,
      UsageRestrictions entityUsageRestrictions, Map<String, Set<String>> appEnvMapFromPermissions,
      UsageRestrictions restrictionsFromUserPermissions) {
    if (hasAllEnvAccess(entityUsageRestrictions)) {
      return true;
    }

    if (hasNoRestrictions(entityUsageRestrictions)) {
      return isAccountAdmin;
    }

    if (isEmpty(appEnvMapFromPermissions) || restrictionsFromUserPermissions == null
        || isEmpty(restrictionsFromUserPermissions.getAppEnvRestrictions())) {
      return false;
    }

    UsageRestrictions entityUsageRestrictionsFinal = entityUsageRestrictions;
    // We want to first check if the restrictions from user permissions is not null
    if (isEmpty(appEnvMapFromEntityRestrictions)) {
      return hasAnyCommonEnv(entityUsageRestrictions, restrictionsFromUserPermissions);
    }

    return appEnvMapFromEntityRestrictions.entrySet().stream().anyMatch(
        (Entry<String, Set<String>> appEnvEntryOfEntity) -> {
          String appId = appEnvEntryOfEntity.getKey();

          if (!appEnvMapFromPermissions.containsKey(appId)) {
            return false;
          }

          Set<String> envIdsFromRestrictions = appEnvEntryOfEntity.getValue();
          if (isEmpty(envIdsFromRestrictions)) {
            return hasAnyCommonEnv(appId, entityUsageRestrictionsFinal, restrictionsFromUserPermissions);
          }

          Set<String> envIdsOfUser = appEnvMapFromPermissions.get(appId);
          if (isEmpty(envIdsOfUser)) {
            return false;
          }

          return envIdsFromRestrictions.stream().anyMatch(envIdsOfUser::contains);
        });
  }

  private boolean hasAnyCommonEnv(
      String appId, UsageRestrictions restrictionsFromEntity, UsageRestrictions restrictionsFromUserPermissions) {
    return (hasAllEnvAccessOfType(restrictionsFromEntity, appId, FilterType.PROD)
               && hasAllEnvAccessOfType(restrictionsFromUserPermissions, appId, FilterType.PROD))
        || (hasAllEnvAccessOfType(restrictionsFromEntity, appId, FilterType.NON_PROD)
               && hasAllEnvAccessOfType(restrictionsFromUserPermissions, appId, FilterType.NON_PROD));
  }

  private boolean hasAnyCommonEnv(
      UsageRestrictions restrictionsFromEntity, UsageRestrictions restrictionsFromUserPermissions) {
    return (hasAllEnvAccessOfType(restrictionsFromEntity, FilterType.PROD)
               && hasAllEnvAccessOfType(restrictionsFromUserPermissions, FilterType.PROD))
        || (hasAllEnvAccessOfType(restrictionsFromEntity, FilterType.NON_PROD)
               && hasAllEnvAccessOfType(restrictionsFromUserPermissions, FilterType.NON_PROD));
  }

  private boolean hasAllCommonEnv(
      String appId, UsageRestrictions restrictionsFromEntity, UsageRestrictions restrictionsFromUserPermissions) {
    boolean hasAllProdRestrictions = hasAllEnvAccessOfType(restrictionsFromEntity, appId, FilterType.PROD);
    boolean hasAllProdPermissions = hasAllEnvAccessOfType(restrictionsFromUserPermissions, appId, FilterType.PROD);
    boolean hasAllNonProdRestrictions = hasAllEnvAccessOfType(restrictionsFromEntity, appId, FilterType.NON_PROD);
    boolean hasAllNonProdPermissions =
        hasAllEnvAccessOfType(restrictionsFromUserPermissions, appId, FilterType.NON_PROD);

    return hasAllAccess(
        hasAllProdPermissions, hasAllProdRestrictions, hasAllNonProdPermissions, hasAllNonProdRestrictions);
  }

  private boolean hasAllCommonEnv(
      UsageRestrictions restrictionsFromEntity, UsageRestrictions restrictionsFromUserPermissions) {
    boolean hasAllProdRestrictions = hasAllEnvAccessOfType(restrictionsFromEntity, FilterType.PROD);
    boolean hasAllProdPermissions = hasAllEnvAccessOfType(restrictionsFromUserPermissions, FilterType.PROD);
    boolean hasAllNonProdRestrictions = hasAllEnvAccessOfType(restrictionsFromEntity, FilterType.NON_PROD);
    boolean hasAllNonProdPermissions = hasAllEnvAccessOfType(restrictionsFromUserPermissions, FilterType.NON_PROD);

    return hasAllAccess(
        hasAllProdPermissions, hasAllProdRestrictions, hasAllNonProdPermissions, hasAllNonProdRestrictions);
  }

  private boolean hasAllAccess(boolean hasAllProdPermissions, boolean hasAllProdRestrictions,
      boolean hasAllNonProdPermissions, boolean hasAllNonProdRestrictions) {
    boolean hasAllAccess = false;
    if (hasAllProdRestrictions) {
      if (!hasAllProdPermissions) {
        return false;
      }
      hasAllAccess = true;
    }

    if (hasAllNonProdRestrictions) {
      if (!hasAllNonProdPermissions) {
        return false;
      }
      hasAllAccess = true;
    }

    return hasAllAccess;
  }

  @Override
  public Map<String, Set<String>> getAppEnvMapFromUserPermissions(
      String accountId, UserPermissionInfo userPermissionInfo, Action action) {
    Map<String, Set<String>> appEnvMap = new HashMap<>();

    Map<String, AppPermissionSummary> appPermissionMap = userPermissionInfo.getAppPermissionMapInternal();

    if (isEmpty(appPermissionMap)) {
      return appEnvMap;
    }

    Set<Entry<String, AppPermissionSummary>> entrySet = appPermissionMap.entrySet();

    if (isEmpty(entrySet)) {
      return appEnvMap;
    }

    entrySet.forEach(appPermission -> {
      String appId = appPermission.getKey();

      // Multimap is deliberately not used since we want to be able to insert the key with null values.
      Set<String> envSet = appEnvMap.get(appId);
      if (envSet == null) {
        envSet = new HashSet<>();
        appEnvMap.put(appId, envSet);
      }

      Set<String> envSetFinal = envSet;
      AppPermissionSummary appPermissionSummary = appPermission.getValue();

      if (appPermissionSummary == null) {
        return;
      }

      Map<Action, Set<EnvInfo>> envPermissions = appPermissionSummary.getEnvPermissions();
      if (isEmpty(envPermissions)) {
        return;
      }

      Set<EnvInfo> envInfoSet = envPermissions.get(action);

      if (isEmpty(envInfoSet)) {
        return;
      }

      envInfoSet.forEach(envInfo -> envSetFinal.add(envInfo.getEnvId()));
    });

    return appEnvMap;
  }

  @Override
  public Map<String, Set<String>> getAppEnvMap(
      Set<AppEnvRestriction> appEnvRestrictions, Map<String, List<Base>> appIdEnvMap) {
    Map<String, Set<String>> appEnvMap = new HashMap<>();

    if (isEmpty(appEnvRestrictions)) {
      return appEnvMap;
    }

    appEnvRestrictions.forEach(appEnvRestriction -> {
      GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
      Set<String> appSet = getAppIdsByFilter(appFilter, appIdEnvMap.keySet());

      if (isEmpty(appSet)) {
        return;
      }

      EnvFilter envFilter = appEnvRestriction.getEnvFilter();
      appSet.forEach(appId -> {
        Set<String> envIdsByFilter = getEnvIdsByFilter(envFilter, appId, appIdEnvMap);
        // Multimap is deliberately not used since we want to be able to insert the key with null values.
        Set<String> valueSet = appEnvMap.computeIfAbsent(appId, k -> new HashSet<>());

        if (!isEmpty(envIdsByFilter)) {
          valueSet.addAll(envIdsByFilter);
        }
      });
    });

    return appEnvMap;
  }

  private Set<String> getAppIdsByFilter(GenericEntityFilter appFilter, Set<String> allAppIdsInAccount) {
    Set<String> appSet;
    switch (appFilter.getFilterType()) {
      case GenericEntityFilter.FilterType.ALL:
        appSet = new HashSet<>(allAppIdsInAccount);
        break;
      case GenericEntityFilter.FilterType.SELECTED:
        appSet = appFilter.getIds();
        break;
      default:
        throw new WingsException("Unsupported app filter type" + appFilter.getFilterType());
    }

    return appSet;
  }

  private Set<String> getEnvIdsByFilter(EnvFilter envFilter, String appId, Map<String, List<Base>> appIdEnvMap) {
    Set<String> envSet = new HashSet<>();
    if (envFilter == null) {
      return envSet;
    }

    Set<String> filterTypes = envFilter.getFilterTypes();

    if (isEmpty(filterTypes)) {
      return envSet;
    }

    List<Base> envList = appIdEnvMap.get(appId);
    if (isEmpty(envList)) {
      return envSet;
    }

    if (filterTypes.contains(FilterType.PROD) && filterTypes.contains(FilterType.NON_PROD)) {
      return envList.stream().map(Base::getUuid).collect(Collectors.toSet());
    }

    filterTypes.forEach(filterType -> {
      switch (filterType) {
        case FilterType.PROD:
          Set<String> prodEnvSet = envList.stream()
                                       .filter(env -> ((Environment) env).getEnvironmentType() == EnvironmentType.PROD)
                                       .map(Base::getUuid)
                                       .collect(Collectors.toSet());
          envSet.addAll(prodEnvSet);
          break;
        case FilterType.NON_PROD:
          Set<String> nonProdEnvSet =
              envList.stream()
                  .filter(env -> ((Environment) env).getEnvironmentType() == EnvironmentType.NON_PROD)
                  .map(Base::getUuid)
                  .collect(Collectors.toSet());
          envSet.addAll(nonProdEnvSet);
          break;
        case FilterType.SELECTED:
          envSet.addAll(envFilter.getIds());
          break;
        default:
          throw new WingsException("Unsupported env filter type" + envFilter.getFilterTypes());
      }
    });

    return envSet;
  }

  private boolean hasUserContext() {
    User user = UserThreadLocal.get();
    return user != null && user.getUserRequestContext() != null;
  }

  @Override
  public UsageRestrictions getUsageRestrictionsFromUserPermissions(Action action, List<UserGroup> userGroupList) {
    Set<AppEnvRestriction> appEnvRestrictions = Sets.newHashSet();

    userGroupList.forEach(userGroup -> {
      Set<AppPermission> appPermissions = userGroup.getAppPermissions();
      if (isEmpty(appPermissions)) {
        return;
      }

      appPermissions.forEach(appPermission -> {
        PermissionType permissionType = appPermission.getPermissionType();

        Set<Action> actions = appPermission.getActions();
        if (!actions.contains(action)) {
          return;
        }

        GenericEntityFilter appFilter = appPermission.getAppFilter();
        if (permissionType == PermissionType.ALL_APP_ENTITIES) {
          EnvFilter prodEntityFilter = EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.PROD)).build();
          AppEnvRestriction prodAppEnvRestriction =
              AppEnvRestriction.builder().appFilter(appFilter).envFilter(prodEntityFilter).build();
          appEnvRestrictions.add(prodAppEnvRestriction);

          EnvFilter nonProdEntityFilter = EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.NON_PROD)).build();
          AppEnvRestriction nonProdAppEnvRestriction =
              AppEnvRestriction.builder().appFilter(appFilter).envFilter(nonProdEntityFilter).build();
          appEnvRestrictions.add(nonProdAppEnvRestriction);

        } else {
          Filter entityFilter = appPermission.getEntityFilter();
          if (!(entityFilter instanceof EnvFilter)) {
            return;
          }

          if (entityFilter instanceof WorkflowFilter) {
            entityFilter = getEnvFilterFromWorkflowFilter((WorkflowFilter) entityFilter);
          }

          AppEnvRestriction appEnvRestriction =
              AppEnvRestriction.builder().appFilter(appFilter).envFilter((EnvFilter) entityFilter).build();
          appEnvRestrictions.add(appEnvRestriction);
        }
      });
    });

    if (isEmpty(appEnvRestrictions)) {
      return null;
    }

    return UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictions).build();
  }

  public Set<EnvFilter> getEnvFilterForApp(String accountId, String appId) {
    if (!hasUserContext()) {
      return null;
    }

    User user = UserThreadLocal.get();
    UserRequestContext userRequestContext = user.getUserRequestContext();

    Set<EnvFilter> envFilters = Sets.newHashSet();

    List<UserGroup> userGroupsByAccountId =
        userGroupService.getUserGroupsByAccountId(userRequestContext.getAccountId(), user);
    userGroupsByAccountId.forEach(userGroup -> {
      Set<AppPermission> appPermissions = userGroup.getAppPermissions();
      if (isEmpty(appPermissions)) {
        return;
      }

      appPermissions.forEach(appPermission -> {
        PermissionType permissionType = appPermission.getPermissionType();

        Set<Action> actions = appPermission.getActions();
        if (!actions.contains(Action.UPDATE)) {
          return;
        }

        GenericEntityFilter appFilter = appPermission.getAppFilter();
        Set<String> appIdsByFilter = authHandler.getAppIdsByFilter(accountId, appFilter);
        if (isEmpty(appIdsByFilter)) {
          return;
        }

        if (!appIdsByFilter.contains(appId)) {
          return;
        }

        Filter entityFilter;
        if (permissionType == PermissionType.ALL_APP_ENTITIES) {
          entityFilter = EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.PROD, FilterType.NON_PROD)).build();
        } else {
          entityFilter = appPermission.getEntityFilter();
          if (!(entityFilter instanceof EnvFilter)) {
            return;
          }
        }

        if (entityFilter instanceof WorkflowFilter) {
          entityFilter = getEnvFilterFromWorkflowFilter((WorkflowFilter) entityFilter);
        }

        envFilters.add((EnvFilter) entityFilter);
      });
    });

    return envFilters;
  }

  private EnvFilter getEnvFilterFromWorkflowFilter(WorkflowFilter workflowFilter) {
    EnvFilterBuilder envFilterBuilder = EnvFilter.builder();
    Set<String> envFilterTypes = Sets.newHashSet();

    if (isEmpty(workflowFilter.getFilterTypes())) {
      return envFilterBuilder.build();
    }

    workflowFilter.getFilterTypes().forEach(filterType -> {
      if (filterType.equals(WorkflowFilter.FilterType.TEMPLATES)) {
        return;
      }

      envFilterTypes.add(filterType);
      if (filterType.equals(EnvFilter.FilterType.SELECTED)) {
        envFilterBuilder.ids(workflowFilter.getIds());
      }
    });
    envFilterBuilder.filterTypes(envFilterTypes);
    return envFilterBuilder.build();
  }

  @Override
  public RestrictionsSummary listAppsWithEnvUpdatePermissions(String accountId) {
    RestrictionsAndAppEnvMap restrictionsAndAppEnvMap = getRestrictionsAndAppEnvMapFromCache(accountId, Action.UPDATE);
    UsageRestrictions usageRestrictionsFromUserPermissions = restrictionsAndAppEnvMap.getUsageRestrictions();

    Set<AppRestrictionsSummary> appRestrictionsSummarySet = Sets.newHashSet();
    if (usageRestrictionsFromUserPermissions == null) {
      return RestrictionsSummary.builder()
          .hasAllAppAccess(false)
          .hasAllNonProdEnvAccess(false)
          .hasAllProdEnvAccess(false)
          .applications(appRestrictionsSummarySet)
          .build();
    }

    PageResponse<Application> pageResponse = appService.list(PageRequestBuilder.aPageRequest()
                                                                 .addFilter("accountId", EQ, accountId)
                                                                 .addFieldsIncluded("_id", "name")
                                                                 .build(),
        false, false, null);

    Map<String, String> appMap =
        pageResponse.getResponse().stream().collect(Collectors.toMap(Base::getUuid, Application::getName));

    if (isEmpty(appMap)) {
      return RestrictionsSummary.builder()
          .hasAllAppAccess(true)
          .hasAllNonProdEnvAccess(true)
          .hasAllProdEnvAccess(true)
          .applications(emptySet())
          .build();
    }

    PageResponse<Environment> envPageResponse =
        environmentService.list(PageRequestBuilder.aPageRequest()
                                    .addFilter("appId", Operator.IN, appMap.keySet().toArray(new String[0]))
                                    .addFieldsIncluded("_id", "name")
                                    .build(),
            false, null);

    Map<String, String> envMap =
        envPageResponse.getResponse().stream().collect(Collectors.toMap(Base::getUuid, Environment::getName));

    Map<String, Set<String>> appEnvMapOfUser = restrictionsAndAppEnvMap.getAppEnvMap();

    boolean hasAllAppAccess = hasAllAppAccess(usageRestrictionsFromUserPermissions);
    boolean hasAllProdEnvAccessForAllApps =
        hasAllEnvAccessOfType(usageRestrictionsFromUserPermissions, FilterType.PROD);
    boolean hasAllNonProdEnvAccessForAllApps =
        hasAllEnvAccessOfType(usageRestrictionsFromUserPermissions, FilterType.NON_PROD);

    appEnvMapOfUser.forEach((key, value) -> {
      String appId = key;
      boolean hasAllProdEnvAccess = hasAllEnvAccessOfType(usageRestrictionsFromUserPermissions, appId, FilterType.PROD);
      boolean hasAllNonProdEnvAccess =
          hasAllEnvAccessOfType(usageRestrictionsFromUserPermissions, appId, FilterType.NON_PROD);
      Set<EntityReference> envSet = Sets.newHashSet();
      value.forEach(
          envId -> envSet.add(EntityReference.builder().name(envMap.get(envId)).id(envId).appId(appId).build()));

      AppRestrictionsSummary appRestrictionsSummary = AppRestrictionsSummary.builder()
                                                          .application(EntityReference.builder()
                                                                           .id(appId)
                                                                           .name(appMap.get(appId))
                                                                           .entityType(EntityType.APPLICATION.name())
                                                                           .appId(appId)
                                                                           .build())
                                                          .environments(envSet)
                                                          .hasAllNonProdEnvAccess(hasAllNonProdEnvAccess)
                                                          .hasAllProdEnvAccess(hasAllProdEnvAccess)
                                                          .build();
      appRestrictionsSummarySet.add(appRestrictionsSummary);
    });
    return RestrictionsSummary.builder()
        .hasAllAppAccess(hasAllAppAccess)
        .hasAllNonProdEnvAccess(hasAllNonProdEnvAccessForAllApps)
        .hasAllProdEnvAccess(hasAllProdEnvAccessForAllApps)
        .applications(appRestrictionsSummarySet)
        .build();
  }

  static boolean hasAllEnvAccessOfType(UsageRestrictions usageRestrictions, String appId, String envType) {
    if (usageRestrictions == null) {
      return false;
    }

    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();
    if (isEmpty(appEnvRestrictions)) {
      return false;
    }

    return appEnvRestrictions.stream().anyMatch(appEnvRestriction -> {
      GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
      EnvFilter envFilter = appEnvRestriction.getEnvFilter();
      if (appFilter == null || appFilter.getFilterType() == null || envFilter == null
          || isEmpty(envFilter.getFilterTypes())) {
        return false;
      } else {
        return (appFilter.getFilterType().equals(GenericEntityFilter.FilterType.ALL)
                   || (appFilter.getIds() != null && appFilter.getIds().contains(appId)))
            && envFilter.getFilterTypes().contains(envType);
      }
    });
  }

  private boolean hasAllEnvAccessOfType(UsageRestrictions usageRestrictions, String envType) {
    return hasAllEnvAccessOfTypes(usageRestrictions, Sets.newHashSet(envType));
  }

  private boolean hasAllEnvAccessOfTypes(UsageRestrictions usageRestrictions, Set<String> envTypes) {
    if (usageRestrictions == null) {
      return false;
    }

    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();
    if (isEmpty(appEnvRestrictions)) {
      return false;
    }

    return appEnvRestrictions.stream().anyMatch(appEnvRestriction -> {
      if (!appEnvRestriction.getAppFilter().getFilterType().equals(GenericEntityFilter.FilterType.ALL)) {
        return false;
      }
      return envTypes.stream().allMatch(envType -> appEnvRestriction.getEnvFilter().getFilterTypes().contains(envType));
    });
  }

  private boolean hasAllAppAccess(UsageRestrictions usageRestrictions) {
    if (usageRestrictions == null) {
      return false;
    }

    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();
    if (isEmpty(appEnvRestrictions)) {
      return false;
    }

    return appEnvRestrictions.stream().anyMatch(appEnvRestriction
        -> appEnvRestriction.getAppFilter().getFilterType().equals(GenericEntityFilter.FilterType.ALL));
  }

  @Override
  public UsageRestrictions getUsageRestrictionsFromJson(String usageRestrictionsString) {
    // TODO use a bean param instead. It wasn't working for some reason.
    if (EmptyPredicate.isNotEmpty(usageRestrictionsString)) {
      try {
        return JsonUtils.asObject(usageRestrictionsString, UsageRestrictions.class);
      } catch (Exception ex) {
        throw new WingsException("Invalid usage restrictions", ex);
      }
    }
    return null;
  }

  @Override
  public UsageRestrictions getDefaultUsageRestrictions(String accountId, String appId, String envId) {
    Set<AppEnvRestriction> appEnvRestrictions = Sets.newHashSet();
    if (isNotEmpty(appId)) {
      GenericEntityFilter appFilter = GenericEntityFilter.builder()
                                          .filterType(GenericEntityFilter.FilterType.SELECTED)
                                          .ids(Sets.newHashSet(appId))
                                          .build();
      if (isNotEmpty(envId)) {
        AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder()
                                                  .appFilter(appFilter)
                                                  .envFilter(EnvFilter.builder()
                                                                 .filterTypes(Sets.newHashSet(FilterType.SELECTED))
                                                                 .ids(Sets.newHashSet(envId))
                                                                 .build())
                                                  .build();
        appEnvRestrictions.add(appEnvRestriction);
      } else {
        Set<EnvFilter> envFilters = getEnvFilterForApp(accountId, appId);
        if (isEmpty(envFilters)) {
          return null;
        }
        envFilters.forEach(envFilter -> {
          AppEnvRestriction appEnvRestriction =
              AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
          appEnvRestrictions.add(appEnvRestriction);
        });
      }

      return UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictions).build();
    } else {
      return getRestrictionsAndAppEnvMapFromCache(accountId, Action.UPDATE).getUsageRestrictions();
    }
  }

  @Override
  public boolean userHasPermissionsToChangeEntity(
      String accountId, UsageRestrictions entityUsageRestrictions, UsageRestrictions restrictionsFromUserPermissions) {
    if (!hasUserContext()) {
      return true;
    }

    Set<String> appIdsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, List<Base>> appIdEnvMap = environmentService.getAppIdEnvMap(appIdsByAccountId);
    return userHasPermissions(accountId, entityUsageRestrictions, restrictionsFromUserPermissions, appIdEnvMap);
  }

  @Override
  public boolean userHasPermissionsToChangeEntity(String accountId, UsageRestrictions entityUsageRestrictions,
      UsageRestrictions restrictionsFromUserPermissions, Map<String, List<Base>> appIdEnvMap) {
    if (!hasUserContext()) {
      return true;
    }

    return userHasPermissions(accountId, entityUsageRestrictions, restrictionsFromUserPermissions, appIdEnvMap);
  }

  @Override
  public boolean userHasPermissionsToChangeEntity(String accountId, UsageRestrictions entityUsageRestrictions) {
    Set<String> appIdsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, List<Base>> appIdEnvMap = environmentService.getAppIdEnvMap(appIdsByAccountId);
    return userHasPermissionsToChangeEntity(accountId, entityUsageRestrictions, appIdEnvMap);
  }

  @Override
  public boolean userHasPermissionsToChangeEntity(
      String accountId, UsageRestrictions entityUsageRestrictions, Map<String, List<Base>> appIdEnvMap) {
    return userHasPermissionsToChangeEntity(accountId, entityUsageRestrictions,
        getRestrictionsAndAppEnvMapFromCache(accountId, Action.UPDATE).getUsageRestrictions(), appIdEnvMap);
  }

  private boolean userHasPermissions(String accountId, UsageRestrictions entityUsageRestrictions,
      UsageRestrictions restrictionsFromUserPermissions, Map<String, List<Base>> appIdEnvMap) {
    // If no restrictions, only account admin can modify it
    if (hasNoRestrictions(entityUsageRestrictions)) {
      return userService.isAccountAdmin(accountId);
    }

    if (hasNoRestrictions(restrictionsFromUserPermissions)) {
      return false;
    }

    Map<String, Set<String>> appEnvMapFromUserPermissions =
        getRestrictionsAndAppEnvMapFromCache(accountId, Action.UPDATE).getAppEnvMap();

    Map<String, Set<String>> appEnvMapFromEntityRestrictions =
        getAppEnvMap(entityUsageRestrictions.getAppEnvRestrictions(), appIdEnvMap);

    if (isEmpty(appEnvMapFromEntityRestrictions)) {
      return hasAllCommonEnv(entityUsageRestrictions, restrictionsFromUserPermissions);
    }

    UsageRestrictions entityUsageRestrictionsFinal = entityUsageRestrictions;
    return appEnvMapFromEntityRestrictions.entrySet().stream().allMatch(
        (Entry<String, Set<String>> appEnvEntryOfEntity) -> {
          String appId = appEnvEntryOfEntity.getKey();

          if (!appEnvMapFromUserPermissions.containsKey(appId)) {
            return false;
          }

          Set<String> envIdsFromRestrictions = appEnvEntryOfEntity.getValue();
          if (isEmpty(envIdsFromRestrictions)) {
            return hasAllCommonEnv(appId, entityUsageRestrictionsFinal, restrictionsFromUserPermissions);
          }

          Set<String> envIdsFromUserPermissions = appEnvMapFromUserPermissions.get(appId);
          if (isEmpty(envIdsFromUserPermissions)) {
            return false;
          }

          return envIdsFromRestrictions.stream().allMatch(envIdsFromUserPermissions::contains);
        });
  }

  /**
   * Checks if user can create / update an entity without any usage restrictions. Only users with Account Manager
   * permission or (All Apps - All Envs) permissions can do it.
   * @param accountId
   * @param usageRestrictions usage restrictions
   * @param restrictionsFromUserPermissions
   * @return boolean
   */
  private boolean checkIfUserCanSetWithNoUsageRestrictions(
      String accountId, UsageRestrictions usageRestrictions, UsageRestrictions restrictionsFromUserPermissions) {
    if (!hasNoRestrictions(usageRestrictions)) {
      return true;
    }
    if (userService.isAccountAdmin(accountId)) {
      return true;
    }

    return hasAllEnvAccess(restrictionsFromUserPermissions);
  }

  @Override
  public void validateUsageRestrictionsOnEntitySave(String accountId, UsageRestrictions usageRestrictions) {
    checkIfValidUsageRestrictions(usageRestrictions);

    if (!hasUserContext()) {
      return;
    }

    UsageRestrictions restrictionsFromUserPermissions =
        getRestrictionsAndAppEnvMapFromCache(accountId, Action.UPDATE).getUsageRestrictions();
    boolean allowed =
        checkIfUserCanSetWithNoUsageRestrictions(accountId, usageRestrictions, restrictionsFromUserPermissions);

    if (!allowed) {
      throw new WingsException(ErrorCode.NOT_ACCOUNT_MGR_NOR_HAS_ALL_APP_ACCESS, USER);
    }

    Set<String> appIdsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, List<Base>> appIdEnvMap = environmentService.getAppIdEnvMap(appIdsByAccountId);
    boolean canUpdateEntity =
        userHasPermissionsToChangeEntity(accountId, usageRestrictions, restrictionsFromUserPermissions, appIdEnvMap);

    if (!canUpdateEntity) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, USER);
    }
  }

  @Override
  public void validateUsageRestrictionsOnEntityUpdate(
      String accountId, UsageRestrictions oldUsageRestrictions, UsageRestrictions newUsageRestrictions) {
    checkIfValidUsageRestrictions(newUsageRestrictions);

    if (!hasUserContext()) {
      return;
    }

    UsageRestrictions restrictionsFromUserPermissions =
        getRestrictionsAndAppEnvMapFromCache(accountId, Action.UPDATE).getUsageRestrictions();
    boolean allowed =
        checkIfUserCanSetWithNoUsageRestrictions(accountId, newUsageRestrictions, restrictionsFromUserPermissions);

    if (!allowed) {
      throw new WingsException(ErrorCode.NOT_ACCOUNT_MGR_NOR_HAS_ALL_APP_ACCESS, USER);
    }

    boolean canUpdateEntity =
        userHasPermissionsToChangeEntity(accountId, oldUsageRestrictions, restrictionsFromUserPermissions);

    if (!canUpdateEntity) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, USER);
    }
  }

  @Override
  public void validateSetupUsagesOnUsageRestrictionsUpdate(
      String accountId, Map<String, Set<String>> setupUsages, UsageRestrictions newUsageRestrictions) {
    Set<String> appsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, Set<String>> newAppEnvMap = newUsageRestrictions == null
        ? new HashMap<>()
        : getAppEnvMap(
              newUsageRestrictions.getAppEnvRestrictions(), environmentService.getAppIdEnvMap(appsByAccountId));

    for (Entry<String, Set<String>> setupUsage : setupUsages.entrySet()) {
      String appId = setupUsage.getKey();

      if (!newAppEnvMap.containsKey(appId)) {
        Application referredApplication = appService.get(appId);
        String appName = referredApplication == null ? "" : referredApplication.getName();
        String errorMessage =
            "Can't update usage scope, application '" + appName + "' is still referencing this secret.";
        throw new WingsException(errorMessage, USER);
      }

      Set<String> setupEnvIds = setupUsage.getValue();
      if (isEmpty(setupEnvIds)) {
        continue;
      }

      Set<String> envIdsFromUsageRestrictions = newAppEnvMap.get(appId);
      setupEnvIds.removeAll(envIdsFromUsageRestrictions);

      if (!setupEnvIds.isEmpty()) {
        Environment environment = environmentService.get(appId, setupEnvIds.iterator().next());
        String envName = environment == null ? "" : environment.getName();
        String errorMessage =
            "Can't update usage scope, environment '" + envName + "' is still referencing this secret.";
        throw new WingsException(errorMessage, USER);
      }
    }
  }

  @Override
  public RestrictionsAndAppEnvMap getRestrictionsAndAppEnvMapFromCache(String accountId, Action action) {
    RestrictionsAndAppEnvMapBuilder builder = RestrictionsAndAppEnvMap.builder();

    if (action == null) {
      return builder.build();
    }

    User user = UserThreadLocal.get();

    if (user == null) {
      return builder.build();
    }

    UserRequestContext userRequestContext = user.getUserRequestContext();

    if (userRequestContext == null) {
      return builder.build();
    }

    UserRestrictionInfo userRestrictionInfo = userRequestContext.getUserRestrictionInfo();

    if (userRestrictionInfo == null) {
      return builder.build();
    }

    switch (action) {
      case READ:
        builder.appEnvMap(userRestrictionInfo.getAppEnvMapForReadAction());
        builder.usageRestrictions(userRestrictionInfo.getUsageRestrictionsForReadAction());
        break;
      case UPDATE:
        builder.appEnvMap(userRestrictionInfo.getAppEnvMapForUpdateAction());
        builder.usageRestrictions(userRestrictionInfo.getUsageRestrictionsForUpdateAction());
        break;
      default:
        logger.error("Invalid action {} for restrictions", action);
        break;
    }

    return builder.build();
  }

  @Override
  public boolean isEditable(String accountId, String entityId, String entityType) {
    if (SettingVariableTypes.SECRET_TEXT.name().equals(entityType)
        || SettingVariableTypes.CONFIG_FILE.name().equals(entityType)) {
      EncryptedData encryptedData = secretManager.getSecretById(accountId, entityId);

      if (encryptedData == null) {
        return false;
      }
      return userHasPermissionsToChangeEntity(accountId, encryptedData.getUsageRestrictions());

    } else {
      SettingAttribute settingAttribute = settingsService.get(entityId);
      if (settingAttribute == null || !accountId.equals(settingAttribute.getAccountId())) {
        return false;
      }
      return userHasPermissionsToChangeEntity(accountId, settingAttribute.getUsageRestrictions());
    }
  }

  @Override
  public UsageRestrictionsReferenceSummary getReferenceSummaryForApp(String accountId, String appId) {
    Set<IdNameReference> settings = new LinkedHashSet<>();
    Set<IdNameReference> secrets = new LinkedHashSet<>();

    try (HIterator<SettingAttribute> iterator = getSettingAttributesWithUsageRestrictionsIterator(accountId)) {
      for (SettingAttribute settingAttribute : iterator) {
        UsageRestrictions usageRestrictions = settingAttribute.getUsageRestrictions();
        for (AppEnvRestriction appEnvRestriction : usageRestrictions.getAppEnvRestrictions()) {
          GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
          if (FilterType.SELECTED.equals(appFilter.getFilterType()) && appFilter.getIds().contains(appId)) {
            settings.add(
                IdNameReference.builder().id(settingAttribute.getUuid()).name(settingAttribute.getName()).build());
          }
        }
      }
    }

    try (HIterator<EncryptedData> iterator = getEncryptedDataWithUsageRestrictionsIterator(accountId)) {
      for (EncryptedData encryptedData : iterator) {
        UsageRestrictions usageRestrictions = encryptedData.getUsageRestrictions();
        for (AppEnvRestriction appEnvRestriction : usageRestrictions.getAppEnvRestrictions()) {
          GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
          if (FilterType.SELECTED.equals(appFilter.getFilterType()) && appFilter.getIds().contains(appId)) {
            secrets.add(IdNameReference.builder().id(encryptedData.getUuid()).name(encryptedData.getName()).build());
          }
        }
      }
    }

    int numOfSettings = settings.size();
    int numOfSecrets = secrets.size();
    return UsageRestrictionsReferenceSummary.builder()
        .total(numOfSettings + numOfSecrets)
        .numOfSettings(numOfSettings)
        .numOfSecrets(numOfSecrets)
        .settings(settings)
        .secrets(secrets)
        .build();
  }

  @Override
  public UsageRestrictionsReferenceSummary getReferenceSummaryForEnv(String accountId, String envId) {
    Set<IdNameReference> settings = new LinkedHashSet<>();
    Set<IdNameReference> secrets = new LinkedHashSet<>();

    try (HIterator<SettingAttribute> iterator = getSettingAttributesWithUsageRestrictionsIterator(accountId)) {
      for (SettingAttribute settingAttribute : iterator) {
        UsageRestrictions usageRestrictions = settingAttribute.getUsageRestrictions();
        for (AppEnvRestriction appEnvRestriction : usageRestrictions.getAppEnvRestrictions()) {
          EnvFilter envFilter = appEnvRestriction.getEnvFilter();
          if (envFilter.getFilterTypes().contains(FilterType.SELECTED) && envFilter.getIds().contains(envId)) {
            settings.add(
                IdNameReference.builder().id(settingAttribute.getUuid()).name(settingAttribute.getName()).build());
          }
        }
      }
    }

    try (HIterator<EncryptedData> iterator = getEncryptedDataWithUsageRestrictionsIterator(accountId)) {
      for (EncryptedData encryptedData : iterator) {
        UsageRestrictions usageRestrictions = encryptedData.getUsageRestrictions();
        for (AppEnvRestriction appEnvRestriction : usageRestrictions.getAppEnvRestrictions()) {
          EnvFilter envFilter = appEnvRestriction.getEnvFilter();
          if (envFilter.getFilterTypes().contains(FilterType.SELECTED) && envFilter.getIds().contains(envId)) {
            secrets.add(IdNameReference.builder().id(encryptedData.getUuid()).name(encryptedData.getName()).build());
          }
        }
      }
    }

    int numOfSettings = settings.size();
    int numOfSecrets = secrets.size();
    return UsageRestrictionsReferenceSummary.builder()
        .total(numOfSettings + numOfSecrets)
        .numOfSettings(numOfSettings)
        .numOfSecrets(numOfSecrets)
        .settings(settings)
        .secrets(secrets)
        .build();
  }

  @Override
  public int purgeDanglingAppEnvReferences(String accountId) {
    int count = 0;

    Set<String> existingAppIds = appService.getAppIdsAsSetByAccountId(accountId);
    Set<String> existingEnvIds = new HashSet<>();
    for (String appId : existingAppIds) {
      existingEnvIds.addAll(environmentService.getEnvIdsByApp(appId));
    }

    try (HIterator<SettingAttribute> iterator = getSettingAttributesWithUsageRestrictionsIterator(accountId)) {
      for (SettingAttribute settingAttribute : iterator) {
        UsageRestrictions usageRestrictions = settingAttribute.getUsageRestrictions();
        count += purgeDanglingAppEnvReferenceInternal(usageRestrictions, existingAppIds, existingEnvIds);

        // Dangling reference to App/Env has been cleared in the usage restrictions. Then update with the updated usage
        // restrictions.
        if (count > 0) {
          settingsService.updateUsageRestrictionsInternal(settingAttribute.getUuid(), usageRestrictions);
        }
      }
    }

    try (HIterator<EncryptedData> iterator = getEncryptedDataWithUsageRestrictionsIterator(accountId)) {
      for (EncryptedData encryptedData : iterator) {
        UsageRestrictions usageRestrictions = encryptedData.getUsageRestrictions();
        count += purgeDanglingAppEnvReferenceInternal(usageRestrictions, existingAppIds, existingEnvIds);

        // Dangling reference to App/Env has been cleared in the usage restrictions. Then update with the updated usage
        // restrictions.
        if (count > 0) {
          secretManager.updateUsageRestrictionsForSecretOrFile(accountId, encryptedData.getUuid(), usageRestrictions);
        }
      }
    }

    return count;
  }

  @Override
  public int removeAppEnvReferences(String accountId, String appId, String envId) {
    int count = 0;

    try (HIterator<SettingAttribute> iterator = getSettingAttributesWithUsageRestrictionsIterator(accountId)) {
      for (SettingAttribute settingAttribute : iterator) {
        UsageRestrictions usageRestrictions = settingAttribute.getUsageRestrictions();
        count += removeAppEnvReferencesInternal(usageRestrictions, appId, envId);

        // Dangling reference to App/Env has been cleared in the usage restrictions. Then update with the updated usage
        // restrictions.
        if (count > 0) {
          settingsService.updateUsageRestrictionsInternal(settingAttribute.getUuid(), usageRestrictions);
          logger.info("Reference to application {} has been removed in setting attribute {} with id {} in account {}",
              appId, settingAttribute.getName(), settingAttribute.getUuid(), accountId);
        }
      }
    }

    try (HIterator<EncryptedData> iterator = getEncryptedDataWithUsageRestrictionsIterator(accountId)) {
      for (EncryptedData encryptedData : iterator) {
        UsageRestrictions usageRestrictions = encryptedData.getUsageRestrictions();
        count += removeAppEnvReferencesInternal(usageRestrictions, appId, envId);

        // Dangling reference to App/Env has been cleared in the usage restrictions. Then update with the updated usage
        // restrictions.
        if (count > 0) {
          secretManager.updateUsageRestrictionsForSecretOrFile(accountId, encryptedData.getUuid(), usageRestrictions);
          logger.info("Reference to application {} has been removed in encrypted text/file {} with id {} in account {}",
              appId, encryptedData.getName(), encryptedData.getUuid(), accountId);
        }
      }
    }

    return count;
  }

  private HIterator<SettingAttribute> getSettingAttributesWithUsageRestrictionsIterator(String accountId) {
    return new HIterator<>(wingsPersistence.createQuery(SettingAttribute.class)
                               .filter(SettingAttributeKeys.accountId, accountId)
                               .field("usageRestrictions")
                               .exists()
                               .fetch());
  }

  private HIterator<EncryptedData> getEncryptedDataWithUsageRestrictionsIterator(String accountId) {
    return new HIterator<>(wingsPersistence.createQuery(EncryptedData.class)
                               .filter(EncryptedDataKeys.accountId, accountId)
                               .field("usageRestrictions")
                               .exists()
                               .fetch());
  }

  private int removeAppEnvReferencesInternal(UsageRestrictions usageRestrictions, String appId, String envId) {
    int count = 0;

    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();
    if (isEmpty(appEnvRestrictions)) {
      return count;
    }

    Set<AppEnvRestriction> filteredAppEnvRestrictions = new LinkedHashSet<>();
    for (AppEnvRestriction appEnvRestriction : appEnvRestrictions) {
      // When envId is present, don't rely on appId match to remove appEnvRestriction within the usage restrictions.
      if (envId == null) {
        GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
        Set<String> appIds = appFilter.getIds();

        if (GenericEntityFilter.FilterType.SELECTED.equals(appFilter.getFilterType()) && isNotEmpty(appIds)
            && appIds.contains(appId)) {
          appIds.remove(appId);
          count++;

          if (isEmpty(appFilter.getIds())) {
            continue;
          }
        }
      } else {
        EnvFilter envFilter = appEnvRestriction.getEnvFilter();
        Set<String> envIds = envFilter.getIds();
        Set<String> filterTypes = envFilter.getFilterTypes();

        if (filterTypes.contains(EnvFilter.FilterType.SELECTED) && isNotEmpty(envIds) && envIds.contains(envId)) {
          envIds.remove(envId);
          count++;

          if (isEmpty(envFilter.getIds())) {
            continue;
          }
        }
      }

      filteredAppEnvRestrictions.add(appEnvRestriction);
    }

    if (count > 0) {
      usageRestrictions.setAppEnvRestrictions(filteredAppEnvRestrictions);
    }

    return count;
  }

  private int purgeDanglingAppEnvReferenceInternal(
      UsageRestrictions usageRestrictions, Set<String> existingAppIds, Set<String> existingEnvIds) {
    int count = 0;

    Set<AppEnvRestriction> filteredAppEnvRestrictions = new LinkedHashSet<>();
    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();
    for (AppEnvRestriction appEnvRestriction : appEnvRestrictions) {
      GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
      EnvFilter envFilter = appEnvRestriction.getEnvFilter();

      String appId = null;
      if (GenericEntityFilter.FilterType.SELECTED.equals(appFilter.getFilterType()) && isNotEmpty(appFilter.getIds())) {
        appId = appFilter.getIds().iterator().next();
        if (!existingAppIds.contains(appId)) {
          appFilter.getIds().remove(appId);
          count++;
        }

        if (isEmpty(appFilter.getIds())) {
          continue;
        }
      }

      if (envFilter.getFilterTypes().contains(FilterType.SELECTED) && isNotEmpty(envFilter.getIds())) {
        String envId = envFilter.getIds().iterator().next();
        if (appId != null && !existingEnvIds.contains(envId)) {
          envFilter.getIds().remove(envId);
          count++;
        }

        if (isEmpty(envFilter.getIds())) {
          continue;
        }
      }

      filteredAppEnvRestrictions.add(appEnvRestriction);
    }

    if (count > 0) {
      usageRestrictions.setAppEnvRestrictions(filteredAppEnvRestrictions);
    }
    return count;
  }

  private void checkIfValidUsageRestrictions(UsageRestrictions usageRestrictions) {
    if (!hasNoRestrictions(usageRestrictions)) {
      usageRestrictions.getAppEnvRestrictions().forEach(appEnvRestriction -> {
        GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
        if (appFilter == null || appFilter.getFilterType() == null) {
          throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
        }

        String filterType = appFilter.getFilterType();
        if (!GenericEntityFilter.FilterType.isValidFilterType(filterType)) {
          throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
        }

        if (appFilter.getFilterType().equals(GenericEntityFilter.FilterType.SELECTED)) {
          if (isEmpty(appFilter.getIds())) {
            throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
          } else if (appFilter.getIds().stream().anyMatch(EmptyPredicate::isEmpty)) {
            throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
          }
        }

        EnvFilter envFilter = appEnvRestriction.getEnvFilter();
        if (envFilter == null) {
          throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
        }

        Set<String> envFilterTypes = envFilter.getFilterTypes();
        if (envFilter == null || isEmpty(envFilterTypes)) {
          throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
        }

        for (String envFilterType : envFilterTypes) {
          if (!EnvFilter.FilterType.isValidFilterType(envFilterType)) {
            throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
          }
        }

        if (envFilterTypes.contains(FilterType.SELECTED)) {
          if (envFilterTypes.size() != 1) {
            throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
          } else {
            if (isEmpty(envFilter.getIds())) {
              throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
            } else if (envFilter.getIds().stream().anyMatch(EmptyPredicate::isEmpty)) {
              throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
            }
          }
        }
      });
    }
  }
}
