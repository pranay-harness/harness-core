package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.exception.WingsException.USER;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.EntityReference;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.User;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.restrictions.AppRestrictionsSummary;
import software.wings.beans.security.restrictions.RestrictionsSummary;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.security.AccountPermissionSummary;
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
import software.wings.security.UserThreadLocal;
import software.wings.security.WorkflowFilter;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserGroupService;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

import java.util.HashSet;
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
public class UsageRestrictionsServiceImpl implements UsageRestrictionsService {
  private static final Logger logger = LoggerFactory.getLogger(UsageRestrictionsServiceImpl.class);

  @Inject private AuthHandler authHandler;
  @Inject private UserGroupService userGroupService;
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  private Set<AppEnvRestriction> allAppEnvRestrictions = getAllAppEnvRestrictions();

  @Override
  public boolean hasAccess(String accountId, String appIdFromRequest, String envIdFromRequest,
      UsageRestrictions entityUsageRestrictions, Map<String, Set<String>> appEnvMapFromEntityRestrictions,
      UsageRestrictions restrictionsFromUserPermissions, Map<String, Set<String>> appEnvMapFromPermissions) {
    if (appIdFromRequest != null && !appIdFromRequest.equals(GLOBAL_APP_ID)) {
      if (envIdFromRequest != null) {
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

      return hasAccess(accountId, appEnvMapFromEntityRestrictions, entityUsageRestrictions, appEnvMapFromPermissions,
          restrictionsFromUserPermissions);
    }
  }

  private boolean hasNoRestrictions(UsageRestrictions usageRestrictions) {
    return usageRestrictions == null || isEmpty(usageRestrictions.getAppEnvRestrictions());
  }

  private boolean hasAccess(String accountId, Map<String, Set<String>> appEnvMapFromEntityRestrictions,
      UsageRestrictions entityUsageRestrictions, Map<String, Set<String>> appEnvMapFromPermissions,
      UsageRestrictions restrictionsFromUserPermissions) {
    if (hasNoRestrictions(entityUsageRestrictions)
        || (hasAllEnvAccessOfType(entityUsageRestrictions, FilterType.PROD)
               && hasAllEnvAccessOfType(entityUsageRestrictions, FilterType.NON_PROD))) {
      return true;
    }

    if (isEmpty(appEnvMapFromPermissions) || restrictionsFromUserPermissions == null
        || isEmpty(restrictionsFromUserPermissions.getAppEnvRestrictions())) {
      return false;
    }

    if (hasNoRestrictions(entityUsageRestrictions)) {
      entityUsageRestrictions = UsageRestrictions.builder().appEnvRestrictions(allAppEnvRestrictions).build();
      appEnvMapFromEntityRestrictions = getAppEnvMap(accountId, allAppEnvRestrictions);
    }

    final UsageRestrictions entityUsageRestrictionsFinal = entityUsageRestrictions;
    // We want to first check if the restrictions from user permissions is not null
    if (isEmpty(appEnvMapFromEntityRestrictions)) {
      return hasCommonEnv(entityUsageRestrictions, restrictionsFromUserPermissions);
    }

    return appEnvMapFromEntityRestrictions.entrySet().stream().anyMatch(
        (Entry<String, Set<String>> appEnvEntryOfEntity) -> {
          String appId = appEnvEntryOfEntity.getKey();

          if (!appEnvMapFromPermissions.containsKey(appId)) {
            return false;
          }

          Set<String> envIdsFromRestrictions = appEnvEntryOfEntity.getValue();
          if (isEmpty(envIdsFromRestrictions)) {
            return hasCommonEnv(appId, entityUsageRestrictionsFinal, restrictionsFromUserPermissions);
          }

          Set<String> envIdsOfUser = appEnvMapFromPermissions.get(appId);
          if (isEmpty(envIdsOfUser)) {
            return false;
          }

          return envIdsFromRestrictions.stream().anyMatch(
              envIdFromRestriction -> envIdsOfUser.contains(envIdFromRestriction));
        });
  }

  private boolean hasCommonEnv(
      String appId, UsageRestrictions restrictionsFromEntity, UsageRestrictions restrictionsFromUserPermissions) {
    return (hasAllEnvAccessOfType(restrictionsFromEntity, appId, FilterType.PROD)
               && hasAllEnvAccessOfType(restrictionsFromUserPermissions, appId, FilterType.PROD))
        || (hasAllEnvAccessOfType(restrictionsFromEntity, appId, FilterType.NON_PROD)
               && hasAllEnvAccessOfType(restrictionsFromUserPermissions, appId, FilterType.NON_PROD));
  }

  private boolean hasCommonEnv(
      UsageRestrictions restrictionsFromEntity, UsageRestrictions restrictionsFromUserPermissions) {
    return (hasAllEnvAccessOfType(restrictionsFromEntity, FilterType.PROD)
               && hasAllEnvAccessOfType(restrictionsFromUserPermissions, FilterType.PROD))
        || (hasAllEnvAccessOfType(restrictionsFromEntity, FilterType.NON_PROD)
               && hasAllEnvAccessOfType(restrictionsFromUserPermissions, FilterType.NON_PROD));
  }

  @Override
  public Map<String, Set<String>> getAppEnvMapFromPermissions(String accountId) {
    Map<String, Set<String>> appEnvMap = Maps.newHashMap();
    User user = UserThreadLocal.get();
    if (user == null) {
      return appEnvMap;
    }

    if (user.getUserRequestContext() == null) {
      logger.error("User request context is null, returning");
      return appEnvMap;
    }

    UserPermissionInfo userPermissionInfo = user.getUserRequestContext().getUserPermissionInfo();
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

      final Set<String> envSetFinal = envSet;
      AppPermissionSummary appPermissionSummary = appPermission.getValue();

      if (appPermissionSummary == null) {
        return;
      }

      Map<Action, Set<EnvInfo>> envPermissions = appPermissionSummary.getEnvPermissions();
      if (isEmpty(envPermissions)) {
        return;
      }

      Set<EnvInfo> envInfoSet = envPermissions.get(Action.UPDATE);

      if (isEmpty(envInfoSet)) {
        return;
      }

      envInfoSet.forEach(envInfo -> envSetFinal.add(envInfo.getEnvId()));
    });

