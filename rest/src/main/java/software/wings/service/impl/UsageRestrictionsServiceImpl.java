package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.exception.WingsException.USER;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
import software.wings.settings.UsageRestrictions.AppEnvRestriction.AppEnvRestrictionBuilder;

import java.util.Collection;
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

  @Override
  public boolean hasAccess(
      UsageRestrictions usageRestrictions, String accountId, String appIdFromRequest, String envIdFromRequest) {
    if (usageRestrictions == null) {
      return true;
    }

    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();

    if (isEmpty(appEnvRestrictions)) {
      return true;
    }

    Multimap<String, String> appEnvMap = getAppEnvMap(accountId, appEnvRestrictions);

    if (appIdFromRequest != null && !appIdFromRequest.equals(GLOBAL_APP_ID)) {
      if (envIdFromRequest != null) {
        // Restrict it to both app and env
        return appEnvMap.containsKey(appIdFromRequest) && appEnvMap.containsEntry(appIdFromRequest, envIdFromRequest);
      } else {
        // Restrict it to app
        return appEnvMap.containsKey(appIdFromRequest);
      }
    } else {
      User user = UserThreadLocal.get();

      if (user == null) {
        return true;
      }

      return hasAccess(accountId, appEnvMap);
    }
  }

  private boolean hasAccess(String accountId, Multimap<String, String> appEnvMapOfEntity) {
    UsageRestrictions usageRestrictionsOfUser = getUsageRestrictionsFromUserPermissions(accountId, false);

    if (usageRestrictionsOfUser == null) {
      return false;
    }

    Multimap<String, String> appEnvMapOfUser = getAppEnvMap(accountId, usageRestrictionsOfUser.getAppEnvRestrictions());
    return appEnvMapOfEntity.asMap().entrySet().stream().anyMatch(
        (Entry<String, Collection<String>> appEnvEntryOfEntity) -> {
          String appId = appEnvEntryOfEntity.getKey();
          Collection<String> envIdsFromRestrictions = appEnvEntryOfEntity.getValue();
          Collection<String> envIdsOfUser = appEnvMapOfUser.get(appId);

          if (isEmpty(envIdsFromRestrictions)) {
            return true;
          }

          if (isEmpty(envIdsOfUser)) {
            return false;
          }

          return envIdsFromRestrictions.stream().anyMatch(
              envIdFromRestriction -> envIdsOfUser.contains(envIdFromRestriction));
        });
  }

  private Multimap<String, String> getAppEnvMap(String accountId, Set<AppEnvRestriction> appEnvRestrictions) {
    Multimap<String, String> appEnvMap = HashMultimap.create();

    appEnvRestrictions.forEach(appEnvRestriction -> {
      GenericEntityFilter appFilter = appEnvRestriction.getAppFilter();
      Set<String> appIds = authHandler.getAppIdsByFilter(accountId, appFilter);
      if (isEmpty(appIds)) {
        return;
      }

      EnvFilter envFilter = appEnvRestriction.getEnvFilter();
      appIds.forEach(appId -> {
        Set<String> envIds = authHandler.getEnvIdsByFilter(appId, envFilter);
        if (isEmpty(envIds)) {
          if (!appEnvMap.containsKey(appId)) {
            appEnvMap.putAll(appId, new HashSet<>());
          }
        } else {
          appEnvMap.putAll(appId, envIds);
        }
      });
    });
    return appEnvMap;
  }

  @Override
  public UsageRestrictions getUsageRestrictionsFromUserPermissions(String accountId, boolean evaluateIfEditable) {
    User user = UserThreadLocal.get();

    if (user == null) {
      return null;
    }

    UserRequestContext userRequestContext = user.getUserRequestContext();

    if (userRequestContext == null) {
      return null;
    }

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

    UsageRestrictions usageRestrictions = UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictions).build();
    if (evaluateIfEditable) {
      boolean isEditable = canUserUpdateOrDeleteEntity(accountId, usageRestrictions);
      usageRestrictions.setEditable(isEditable);
    }
    return usageRestrictions;
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
    PageResponse<Application> pageResponse = appService.list(PageRequestBuilder.aPageRequest()
                                                                 .addFilter("accountId", Operator.EQ, accountId)
                                                                 .addFieldsIncluded("_id", "name")
                                                                 .build(),
        false);

    Map<String, String> appMap =
        pageResponse.getResponse().stream().collect(Collectors.toMap(app -> app.getUuid(), app -> app.getName()));

    PageResponse<Environment> envPageResponse =
        environmentService.list(PageRequestBuilder.aPageRequest()
                                    .addFilter("appId", Operator.IN, appMap.keySet().toArray(new String[0]))
                                    .addFieldsIncluded("_id", "name")
                                    .build(),
            false);

    Map<String, String> envMap =
        envPageResponse.getResponse().stream().collect(Collectors.toMap(env -> env.getUuid(), env -> env.getName()));

    UsageRestrictions usageRestrictionsOfUser = getUsageRestrictionsFromUserPermissions(accountId, true);

    if (usageRestrictionsOfUser == null) {
      throw new WingsException(ErrorCode.USER_HAS_NO_PERMISSIONS);
    }

    Multimap<String, String> appEnvMapOfUser = getAppEnvMap(accountId, usageRestrictionsOfUser.getAppEnvRestrictions());

    boolean hasAllAppAccess = hasAllAppAccess(usageRestrictionsOfUser);
    boolean hasAllProdEnvAccessForAllApps = hasAllEnvAccessOfType(usageRestrictionsOfUser, FilterType.PROD);
    boolean hasAllNonProdEnvAccessForAllApps = hasAllEnvAccessOfType(usageRestrictionsOfUser, FilterType.NON_PROD);

    Set<AppRestrictionsSummary> appRestrictionsSummarySet = Sets.newHashSet();
    appEnvMapOfUser.asMap().forEach((key, value) -> {
      String appId = key;
      boolean hasAllProdEnvAccess = hasAllProdEnvAccess(usageRestrictionsOfUser, appId);
      boolean hasAllNonProdEnvAccess = hasAllNonProdEnvAccess(usageRestrictionsOfUser, appId);
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

  private boolean hasAllProdEnvAccess(UsageRestrictions usageRestrictions, String appId) {
    return hasAllEnvAccessOfType(usageRestrictions, appId, FilterType.PROD);
  }

  private boolean hasAllNonProdEnvAccess(UsageRestrictions usageRestrictions, String appId) {
    return hasAllEnvAccessOfType(usageRestrictions, appId, FilterType.NON_PROD);
  }

  private boolean hasAllEnvAccessOfType(UsageRestrictions usageRestrictions, String appId, String envType) {
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
    Set<AppEnvRestriction> appEnvRestrictions = usageRestrictions.getAppEnvRestrictions();
    if (isEmpty(appEnvRestrictions)) {
      return false;
    }

    return appEnvRestrictions.stream().anyMatch(appEnvRestriction
        -> appEnvRestriction.getAppFilter().getFilterType().equals(GenericEntityFilter.FilterType.ALL));
  }

  @Override
  public UsageRestrictions getDefaultUsageRestrictions(String accountId, String appId, String envId) {
    AppEnvRestrictionBuilder appEnvRestrictionBuilder = AppEnvRestriction.builder();
    if (isNotEmpty(appId)) {
      appEnvRestrictionBuilder.appFilter(GenericEntityFilter.builder()
                                             .filterType(GenericEntityFilter.FilterType.SELECTED)
                                             .ids(Sets.newHashSet(appId))
                                             .build());
      if (isNotEmpty(envId)) {
        appEnvRestrictionBuilder.envFilter(
            EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.SELECTED)).ids(Sets.newHashSet(envId)).build());
      } else {
        // TODO Make the env part of AppEnvRestriction optional
        appEnvRestrictionBuilder.envFilter(
            EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.PROD, FilterType.NON_PROD)).build());
      }

      return UsageRestrictions.builder()
          .isEditable(true)
          .appEnvRestrictions(Sets.newHashSet(appEnvRestrictionBuilder.build()))
          .build();
    } else {
      if (isAccountAdmin(accountId)) {
        return null;
      }

      return getUsageRestrictionsFromUserPermissions(accountId, true);
    }
  }

  @Override
  public boolean canUserUpdateOrDeleteEntity(String accountId, UsageRestrictions entityUsageRestrictions) {
    if (entityUsageRestrictions == null || isEmpty(entityUsageRestrictions.getAppEnvRestrictions())) {
      return true;
    }

    UsageRestrictions usageRestrictionsOfUser = getUsageRestrictionsFromUserPermissions(accountId, false);

    if (usageRestrictionsOfUser == null) {
      return false;
    }

    Multimap<String, String> appEnvMapOfEntity =
        getAppEnvMap(accountId, entityUsageRestrictions.getAppEnvRestrictions());
    Multimap<String, String> appEnvMapOfUser = getAppEnvMap(accountId, usageRestrictionsOfUser.getAppEnvRestrictions());
    return appEnvMapOfEntity.asMap().entrySet().stream().allMatch(
        (Entry<String, Collection<String>> appEnvEntryOfEntity) -> {
          String appId = appEnvEntryOfEntity.getKey();
          Collection<String> envIdsFromRestrictions = appEnvEntryOfEntity.getValue();
          Collection<String> envIdsOfUser = appEnvMapOfUser.get(appId);

          if (isEmpty(envIdsFromRestrictions)) {
            return true;
          }

          if (isEmpty(envIdsOfUser)) {
            return false;
          }

          return envIdsFromRestrictions.stream().allMatch(
              envIdFromRestriction -> envIdsOfUser.contains(envIdFromRestriction));
        });
  }

  /**
   * Checks if user can create / update an entity without any usage restrictions. Only users with Account Manager
   * permission or (All Apps - All Envs) permissions can do it.
   * @param accountId
   * @param usageRestrictions usage restrictions
   * @return boolean
   */
  private boolean checkIfUserCanSetWithNoUsageRestrictions(String accountId, UsageRestrictions usageRestrictions) {
    if (usageRestrictions != null && isNotEmpty(usageRestrictions.getAppEnvRestrictions())) {
      return true;
    }
    if (isAccountAdmin(accountId)) {
      return true;
    }

    UsageRestrictions restrictionsFromUserPermissions = getUsageRestrictionsFromUserPermissions(accountId, false);
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
  public void validateUsageRestrictions(String accountId, UsageRestrictions oldUsageRestrictions,
      UsageRestrictions newUsageRestrictions) throws WingsException {
    checkIfValidUsageRestrictions(newUsageRestrictions);

    boolean allowed = checkIfUserCanSetWithNoUsageRestrictions(accountId, newUsageRestrictions);

    if (!allowed) {
      throw new WingsException(ErrorCode.NOT_ACCOUNT_MGR, USER);
    }

    boolean canUpdateEntity = canUserUpdateOrDeleteEntity(accountId, oldUsageRestrictions);

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