    return appEnvMap;
  }

  @Override
  public Map<String, Set<String>> getAppEnvMap(String accountId, Set<AppEnvRestriction> appEnvRestrictions) {
    Map<String, Set<String>> appEnvMap = Maps.newHashMap();
    User user = UserThreadLocal.get();
    if (user == null) {
      return appEnvMap;
    }

    if (user.getUserRequestContext() == null) {
      logger.error("User request context is null, returning");
      return appEnvMap;
    }
    UserPermissionInfo userPermissionInfo = user.getUserRequestContext().getUserPermissionInfo();
    if (userPermissionInfo.getAppPermissionMapInternal() == null) {
      return appEnvMap;
    }

    Set<String> appSetFromPermissions = userPermissionInfo.getAppPermissionMapInternal().keySet();

    if (isEmpty(appSetFromPermissions)) {
      return appEnvMap;
    }

    appEnvRestrictions.forEach(appEnvRestriction -> {
      GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
      Set<String> appSet = getAppIdsByFilter(appFilter, appSetFromPermissions);

      if (isEmpty(appSet)) {
        return;
      }

      EnvFilter envFilter = appEnvRestriction.getEnvFilter();
      appSet.forEach(appId -> {
        Set<String> envIdsByFilter =
            getEnvIdsByFilter(envFilter, userPermissionInfo.getAppPermissionMapInternal().get(appId));
        // Multimap is deliberately not used since we want to be able to insert the key with null values.
        Set<String> valueSet = appEnvMap.get(appId);
        if (valueSet == null) {
          valueSet = new HashSet<>();
          appEnvMap.put(appId, valueSet);
        }

        if (!isEmpty(envIdsByFilter)) {
          valueSet.addAll(envIdsByFilter);
        }
      });
    });

    return appEnvMap;
  }

  private Set<String> getAppIdsByFilter(GenericEntityFilter appFilter, Set<String> appSetFromPermissions) {
    Set<String> appSet;
    switch (appFilter.getFilterType()) {
      case GenericEntityFilter.FilterType.ALL:
        appSet = appSetFromPermissions;
        break;
      case GenericEntityFilter.FilterType.SELECTED:
        appSet = Sets.intersection(appSetFromPermissions, appFilter.getIds());
        break;
      default:
        throw new WingsException("Unsupported app filter type" + appFilter.getFilterType());
    }

    return appSet;
  }

  private Set<String> getEnvIdsByFilter(EnvFilter envFilter, AppPermissionSummary appPermissionSummary) {
    Set<String> envSet = new HashSet<>();
    if (appPermissionSummary == null) {
      return envSet;
    }

    Set<String> filterTypes = envFilter.getFilterTypes();

    if (isEmpty(filterTypes)) {
      return envSet;
    }

    Map<Action, Set<EnvInfo>> envActionMap = appPermissionSummary.getEnvPermissions();
    if (isEmpty(envActionMap)) {
      return envSet;
    }

    Set<EnvInfo> envSetFromPermissions = envActionMap.get(Action.UPDATE);

    if (filterTypes.contains(FilterType.PROD) && filterTypes.contains(FilterType.NON_PROD)) {
      return envSetFromPermissions.stream().map(envInfo -> envInfo.getEnvId()).collect(Collectors.toSet());
    }

    filterTypes.forEach(filterType -> {
      switch (filterType) {
        case FilterType.PROD:
          Set<String> envs = envSetFromPermissions.stream()
                                 .filter(envInfo -> FilterType.PROD.equals(envInfo.getEnvType()))
                                 .map(envInfo -> envInfo.getEnvId())
                                 .collect(Collectors.toSet());
          envSet.addAll(envs);
          break;
        case FilterType.NON_PROD:
          envs = envSetFromPermissions.stream()
                     .filter(envInfo -> FilterType.NON_PROD.equals(envInfo.getEnvType()))
                     .map(envInfo -> envInfo.getEnvId())
                     .collect(Collectors.toSet());
          envSet.addAll(envs);
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
    if (user == null || user.getUserRequestContext() == null) {
      return false;
    }
    return true;
  }

  @Override
  public UsageRestrictions getUsageRestrictionsFromUserPermissions(String accountId) {
    if (!hasUserContext()) {
      return null;
    }

    User user = UserThreadLocal.get();
    UserRequestContext userRequestContext = user.getUserRequestContext();

    Set<AppEnvRestriction> appEnvRestrictions = Sets.newHashSet();

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

        Filter entityFilter;
        if (permissionType == PermissionType.ALL_APP_ENTITIES) {
          entityFilter = EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.PROD, FilterType.NON_PROD)).build();
        } else {
          entityFilter = appPermission.getEntityFilter();
          if (!(entityFilter instanceof EnvFilter)) {
            return;
          }
        }

        GenericEntityFilter appFilter = appPermission.getAppFilter();

        if (entityFilter instanceof WorkflowFilter) {
          entityFilter = getEnvFilterFromWorkflowFilter((WorkflowFilter) entityFilter);
        }

        AppEnvRestriction appEnvRestriction =
            AppEnvRestriction.builder().appFilter(appFilter).envFilter((EnvFilter) entityFilter).build();
        appEnvRestrictions.add(appEnvRestriction);
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
    UsageRestrictions usageRestrictionsFromUserPermissions = getUsageRestrictionsFromUserPermissions(accountId);

    if (usageRestrictionsFromUserPermissions == null) {
      throw new WingsException(ErrorCode.USER_HAS_NO_PERMISSIONS);
    }

    PageResponse<Application> pageResponse = appService.list(PageRequestBuilder.aPageRequest()
                                                                 .addFilter("accountId", Operator.EQ, accountId)
                                                                 .addFieldsIncluded("_id", "name")
                                                                 .build(),
        false);

    Map<String, String> appMap =
        pageResponse.getResponse().stream().collect(Collectors.toMap(app -> app.getUuid(), app -> app.getName()));

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
            false);

    Map<String, String> envMap =
        envPageResponse.getResponse().stream().collect(Collectors.toMap(env -> env.getUuid(), env -> env.getName()));

    Map<String, Set<String>> appEnvMapOfUser = getAppEnvMapFromPermissions(accountId);

    boolean hasAllAppAccess = hasAllAppAccess(usageRestrictionsFromUserPermissions);
    boolean hasAllProdEnvAccessForAllApps =
        hasAllEnvAccessOfType(usageRestrictionsFromUserPermissions, FilterType.PROD);
    boolean hasAllNonProdEnvAccessForAllApps =
        hasAllEnvAccessOfType(usageRestrictionsFromUserPermissions, FilterType.NON_PROD);

    Set<AppRestrictionsSummary> appRestrictionsSummarySet = Sets.newHashSet();
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

  private boolean hasAllEnvAccessOfType(UsageRestrictions usageRestrictions, String appId, String envType) {
    if (usageRestrictions == null) {
      return false;
    }

    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();
    if (isEmpty(appEnvRestrictions)) {
      return false;
    }

    return appEnvRestrictions.stream().anyMatch(appEnvRestriction
        -> (appEnvRestriction.getAppFilter().getFilterType().equals(GenericEntityFilter.FilterType.ALL)
               || (appEnvRestriction.getAppFilter().getIds() != null
                      && appEnvRestriction.getAppFilter().getIds().contains(appId)))
            && appEnvRestriction.getEnvFilter().getFilterTypes().contains(envType));
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

      return UsageRestrictions.builder().isEditable(true).appEnvRestrictions(appEnvRestrictions).build();
    } else {
      if (isAccountAdmin(accountId)) {
        return null;
      }

      return getUsageRestrictionsFromUserPermissions(accountId);
    }
  }

  @Override
  public boolean userHasPermissionsToChangeEntity(String accountId, UsageRestrictions entityUsageRestrictions,
      Map<String, Set<String>> appEnvMapFromEntityRestrictions, UsageRestrictions restrictionsFromUserPermissions,
      Map<String, Set<String>> appEnvMapFromUserPermissions) {
    if (!hasUserContext()) {
      return true;
    }

    return userHasPermissions(accountId, entityUsageRestrictions, appEnvMapFromEntityRestrictions,
        restrictionsFromUserPermissions, appEnvMapFromUserPermissions);
  }

  @Override
  public boolean userHasPermissionsToChangeEntity(
      String accountId, UsageRestrictions entityUsageRestrictions, UsageRestrictions restrictionsFromUserPermissions) {
    if (!hasUserContext()) {
      return true;
    }

    Map<String, Set<String>> appEnvMapFromUserPermissions = restrictionsFromUserPermissions != null
        ? getAppEnvMap(accountId, restrictionsFromUserPermissions.getAppEnvRestrictions())
        : emptyMap();

    Map<String, Set<String>> appEnvMapFromEntityRestrictions = entityUsageRestrictions != null
        ? getAppEnvMap(accountId, entityUsageRestrictions.getAppEnvRestrictions())
        : emptyMap();

    return userHasPermissions(accountId, entityUsageRestrictions, appEnvMapFromEntityRestrictions,
        restrictionsFromUserPermissions, appEnvMapFromUserPermissions);
  }

  @Override
  public boolean userHasPermissionsToChangeEntity(String accountId, UsageRestrictions entityUsageRestrictions) {
    return this.userHasPermissionsToChangeEntity(
        accountId, entityUsageRestrictions, getUsageRestrictionsFromUserPermissions(accountId));
  }

  private boolean userHasPermissions(String accountId, UsageRestrictions entityUsageRestrictions,
      Map<String, Set<String>> appEnvMapFromEntityRestrictions, UsageRestrictions restrictionsFromUserPermissions,
      Map<String, Set<String>> appEnvMapFromUserPermissions) {
    if (hasNoRestrictions(restrictionsFromUserPermissions)) {
      return false;
    }

    // If no restrictions, its equivalent to selecting all apps -> prod and non-prod
    if (hasNoRestrictions(entityUsageRestrictions)) {
      entityUsageRestrictions = UsageRestrictions.builder().appEnvRestrictions(allAppEnvRestrictions).build();
      appEnvMapFromEntityRestrictions = getAppEnvMap(accountId, allAppEnvRestrictions);
    }

    if (isEmpty(appEnvMapFromEntityRestrictions)) {
      return hasCommonEnv(entityUsageRestrictions, restrictionsFromUserPermissions);
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
            return hasCommonEnv(appId, entityUsageRestrictionsFinal, restrictionsFromUserPermissions);
          }

          Set<String> envIdsFromUserPermissions = appEnvMapFromUserPermissions.get(appId);
          if (isEmpty(envIdsFromUserPermissions)) {
            return false;
          }

          return envIdsFromRestrictions.stream().allMatch(
              envIdFromRestriction -> envIdsFromUserPermissions.contains(envIdFromRestriction));
        });
  }

  private Set<AppEnvRestriction> getAllAppEnvRestrictions() {
    Set<AppEnvRestriction> appEnvRestrictions = new HashSet<>(1);
    appEnvRestrictions.add(
        AppEnvRestriction.builder()
            .appFilter(GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build())
            .envFilter(EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.PROD, FilterType.NON_PROD)).build())
            .build());
    return appEnvRestrictions;
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
    if (usageRestrictions != null && isNotEmpty(usageRestrictions.getAppEnvRestrictions())) {
      return true;
    }
    if (isAccountAdmin(accountId)) {
      return true;
    }

    return hasAllEnvAccessOfTypes(
        restrictionsFromUserPermissions, Sets.newHashSet(FilterType.PROD, FilterType.NON_PROD));
  }

  private boolean isAccountAdmin(String accountId) {
    User user = UserThreadLocal.get();
    if (user == null) {
      return true;
    }

    UserRequestContext userRequestContext = user.getUserRequestContext();
    if (userRequestContext == null) {
      return true;
    }

    if (!accountId.equals(userRequestContext.getAccountId())) {
      return false;
    }

    UserPermissionInfo userPermissionInfo = userRequestContext.getUserPermissionInfo();

    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    if (accountPermissionSummary == null) {
      return false;
    }

    Set<PermissionType> permissions = accountPermissionSummary.getPermissions();

    if (isEmpty(permissions)) {
      return false;
    }

    return permissions.contains(PermissionType.ACCOUNT_MANAGEMENT);
  }

  @Override
  public void validateUsageRestrictionsOnEntitySave(String accountId, UsageRestrictions usageRestrictions)
      throws WingsException {
    checkIfValidUsageRestrictions(usageRestrictions);

    if (!hasUserContext()) {
      return;
    }

    UsageRestrictions restrictionsFromUserPermissions = getUsageRestrictionsFromUserPermissions(accountId);
    boolean allowed =
        checkIfUserCanSetWithNoUsageRestrictions(accountId, usageRestrictions, restrictionsFromUserPermissions);

    if (!allowed) {
      throw new WingsException(ErrorCode.NOT_ACCOUNT_MGR_NOR_HAS_ALL_APP_ACCESS, USER);
    }

    boolean canUpdateEntity =
        userHasPermissionsToChangeEntity(accountId, usageRestrictions, restrictionsFromUserPermissions);

    if (!canUpdateEntity) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }
  }

  @Override
  public void validateUsageRestrictionsOnEntityUpdate(String accountId, UsageRestrictions oldUsageRestrictions,
      UsageRestrictions newUsageRestrictions) throws WingsException {
    checkIfValidUsageRestrictions(newUsageRestrictions);

    if (!hasUserContext()) {
      return;
    }

    UsageRestrictions restrictionsFromUserPermissions = getUsageRestrictionsFromUserPermissions(accountId);
    boolean allowed =
        checkIfUserCanSetWithNoUsageRestrictions(accountId, newUsageRestrictions, restrictionsFromUserPermissions);

    if (!allowed) {
      throw new WingsException(ErrorCode.NOT_ACCOUNT_MGR_NOR_HAS_ALL_APP_ACCESS, USER);
    }

    boolean canUpdateEntity =
        userHasPermissionsToChangeEntity(accountId, oldUsageRestrictions, restrictionsFromUserPermissions);

    if (!canUpdateEntity) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }
  }

  private void checkIfValidUsageRestrictions(UsageRestrictions usageRestrictions) {
    if (usageRestrictions != null && isNotEmpty(usageRestrictions.getAppEnvRestrictions())) {
      usageRestrictions.getAppEnvRestrictions().forEach(appEnvRestriction -> {
        GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
        if (appFilter == null || appFilter.getFilterType() == null) {
          throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
        }
        if (appFilter.getFilterType().equals(GenericEntityFilter.FilterType.SELECTED)) {
          if (isEmpty(appFilter.getIds())) {
            throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
          } else if (appFilter.getIds().stream().anyMatch(id -> isEmpty(id))) {
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

        if (envFilterTypes.contains(FilterType.SELECTED)) {
          if (envFilterTypes.size() != 1) {
            throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
          } else {
            if (isEmpty(envFilter.getIds())) {
              throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
            } else if (envFilter.getIds().stream().anyMatch(id -> isEmpty(id))) {
              throw new WingsException(ErrorCode.INVALID_USAGE_RESTRICTION, USER);
            }
          }
        }
      });
    }
  }
}
