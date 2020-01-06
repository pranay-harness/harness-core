package software.wings.service.impl.security.auth;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.noop;
import static java.util.Arrays.asList;
import static software.wings.beans.security.UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.GenericEntityFilter.FilterType.SELECTED;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.AUDIT_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.PIPELINE;
import static software.wings.security.PermissionAttribute.PermissionType.PROVISIONER;
import static software.wings.security.PermissionAttribute.PermissionType.SERVICE;
import static software.wings.security.PermissionAttribute.PermissionType.TAG_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_READ;
import static software.wings.security.PermissionAttribute.PermissionType.WORKFLOW;
import static software.wings.security.UserRequestContext.EntityInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.HttpMethod;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AccountPermissionSummary.AccountPermissionSummaryBuilder;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.AppPermissionSummaryForUI.AppPermissionSummaryForUIBuilder;
import software.wings.security.EnvFilter;
import software.wings.security.Filter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserPermissionInfo.UserPermissionInfoBuilder;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.security.WorkflowFilter;
import software.wings.service.impl.UserGroupServiceImpl;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HarnessApiKeyService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author rktummala on 3/7/18
 */
@Singleton
@Slf4j
public class AuthHandler {
  /**
   * The constant DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION.
   */
  private static final String DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION =
      "Production Support members have access to override configuration, "
      + "setup infrastructure and setup/execute deployment workflows within PROD environments";
  /**
   * The constant DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION.
   */
  private static final String DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION =
      "Non-production Support members have access to override configuration, "
      + "setup infrastructure and setup/execute deployment workflows within NON_PROD environments";

  @Inject private PipelineService pipelineService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowService workflowService;
  @Inject private UserGroupService userGroupService;
  @Inject private AuthService authService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DashboardAuthHandler dashboardAuthHandler;
  @Inject private ApiKeyService apiKeyService;
  @Inject private HarnessApiKeyService harnessApiKeyService;

  public UserPermissionInfo evaluateUserPermissionInfo(String accountId, List<UserGroup> userGroups, User user) {
    UserPermissionInfoBuilder userPermissionInfoBuilder = UserPermissionInfo.builder().accountId(accountId);

    Set<PermissionType> accountPermissionSet = new HashSet<>();
    AccountPermissionSummaryBuilder accountPermissionSummaryBuilder =
        AccountPermissionSummary.builder().permissions(accountPermissionSet);

    populateRequiredAccountPermissions(userGroups, accountPermissionSet);

    // Get all app ids
    HashSet<String> allAppIds = new HashSet<>(appService.getAppIdsByAccountId(accountId));

    // Cache all the entities by app id first
    Map<PermissionType, Set<String>> permissionTypeAppIdSetMap = collectRequiredAppIds(userGroups, allAppIds);

    // Fetch all entities by appIds
    Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap =
        fetchRequiredEntities(accountId, permissionTypeAppIdSetMap);

    // Filter and assign permissions
    Map<String, AppPermissionSummary> appPermissionMap =
        populateAppPermissions(userGroups, permissionTypeAppIdEntityMap, allAppIds);

    userPermissionInfoBuilder.appPermissionMapInternal(appPermissionMap)
        .accountPermissionSummary(accountPermissionSummaryBuilder.build());

    userPermissionInfoBuilder.hasAllAppAccess(allAppIds.size() <= appPermissionMap.keySet().size());

    UserPermissionInfo userPermissionInfo = userPermissionInfoBuilder.build();
    setAppPermissionMap(userPermissionInfo);

    Map<String, Set<io.harness.dashboard.Action>> dashboardPermissions =
        dashboardAuthHandler.getDashboardAccessPermissions(user, accountId, userPermissionInfo, userGroups);
    userPermissionInfo.setDashboardPermissions(dashboardPermissions);

    return userPermissionInfo;
  }

  private Map<String, AppPermissionSummary> populateAppPermissions(List<UserGroup> userGroups,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, HashSet<String> allAppIds) {
    Map<String, AppPermissionSummary> appPermissionMap = new HashMap<>();
    Multimap<String, Action> envActionMapForPipeline = HashMultimap.create();
    Multimap<String, Action> envActionMapForDeployment = HashMultimap.create();

    userGroups.forEach(userGroup -> {
      Set<AppPermission> appPermissions = userGroup.getAppPermissions();
      if (isEmpty(appPermissions)) {
        return;
      }
      appPermissions.forEach(appPermission -> {
        if (isEmpty(appPermission.getActions())) {
          logger.error("Actions empty for apps: {}", appPermission.getAppFilter());
          return;
        }

        Set<String> appIds = getAppIdsByFilter(allAppIds, appPermission.getAppFilter());
        PermissionType permissionType = appPermission.getPermissionType();

        if (permissionType == ALL_APP_ENTITIES) {
          asList(SERVICE, PROVISIONER, ENV, WORKFLOW, DEPLOYMENT).forEach(permissionType1 -> {
            // ignoring entity filter in case of ALL_APP_ENTITIES
            attachPermission(appPermissionMap, permissionTypeAppIdEntityMap, appIds, permissionType1, null,
                appPermission.getActions());
          });

          attachPipelinePermission(envActionMapForPipeline, appPermissionMap, permissionTypeAppIdEntityMap, appIds,
              PIPELINE, null, appPermission.getActions());

          attachPipelinePermission(envActionMapForDeployment, appPermissionMap, permissionTypeAppIdEntityMap, appIds,
              DEPLOYMENT, null, appPermission.getActions());

        } else {
          if (permissionType == PIPELINE) {
            attachPipelinePermission(envActionMapForPipeline, appPermissionMap, permissionTypeAppIdEntityMap, appIds,
                permissionType, appPermission.getEntityFilter(), appPermission.getActions());
          } else {
            attachPermission(appPermissionMap, permissionTypeAppIdEntityMap, appIds, permissionType,
                appPermission.getEntityFilter(), appPermission.getActions());

            if (permissionType == DEPLOYMENT) {
              attachPipelinePermission(envActionMapForDeployment, appPermissionMap, permissionTypeAppIdEntityMap,
                  appIds, permissionType, appPermission.getEntityFilter(), appPermission.getActions());
            }
          }
        }
      });
    });

    return appPermissionMap;
  }

  private Map<String, Set<Action>> setActionsForEntity(
      Map<String, Set<Action>> permissionMap, String entityId, Collection<Action> actionCollection) {
    if (permissionMap == null) {
      permissionMap = new HashMap<>();
    }

    Set<Action> actions = permissionMap.get(entityId);

    if (actions == null) {
      actions = new HashSet<>();
      permissionMap.put(entityId, actions);
    }

    actions.addAll(actionCollection);
    return permissionMap;
  }

  private Map<Action, Set<String>> buildActionEntityMap(
      Map<Action, Set<String>> permissionMap, Set<String> entityIdSet, Set<Action> actionSet) {
    if (permissionMap == null) {
      permissionMap = new HashMap<>();
    }

    Map<Action, Set<String>> finalPermissionMap = permissionMap;
    actionSet.forEach(action -> {
      Set<String> existingEntityIdSet = finalPermissionMap.get(action);
      if (existingEntityIdSet == null) {
        existingEntityIdSet = new HashSet<>();
        finalPermissionMap.put(action, existingEntityIdSet);
      }
      existingEntityIdSet.addAll(entityIdSet);
    });
    return permissionMap;
  }

  private Map<Action, Set<String>> buildActionPipelineMap(
      Map<Action, Set<String>> permissionMap, Multimap<String, Action> pipelineIdActionMap) {
    if (permissionMap == null) {
      permissionMap = new HashMap<>();
    }

    Map<Action, Set<String>> finalPermissionMap = permissionMap;
    pipelineIdActionMap.forEach((pipelineId, action) -> {
      Set<String> existingPipelineIdSet = finalPermissionMap.get(action);
      if (existingPipelineIdSet == null) {
        existingPipelineIdSet = new HashSet<>();
        finalPermissionMap.put(action, existingPipelineIdSet);
      }
      existingPipelineIdSet.add(pipelineId);
    });
    return permissionMap;
  }

  private Map<Action, Set<EnvInfo>> buildActionEnvMap(
      Map<Action, Set<EnvInfo>> permissionMap, Set<EnvInfo> envInfoSet, Set<Action> actionSet) {
    if (permissionMap == null) {
      permissionMap = new HashMap<>();
    }

    Map<Action, Set<EnvInfo>> finalPermissionMap = permissionMap;
    actionSet.forEach(action -> {
      Set<EnvInfo> existingEnvIdSet = finalPermissionMap.get(action);
      if (existingEnvIdSet == null) {
        existingEnvIdSet = new HashSet<>();
        finalPermissionMap.put(action, existingEnvIdSet);
      }
      existingEnvIdSet.addAll(envInfoSet);
    });
    return permissionMap;
  }

  private void attachPermission(Map<String, AppPermissionSummary> appPermissionMap,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, Set<String> appIds,
      PermissionType permissionType, Filter entityFilter, Set<Action> actions) {
    final HashSet<Action> fixedEntityActions =
        Sets.newHashSet(Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE);
    appIds.forEach(appId -> {
      AppPermissionSummary appPermissionSummary = appPermissionMap.get(appId);
      if (appPermissionSummary == null) {
        appPermissionSummary = new AppPermissionSummary();
        appPermissionMap.put(appId, appPermissionSummary);
      }

      SetView<Action> intersection = Sets.intersection(fixedEntityActions, actions);
      Set<Action> entityActions = new HashSet<>(intersection);
      final AppPermissionSummary finalAppPermissionSummary = appPermissionSummary;
      switch (permissionType) {
        case SERVICE: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreateService(true);
          }

          if (isEmpty(entityActions)) {
            break;
          }

          Set<String> entityIds = getServiceIdsByFilter(
              permissionTypeAppIdEntityMap.get(permissionType).get(appId), (GenericEntityFilter) entityFilter);
          if (isEmpty(entityIds)) {
            break;
          }
          Map<Action, Set<String>> actionEntityIdMap =
              buildActionEntityMap(finalAppPermissionSummary.getServicePermissions(), entityIds, entityActions);
          finalAppPermissionSummary.setServicePermissions(actionEntityIdMap);
          break;
        }
        case PROVISIONER: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreateProvisioner(true);
          }

          if (isEmpty(entityActions)) {
            break;
          }
          Set<String> entityIds = getProvisionerIdsByFilter(
              permissionTypeAppIdEntityMap.get(permissionType).get(appId), (GenericEntityFilter) entityFilter);
          if (isEmpty(entityIds)) {
            break;
          }
          Map<Action, Set<String>> actionEntityIdMap =
              buildActionEntityMap(finalAppPermissionSummary.getProvisionerPermissions(), entityIds, entityActions);
          finalAppPermissionSummary.setProvisionerPermissions(actionEntityIdMap);
          break;
        }
        case ENV: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreateEnvironment(true);
            Set<EnvironmentType> environmentTypeSet = addEnvTypesByFilter(
                finalAppPermissionSummary.getEnvCreatePermissionsForEnvTypes(), (EnvFilter) entityFilter);
            finalAppPermissionSummary.setEnvCreatePermissionsForEnvTypes(environmentTypeSet);
          }

          if (isEmpty(entityActions)) {
            break;
          }
          Set<EnvInfo> envInfoSet = getEnvsInfoByFilter(
              permissionTypeAppIdEntityMap.get(permissionType).get(appId), (EnvFilter) entityFilter);
          if (isEmpty(envInfoSet)) {
            break;
          }

          Map<Action, Set<EnvInfo>> actionEnvInfoMap =
              buildActionEnvMap(finalAppPermissionSummary.getEnvPermissions(), envInfoSet, entityActions);
          finalAppPermissionSummary.setEnvPermissions(actionEnvInfoMap);
          break;
        }
        case WORKFLOW: {
          Set<String> envIdSet = null;
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreateWorkflow(true);
            envIdSet = getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getWorkflowCreatePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setWorkflowCreatePermissionsForEnvs(updatedEnvIdSet);

            if (!finalAppPermissionSummary.isCanCreateTemplatizedWorkflow()) {
              WorkflowFilter workflowFilter = getDefaultWorkflowFilterIfNull((WorkflowFilter) entityFilter);
              Set<String> filterTypes = workflowFilter.getFilterTypes();
              if (isNotEmpty(filterTypes)) {
                boolean hasTemplateFilterType = filterTypes.contains(WorkflowFilter.FilterType.TEMPLATES);
                finalAppPermissionSummary.setCanCreateTemplatizedWorkflow(hasTemplateFilterType);
              }
            }
          }

          if (isEmpty(entityActions)) {
            break;
          }

          if (entityActions.contains(Action.UPDATE)) {
            if (envIdSet == null) {
              envIdSet = getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
            }

            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getWorkflowUpdatePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setWorkflowUpdatePermissionsForEnvs(updatedEnvIdSet);
          }

          Set<String> entityIds = getWorkflowIdsByFilter(permissionTypeAppIdEntityMap.get(permissionType).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (WorkflowFilter) entityFilter);

          if (isEmpty(entityIds)) {
            break;
          }

          Map<Action, Set<String>> actionEntityIdMap =
              buildActionEntityMap(finalAppPermissionSummary.getWorkflowPermissions(), entityIds, entityActions);
          finalAppPermissionSummary.setWorkflowPermissions(actionEntityIdMap);
          break;
        }
        case DEPLOYMENT: {
          if (isEmpty(entityActions)) {
            break;
          }
          Set<String> entityIds = getDeploymentIdsByFilter(permissionTypeAppIdEntityMap.get(WORKFLOW).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, appId);

          if (isEmpty(entityIds)) {
            break;
          }

          Map<Action, Set<String>> actionEntityIdMap =
              buildActionEntityMap(finalAppPermissionSummary.getDeploymentPermissions(), entityIds, entityActions);
          finalAppPermissionSummary.setDeploymentPermissions(actionEntityIdMap);

          Set<String> envIdSet =
              getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
          if (entityActions.contains(Action.EXECUTE)) {
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getDeploymentExecutePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setDeploymentExecutePermissionsForEnvs(updatedEnvIdSet);
          }
          break;
        }
        default:
          noop();
      }
    });
  }

  private Set<String> addToExistingEntityIdSet(Set<String> existingEntityIdSet, Set<String> entityIdSet) {
    if (isEmpty(entityIdSet)) {
      return existingEntityIdSet;
    }

    if (existingEntityIdSet == null) {
      existingEntityIdSet = new HashSet<>();
    }

    existingEntityIdSet.addAll(entityIdSet);
    return existingEntityIdSet;
  }

  private void attachPipelinePermission(Multimap<String, Action> envActionMap,
      Map<String, AppPermissionSummary> appPermissionMap,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, Set<String> appIds,
      PermissionType permissionType, Filter entityFilter, Set<Action> actions) {
    final HashSet<Action> fixedEntityActions =
        Sets.newHashSet(Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE);
    appIds.forEach(appId -> {
      AppPermissionSummary appPermissionSummary = appPermissionMap.get(appId);
      if (appPermissionSummary == null) {
        appPermissionSummary = new AppPermissionSummary();
        appPermissionMap.put(appId, appPermissionSummary);
      }

      SetView<Action> intersection = Sets.intersection(fixedEntityActions, actions);
      Set<Action> entityActions = new HashSet<>(intersection);
      AppPermissionSummary finalAppPermissionSummary = appPermissionSummary;
      Multimap<String, Action> pipelineIdActionMap;
      switch (permissionType) {
        case PIPELINE:
          Set<String> envIdSet = null;
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreatePipeline(true);
            envIdSet = getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getPipelineCreatePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setPipelineCreatePermissionsForEnvs(updatedEnvIdSet);
          }

          if (isEmpty(entityActions)) {
            break;
          }

          if (entityActions.contains(Action.UPDATE)) {
            if (envIdSet == null) {
              envIdSet = getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
            }
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getPipelineUpdatePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setPipelineUpdatePermissionsForEnvs(updatedEnvIdSet);
          }

          pipelineIdActionMap = getPipelineIdsByFilter(permissionTypeAppIdEntityMap.get(PIPELINE).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, envActionMap, entityActions);

          Map<Action, Set<String>> actionEntityIdMap =
              buildActionPipelineMap(finalAppPermissionSummary.getPipelinePermissions(), pipelineIdActionMap);
          finalAppPermissionSummary.setPipelinePermissions(actionEntityIdMap);
          break;

        case DEPLOYMENT:
          if (isEmpty(entityActions)) {
            break;
          }

          pipelineIdActionMap = getPipelineIdsByFilter(permissionTypeAppIdEntityMap.get(PIPELINE).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, envActionMap, entityActions);

          actionEntityIdMap =
              buildActionPipelineMap(finalAppPermissionSummary.getDeploymentPermissions(), pipelineIdActionMap);
          finalAppPermissionSummary.setDeploymentPermissions(actionEntityIdMap);
          break;

        default:
          noop();
      }
    });
  }

  private Map<PermissionType, Map<String, List<Base>>> fetchRequiredEntities(
      String accountId, Map<PermissionType, Set<String>> permissionTypeAppIdSetMap) {
    Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap = new HashMap<>();
    permissionTypeAppIdSetMap.keySet().forEach(permissionType -> {
      switch (permissionType) {
        case SERVICE: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdServiceMap(accountId));
          break;
        }
        case PROVISIONER: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdProvisionerMap(accountId));
          break;
        }
        case ENV: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdEnvMap(accountId));
          break;
        }
        case WORKFLOW: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdWorkflowMap(accountId));
          break;
        }
        case PIPELINE: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdPipelineMap(accountId));
          break;
        }
        default: { noop(); }
      }
    });
    return permissionTypeAppIdEntityMap;
  }

  private Map<String, List<Base>> getAppIdServiceMap(String accountId) {
    PageRequest<Service> pageRequest =
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFieldsIncluded("_id", "appId").build();
    List<Service> list =
        getAllEntities(pageRequest, () -> serviceResourceService.list(pageRequest, false, false, false, null));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdProvisionerMap(String accountId) {
    PageRequest<InfrastructureProvisioner> pageRequest =
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFieldsIncluded("_id", "appId").build();

    List<InfrastructureProvisioner> list =
        getAllEntities(pageRequest, () -> infrastructureProvisionerService.list(pageRequest));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdEnvMap(String accountId) {
    PageRequest<Environment> pageRequest = aPageRequest()
                                               .addFilter("accountId", Operator.EQ, accountId)
                                               .addFieldsIncluded("_id", "appId", "environmentType")
                                               .build();

    List<Environment> list =
        getAllEntities(pageRequest, () -> environmentService.list(pageRequest, false, false, null));

    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdWorkflowMap(String accountId) {
    PageRequest<Workflow> pageRequest =
        aPageRequest()
            .addFilter("accountId", Operator.EQ, accountId)
            .addFieldsIncluded("_id", "appId", "envId", "templatized", "templateExpressions")
            .build();

    List<Workflow> list =
        getAllEntities(pageRequest, () -> workflowService.listWorkflowsWithoutOrchestration(pageRequest));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdPipelineMap(String accountId) {
    PageRequest<Pipeline> pageRequest = aPageRequest().addFilter("accountId", Operator.EQ, accountId).build();
    List<Pipeline> list = getAllEntities(pageRequest, () -> pipelineService.listPipelines(pageRequest));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private void populateRequiredAccountPermissions(
      List<UserGroup> userGroups, Set<PermissionType> accountPermissionSet) {
    userGroups.forEach(userGroup -> {
      AccountPermissions accountPermissions = userGroup.getAccountPermissions();
      if (accountPermissions != null) {
        Set<PermissionType> permissions = accountPermissions.getPermissions();
        if (CollectionUtils.isNotEmpty(permissions)) {
          accountPermissionSet.addAll(permissions);
        }
      }
    });
  }

  private Map<PermissionType, Set<String>> collectRequiredAppIds(
      List<UserGroup> userGroups, HashSet<String> allAppIds) {
    Map<PermissionType, Set<String>> permissionTypeAppIdSetMap = new HashMap<>();
    // initialize
    asList(SERVICE, PROVISIONER, ENV, WORKFLOW, PIPELINE, DEPLOYMENT)
        .forEach(permissionType -> permissionTypeAppIdSetMap.put(permissionType, new HashSet<>()));

    userGroups.forEach(userGroup -> {
      Set<AppPermission> appPermissions = userGroup.getAppPermissions();
      if (isEmpty(appPermissions)) {
        return;
      }

      appPermissions.forEach(appPermission -> {
        Set<String> appIdSet = getAppIdsByFilter(allAppIds, appPermission.getAppFilter());
        if (appIdSet == null) {
          return;
        }
        PermissionType permissionType = appPermission.getPermissionType();
        if (permissionType == PermissionType.ALL_APP_ENTITIES) {
          asList(SERVICE, PROVISIONER, ENV, WORKFLOW, PIPELINE, DEPLOYMENT).forEach(permissionType1 -> {
            permissionTypeAppIdSetMap.get(permissionType1).addAll(appIdSet);
          });
        } else {
          permissionTypeAppIdSetMap.get(permissionType).addAll(appIdSet);
        }
      });
    });

    Set<String> appIdSetForWorkflowPermission = permissionTypeAppIdSetMap.get(WORKFLOW);
    if (appIdSetForWorkflowPermission == null) {
      appIdSetForWorkflowPermission = new HashSet<>();
      permissionTypeAppIdSetMap.put(WORKFLOW, appIdSetForWorkflowPermission);
    }

    // pipeline will need workflow
    appIdSetForWorkflowPermission.addAll(permissionTypeAppIdSetMap.get(PIPELINE));

    Set<String> appIdSetForEnvPermission = permissionTypeAppIdSetMap.get(ENV);
    if (appIdSetForEnvPermission == null) {
      appIdSetForEnvPermission = new HashSet<>();
      permissionTypeAppIdSetMap.put(ENV, appIdSetForEnvPermission);
    }

    // workflow will need env
    appIdSetForEnvPermission.addAll(appIdSetForWorkflowPermission);

    // DEPLOYMENT will need env
    appIdSetForEnvPermission.addAll(permissionTypeAppIdSetMap.get(DEPLOYMENT));

    return permissionTypeAppIdSetMap;
  }

  public Set<String> getAppIdsByFilter(Set<String> allAppIds, GenericEntityFilter appFilter) {
    if (appFilter == null || FilterType.ALL.equals(appFilter.getFilterType())) {
      return new HashSet<>(allAppIds);
    }

    if (FilterType.SELECTED.equals(appFilter.getFilterType())) {
      SetView<String> intersection = Sets.intersection(appFilter.getIds(), allAppIds);
      return new HashSet<>(intersection);
    } else {
      throw new InvalidRequestException("Unknown app filter type: " + appFilter.getFilterType());
    }
  }

  public Set<String> getAppIdsByFilter(String accountId, GenericEntityFilter appFilter) {
    List<String> appIdsByAccountId = appService.getAppIdsByAccountId(accountId);
    return getAppIdsByFilter(Sets.newHashSet(appIdsByAccountId), appFilter);
  }

  public Set<String> getEnvIdsByFilter(String appId, EnvFilter envFilter) {
    PageRequest<Environment> pageRequest =
        aPageRequest().addFilter("appId", Operator.EQ, appId).addFieldsIncluded("_id", "environmentType").build();
    List<Environment> envList =
        getAllEntities(pageRequest, () -> environmentService.list(pageRequest, false, false, null));

    return getEnvIdsByFilter(envList, envFilter);
  }

  public <T extends Base> List<T> getAllEntities(PageRequest<T> pageRequest, Callable<PageResponse<T>> callable) {
    return wingsPersistence.getAllEntities(pageRequest, callable);
  }

  public void setEntityIdFilterIfUserAction(
      List<PermissionAttribute> requiredPermissionAttributes, List<String> appIds) {
    User user = UserThreadLocal.get();
    if (user != null && user.getUserRequestContext() != null) {
      setEntityIdFilter(requiredPermissionAttributes, user.getUserRequestContext(), appIds);
    }
  }

  public void setAppIdFilter(UserRequestContext userRequestContext, Set<String> appIds) {
    userRequestContext.setAppIdFilterRequired(true);
    userRequestContext.setAppIds(appIds);
  }

  public boolean authorize(
      List<PermissionAttribute> requiredPermissionAttributes, List<String> appIds, String entityId) {
    User user = UserThreadLocal.get();
    if (user != null) {
      UserRequestContext userRequestContext = user.getUserRequestContext();
      // UserRequestContext is null if rbac enabled is false
      if (userRequestContext != null) {
        authService.authorize(userRequestContext.getAccountId(), appIds, entityId, user, requiredPermissionAttributes);
      }
    }
    return true;
  }

  public void setEntityIdFilter(List<PermissionAttribute> requiredPermissionAttributes,
      UserRequestContext userRequestContext, List<String> appIds) {
    String entityFieldName = getEntityFieldName(requiredPermissionAttributes);

    userRequestContext.setEntityIdFilterRequired(true);

    Set<String> entityIds =
        getEntityIds(requiredPermissionAttributes, userRequestContext.getUserPermissionInfo(), appIds);
    EntityInfo entityInfo = EntityInfo.builder().entityFieldName(entityFieldName).entityIds(entityIds).build();
    String entityClassName = getEntityClassName(requiredPermissionAttributes);
    userRequestContext.getEntityInfoMap().put(entityClassName, entityInfo);
  }

  public void setEntityIdFilterIfGet(String httpMethod, boolean skipAuth,
      List<PermissionAttribute> requiredPermissionAttributes, UserRequestContext userRequestContext,
      boolean appIdFilterRequired, Set<String> allowedAppIds, List<String> appIdsFromRequest) {
    if (!skipAuth && HttpMethod.GET.name().equals(httpMethod)) {
      setEntityIdFilter(requiredPermissionAttributes, userRequestContext,
          appIdFilterRequired ? ImmutableList.copyOf(allowedAppIds) : appIdsFromRequest);
    }
  }

  private Set<String> getEntityIds(
      List<PermissionAttribute> permissionAttributes, UserPermissionInfo userPermissionInfo, List<String> appIds) {
    final Set<String> entityIds = new HashSet<>();

    if (appIds == null) {
      return entityIds;
    }

    Map<String, AppPermissionSummary> appPermissionMap = userPermissionInfo.getAppPermissionMapInternal();
    if (MapUtils.isEmpty(appPermissionMap)) {
      return entityIds;
    }

    for (String appId : appIds) {
      AppPermissionSummary appPermissionSummary = appPermissionMap.get(appId);
      if (appPermissionSummary == null) {
        continue;
      }

      for (PermissionAttribute permissionAttribute : permissionAttributes) {
        PermissionType permissionType = permissionAttribute.getPermissionType();
        Action action = permissionAttribute.getAction();

        Map<Action, Set<String>> entityPermissions = null;
        if (permissionType == SERVICE) {
          entityPermissions = appPermissionSummary.getServicePermissions();
        } else if (permissionType == PROVISIONER) {
          entityPermissions = appPermissionSummary.getProvisionerPermissions();
        } else if (permissionType == ENV) {
          Map<Action, Set<EnvInfo>> envEntityPermissions = appPermissionSummary.getEnvPermissions();
          if (isNotEmpty(envEntityPermissions)) {
            Set<EnvInfo> envInfoSet = envEntityPermissions.get(action);
            if (isNotEmpty(envInfoSet)) {
              envInfoSet.forEach(envInfo -> entityIds.add(envInfo.getEnvId()));
            }
          }
          continue;
        } else if (permissionType == WORKFLOW) {
          entityPermissions = appPermissionSummary.getWorkflowPermissions();
        } else if (permissionType == PIPELINE) {
          entityPermissions = appPermissionSummary.getPipelinePermissions();
        } else if (permissionType == DEPLOYMENT) {
          entityPermissions = appPermissionSummary.getDeploymentPermissions();
        }

        if (isEmpty(entityPermissions)) {
          continue;
        }

        Set<String> entityIdCollection = entityPermissions.get(action);
        if (CollectionUtils.isNotEmpty(entityIdCollection)) {
          entityIds.addAll(entityIdCollection);
        }
      }
    }
    return entityIds;
  }

  private String getEntityFieldName(List<PermissionAttribute> permissionAttributes) {
    Optional<String> entityFieldNameOptional = permissionAttributes.stream()
                                                   .map(permissionAttribute -> {
                                                     if (StringUtils.isNotBlank(permissionAttribute.getDbFieldName())) {
                                                       return permissionAttribute.getDbFieldName();
                                                     }

                                                     return "_id";
                                                   })
                                                   .findFirst();

    if (entityFieldNameOptional.isPresent()) {
      return entityFieldNameOptional.get();
    }

    return null;
  }

  private String getEntityClassName(List<PermissionAttribute> permissionAttributes) {
    Optional<String> entityFieldNameOptional =
        permissionAttributes.stream()
            .map(permissionAttribute -> {
              if (StringUtils.isNotBlank(permissionAttribute.getDbCollectionName())) {
                return permissionAttribute.getDbCollectionName();
              }

              PermissionType permissionType = permissionAttribute.getPermissionType();

              String className;
              if (permissionType == SERVICE) {
                className = Service.class.getName();
              } else if (permissionType == PROVISIONER) {
                className = InfrastructureProvisioner.class.getName();
              } else if (permissionType == ENV) {
                className = Environment.class.getName();
              } else if (permissionType == WORKFLOW) {
                className = Workflow.class.getName();
              } else if (permissionType == PIPELINE) {
                className = Pipeline.class.getName();
              } else if (permissionType == DEPLOYMENT) {
                className = WorkflowExecution.class.getName();
              } else {
                throw new InvalidRequestException("Invalid permission type: " + permissionType);
              }

              return className;
            })
            .findFirst();

    if (entityFieldNameOptional.isPresent()) {
      return entityFieldNameOptional.get();
    }

    return null;
  }

  private Set<String> getServiceIdsByFilter(List<Base> services, GenericEntityFilter serviceFilter) {
    if (isEmpty(services)) {
      return new HashSet<>();
    }
    if (serviceFilter == null) {
      serviceFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    }

    if (FilterType.ALL.equals(serviceFilter.getFilterType())) {
      return services.stream().map(Base::getUuid).collect(Collectors.toSet());
    } else if (SELECTED.equals(serviceFilter.getFilterType())) {
      GenericEntityFilter finalServiceFilter = serviceFilter;
      return services.stream()
          .filter(service -> finalServiceFilter.getIds().contains(service.getUuid()))
          .map(Base::getUuid)
          .collect(Collectors.toSet());
    } else {
      String msg = "Unknown service filter type: " + serviceFilter.getFilterType();
      logger.error(msg);
      throw new InvalidRequestException(msg);
    }
  }

  private Set<String> getProvisionerIdsByFilter(List<Base> provisioners, GenericEntityFilter provisionerFilter) {
    if (isEmpty(provisioners)) {
      return new HashSet<>();
    }
    if (provisionerFilter == null) {
      provisionerFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    }

    if (FilterType.ALL.equals(provisionerFilter.getFilterType())) {
      return provisioners.stream().map(Base::getUuid).collect(Collectors.toSet());
    } else if (SELECTED.equals(provisionerFilter.getFilterType())) {
      GenericEntityFilter finalServiceFilter = provisionerFilter;
      return provisioners.stream()
          .filter(service -> finalServiceFilter.getIds().contains(service.getUuid()))
          .map(Base::getUuid)
          .collect(Collectors.toSet());
    } else {
      String msg = "Unknown service filter type: " + provisionerFilter.getFilterType();
      logger.error(msg);
      throw new InvalidRequestException(msg);
    }
  }

  private EnvFilter getDefaultEnvFilterIfNull(EnvFilter envFilter) {
    if (envFilter == null || isEmpty(envFilter.getFilterTypes())) {
      envFilter = new EnvFilter();
      envFilter.setFilterTypes(Sets.newHashSet(PROD, NON_PROD));
    }
    return envFilter;
  }

  private <T extends Base> Set<String> getEnvIdsByFilter(List<T> environments, EnvFilter envFilter) {
    if (environments == null) {
      return new HashSet<>();
    }

    envFilter = getDefaultEnvFilterIfNull(envFilter);

    Set<String> filterTypes = envFilter.getFilterTypes();

    boolean selected = hasEnvSelectedType(envFilter);
    if (selected) {
      EnvFilter finalEnvFilter = envFilter;
      return environments.stream()
          .filter(environment -> finalEnvFilter.getIds().contains(environment.getUuid()))
          .map(Base::getUuid)
          .collect(Collectors.toSet());
    } else {
      return environments.stream()
          .filter(environment -> filterTypes.contains(((Environment) environment).getEnvironmentType().name()))
          .map(Base::getUuid)
          .collect(Collectors.toSet());
    }
  }

  private Set<EnvInfo> getEnvsInfoByFilter(List<Base> environments, EnvFilter envFilter) {
    if (environments == null) {
      return new HashSet<>();
    }

    envFilter = getDefaultEnvFilterIfNull(envFilter);

    Set<String> filterTypes = envFilter.getFilterTypes();

    boolean selected = hasEnvSelectedType(envFilter);
    if (selected) {
      EnvFilter finalEnvFilter = envFilter;
      return environments.stream()
          .filter(environment -> finalEnvFilter.getIds().contains(environment.getUuid()))
          .map(environment
              -> EnvInfo.builder()
                     .envId(environment.getUuid())
                     .envType(((Environment) environment).getEnvironmentType().name())
                     .build())
          .collect(Collectors.toSet());
    } else {
      return environments.stream()
          .filter(environment -> filterTypes.contains(((Environment) environment).getEnvironmentType().name()))
          .map(environment
              -> EnvInfo.builder()
                     .envId(environment.getUuid())
                     .envType(((Environment) environment).getEnvironmentType().name())
                     .build())
          .collect(Collectors.toSet());
    }
  }

  private Set<EnvironmentType> addEnvTypesByFilter(Set<EnvironmentType> existingEnvTypes, EnvFilter envFilter) {
    envFilter = getDefaultEnvFilterIfNull(envFilter);

    if (existingEnvTypes == null) {
      existingEnvTypes = new HashSet<>();
    }

    Set<EnvironmentType> environmentTypeSet = envFilter.getFilterTypes()
                                                  .stream()
                                                  .filter(filter -> !filter.equals(EnvFilter.FilterType.SELECTED))
                                                  .map(EnvironmentType::valueOf)
                                                  .collect(Collectors.toSet());
    existingEnvTypes.addAll(environmentTypeSet);

    return existingEnvTypes;
  }

  private WorkflowFilter getDefaultWorkflowFilterIfNull(WorkflowFilter workflowFilter) {
    if (workflowFilter == null || isEmpty(workflowFilter.getFilterTypes())) {
      workflowFilter = new WorkflowFilter();
      workflowFilter.setFilterTypes(Sets.newHashSet(PROD, NON_PROD, WorkflowFilter.FilterType.TEMPLATES));
    }
    return workflowFilter;
  }

  private Set<String> getWorkflowIdsByFilter(
      List<Base> workflows, List<Base> environments, WorkflowFilter workflowFilter) {
    if (workflows == null) {
      return new HashSet<>();
    }

    workflowFilter = getDefaultWorkflowFilterIfNull(workflowFilter);

    Set<String> filterEnvIds = workflowFilter.getIds();
    if (filterEnvIds == null) {
      filterEnvIds = new HashSet<>();
    }

    boolean hasTemplateFilterType = workflowFilter.getFilterTypes().contains(WorkflowFilter.FilterType.TEMPLATES);

    Set<String> finalFilterEnvIds = filterEnvIds;
    WorkflowFilter finalWorkflowFilter = workflowFilter;

    final Set<String> envIds;
    if (environments != null) {
      envIds = environments.stream()
                   .filter(environment
                       -> finalFilterEnvIds.contains(environment.getUuid())
                           || finalWorkflowFilter.getFilterTypes().contains(
                                  ((Environment) environment).getEnvironmentType().name()))
                   .map(Base::getUuid)
                   .collect(Collectors.toSet());
    } else {
      envIds = Collections.emptySet();
    }

    return workflows.stream()
        .filter(workflow -> {
          Workflow workflowObj = (Workflow) workflow;
          if (isEnvTemplatized(workflowObj)) {
            return hasTemplateFilterType;
          }

          if (workflowObj.getEnvId() == null) {
            return true;
          }

          return envIds.contains(workflowObj.getEnvId());
        })
        .map(Base::getUuid)
        .collect(Collectors.toSet());
  }

  private Set<String> getDeploymentIdsByFilter(
      List<Base> workflows, List<Base> environments, EnvFilter envFilter, String appId) {
    WorkflowFilter workflowFilter = getWorkflowFilterFromEnvFilter(envFilter);

    if (environments != null) {
      Set<String> envIds = getEnvIdsByFilter(environments, envFilter);
      if (CollectionUtils.isEmpty(envIds)) {
        logger.info("No environments matched the filter for app {}. Returning empty set of deployments", appId);
        return new HashSet<>();
      }
    }

    return getWorkflowIdsByFilter(workflows, environments, workflowFilter);
  }

  private WorkflowFilter getWorkflowFilterFromEnvFilter(EnvFilter envFilter) {
    envFilter = getDefaultEnvFilterIfNull(envFilter);

    // Construct workflow filter since we also want to include templates to deployments
    WorkflowFilter workflowFilter = new WorkflowFilter();

    Set<String> workflowFilterTypes = Sets.newHashSet();

    final EnvFilter envFilterFinal = envFilter;

    envFilter.getFilterTypes().forEach(filterType -> {
      workflowFilterTypes.add(filterType);
      if (filterType.equals(EnvFilter.FilterType.SELECTED)) {
        workflowFilter.setIds(envFilterFinal.getIds());
      }
    });

    workflowFilterTypes.add(WorkflowFilter.FilterType.TEMPLATES);
    workflowFilter.setFilterTypes(workflowFilterTypes);

    return workflowFilter;
  }

  private Multimap<String, Action> getPipelineIdsByFilter(List<Base> pipelines, List<Base> environments,
      EnvFilter envFilter, Multimap<String, Action> envActionMap, Set<Action> entityActionsFromCurrentPermission) {
    Multimap<String, Action> pipelineActionMap = HashMultimap.create();

    if (isEmpty(pipelines)) {
      return pipelineActionMap;
    }

    Set<String> envIds;
    if (isNotEmpty(environments)) {
      envIds = getEnvIdsByFilter(environments, envFilter);
      envIds.forEach(envId -> envActionMap.putAll(envId, entityActionsFromCurrentPermission));
    } else {
      envIds = Collections.emptySet();
    }

    Set<String> envIdsFromOtherPermissions = envActionMap.keySet();
    pipelines.forEach(p -> {
      Set<Action> entityActions = new HashSet(entityActionsFromCurrentPermission);
      boolean match;
      Pipeline pipeline = (Pipeline) p;
      if (pipeline.getPipelineStages() == null) {
        match = true;
      } else {
        match = pipeline.getPipelineStages().stream().allMatch(pipelineStage
            -> pipelineStage != null && pipelineStage.getPipelineStageElements() != null
                && pipelineStage.getPipelineStageElements().stream().allMatch(pipelineStageElement -> {
                     if (pipelineStageElement.getType().equals(StateType.APPROVAL.name())) {
                       return true;
                     }

                     if (pipelineStageElement.getProperties() == null) {
                       return false;
                     }

                     Object stageEnvIdObj = pipelineStageElement.getProperties().get("envId");
                     if (stageEnvIdObj == null) {
                       return true;
                     }
                     // TODO: For now we are comparing if env has expression then not check for env permissions
                     // TODO: We should find a better way of handling
                     String stageEnvId = (String) stageEnvIdObj;
                     if (ManagerExpressionEvaluator.matchesVariablePattern(stageEnvId)) {
                       return true;
                     }
                     if (envIds.contains(stageEnvId)) {
                       return true;
                     } else if (envIdsFromOtherPermissions.contains(stageEnvId)) {
                       entityActions.retainAll(envActionMap.get(stageEnvId));
                       return true;
                     }

                     return false;
                   }));
      }

      if (match) {
        pipelineActionMap.putAll(pipeline.getUuid(), entityActions);
      }
    });
    return pipelineActionMap;
  }

  public boolean checkIfPipelineHasOnlyGivenEnvs(Pipeline pipeline, Set<String> allowedEnvIds) {
    if (pipeline.getPipelineStages() == null) {
      return true;
    }

    return pipeline.getPipelineStages().stream().allMatch(pipelineStage
        -> pipelineStage != null && pipelineStage.getPipelineStageElements() != null
            && pipelineStage.getPipelineStageElements().stream().allMatch(pipelineStageElement -> {
                 if (pipelineStageElement.getType().equals(StateType.APPROVAL.name())) {
                   return true;
                 }

                 if (pipelineStageElement.getProperties() == null) {
                   return false;
                 }

                 Object stageEnvIdObj = pipelineStageElement.getProperties().get("envId");
                 if (stageEnvIdObj == null) {
                   return true;
                 }
                 // TODO: For now we are comparing if env has expression then not check for env permissions
                 // TODO: We should find a better way of handling
                 String stageEnvId = (String) stageEnvIdObj;
                 if (ManagerExpressionEvaluator.matchesVariablePattern(stageEnvId)) {
                   return true;
                 }

                 if (isEmpty(allowedEnvIds)) {
                   return false;
                 }

                 if (allowedEnvIds.contains(stageEnvId)) {
                   return true;
                 }

                 return false;
               }));
  }

  public boolean isEnvTemplatized(Workflow workflow) {
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
    if (CollectionUtils.isNotEmpty(templateExpressions)) {
      return templateExpressions.stream()
          .filter(templateExpression -> templateExpression.getFieldName().equals("envId"))
          .findFirst()
          .isPresent();
    }
    return false;
  }

  private boolean hasEnvSelectedType(EnvFilter envFilter) {
    Set<String> filterTypes = envFilter.getFilterTypes();
    if (isEmpty(filterTypes)) {
      return false;
    }

    return filterTypes.stream()
        .filter(filterType -> filterType.equals(EnvFilter.FilterType.SELECTED))
        .findFirst()
        .isPresent();
  }

  private void setAppPermissionMap(UserPermissionInfo userPermissionInfo) {
    Map<String, AppPermissionSummary> fromAppPermissionSummaryMap = userPermissionInfo.getAppPermissionMapInternal();
    Map<String, AppPermissionSummaryForUI> toAppPermissionSummaryMap = new HashMap<>();
    if (MapUtils.isEmpty(fromAppPermissionSummaryMap)) {
      userPermissionInfo.setAppPermissionMap(toAppPermissionSummaryMap);
    }

    fromAppPermissionSummaryMap.forEach((key, summary) -> {
      AppPermissionSummaryForUI toAppPermissionSummary = convertAppSummaryToAppSummaryForUI(summary);
      toAppPermissionSummaryMap.put(key, toAppPermissionSummary);
    });

    userPermissionInfo.setAppPermissionMap(toAppPermissionSummaryMap);
  }

  private AppPermissionSummaryForUI convertAppSummaryToAppSummaryForUI(AppPermissionSummary fromSummary) {
    AppPermissionSummaryForUIBuilder toAppPermissionSummaryBuilder =
        AppPermissionSummaryForUI.builder()
            .canCreateService(fromSummary.isCanCreateService())
            .canCreateProvisioner(fromSummary.isCanCreateProvisioner())
            .canCreateEnvironment(fromSummary.isCanCreateEnvironment())
            .canCreateWorkflow(fromSummary.isCanCreateWorkflow())
            .canCreatePipeline(fromSummary.isCanCreatePipeline())
            .servicePermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getServicePermissions()))
            .provisionerPermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getProvisionerPermissions()))
            .envPermissions(convertActionEnvMapToEnvActionMap(fromSummary.getEnvPermissions()))
            .workflowPermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getWorkflowPermissions()))
            .pipelinePermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getPipelinePermissions()))
            .deploymentPermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getDeploymentPermissions()));
    return toAppPermissionSummaryBuilder.build();
  }

  /**
   * Transforms the Map -> (Action, Set of EntityIds) to Map -> (EntityId, Set of Actions)
   * The second format is organized in the way optimized for UI consumption.
   * @param fromMap
   * @return
   */
  private Map<String, Set<Action>> convertActionEntityIdMapToEntityActionMap(Map<Action, Set<String>> fromMap) {
    Map<String, Set<Action>> toMap = new HashMap<>();
    if (isEmpty(fromMap)) {
      return null;
    }

    fromMap.forEach((action, entitySet) -> {
      if (CollectionUtils.isNotEmpty(entitySet)) {
        entitySet.forEach(entityId -> {
          Set<Action> actionSet = toMap.get(entityId);
          if (actionSet == null) {
            actionSet = new HashSet<>();
          }
          actionSet.add(action);
          toMap.put(entityId, actionSet);
        });
      }
    });

    return toMap;
  }

  private Map<String, Set<Action>> convertActionEnvMapToEnvActionMap(Map<Action, Set<EnvInfo>> fromMap) {
    Map<String, Set<Action>> toMap = new HashMap<>();
    if (isEmpty(fromMap)) {
      return toMap;
    }

    fromMap.forEach((action, envInfoSet) -> {
      if (CollectionUtils.isNotEmpty(envInfoSet)) {
        envInfoSet.forEach(envInfo -> {
          Set<Action> actionSet = toMap.get(envInfo.getEnvId());
          if (actionSet == null) {
            actionSet = new HashSet<>();
          }
          actionSet.add(action);
          toMap.put(envInfo.getEnvId(), actionSet);
        });
      }
    });

    return toMap;
  }

  public UserGroup buildDefaultAdminUserGroup(String accountId, User user) {
    AccountPermissions accountPermissions =
        AccountPermissions.builder().permissions(getAllAccountPermissions()).build();

    Set<AppPermission> appPermissions = Sets.newHashSet();
    AppPermission appPermission = AppPermission.builder()
                                      .actions(getAllActions())
                                      .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                      .permissionType(PermissionType.ALL_APP_ENTITIES)
                                      .build();
    appPermissions.add(appPermission);

    NotificationSettings notificationSettings =
        new NotificationSettings(true, true, Collections.emptyList(), SlackNotificationSetting.emptyConfig(), "");

    UserGroupBuilder userGroupBuilder = UserGroup.builder()
                                            .accountId(accountId)
                                            .name(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME)
                                            .isDefault(true)
                                            .accountPermissions(accountPermissions)
                                            .appPermissions(appPermissions)
                                            .notificationSettings(notificationSettings)
                                            .description(UserGroupServiceImpl.DEFAULT_USER_GROUP_DESCRIPTION);

    if (user != null) {
      userGroupBuilder.memberIds(asList(user.getUuid()));
    }

    return userGroupBuilder.build();
  }

  public UserGroup buildReadOnlyUserGroup(String accountId, User user, String userGroupName) {
    Set<AppPermission> appPermissions = Sets.newHashSet();
    AppPermission appPermission = AppPermission.builder()
                                      .actions(Sets.newHashSet(Action.READ))
                                      .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                      .permissionType(PermissionType.ALL_APP_ENTITIES)
                                      .build();
    appPermissions.add(appPermission);

    UserGroupBuilder userGroupBuilder = UserGroup.builder()
                                            .accountId(accountId)
                                            .name(userGroupName)
                                            .isDefault(true)
                                            .appPermissions(appPermissions)
                                            .description("Default account admin user group");
    if (user != null) {
      userGroupBuilder.memberIds(asList(user.getUuid()));
    }

    return userGroupBuilder.build();
  }

  private UserGroup buildSupportUserGroup(
      String accountId, String envFilterType, String userGroupName, String description, boolean isDefault) {
    // For Account level permissions, only add AUDIT_VIEW permission
    AccountPermissions accountPermissions =
        AccountPermissions.builder().permissions(Sets.newHashSet(AUDIT_VIEWER)).build();

    Set<Action> actions = getAllNonDeploymentActions();
    Set<AppPermission> appPermissions = Sets.newHashSet();
    AppPermission svcPermission = AppPermission.builder()
                                      .actions(actions)
                                      .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                      .entityFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                      .permissionType(PermissionType.SERVICE)
                                      .build();
    appPermissions.add(svcPermission);

    AppPermission provisionerPermission =
        AppPermission.builder()
            .actions(actions)
            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .entityFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .permissionType(PermissionType.PROVISIONER)
            .build();
    appPermissions.add(provisionerPermission);

    AppPermission envPermission = AppPermission.builder()
                                      .actions(actions)
                                      .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                      .entityFilter(new EnvFilter(null, Sets.newHashSet(envFilterType)))
                                      .permissionType(PermissionType.ENV)
                                      .build();
    appPermissions.add(envPermission);

    AppPermission workflowPermission =
        AppPermission.builder()
            .actions(actions)
            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .entityFilter(new WorkflowFilter(null, Sets.newHashSet(envFilterType, WorkflowFilter.FilterType.TEMPLATES)))
            .permissionType(PermissionType.WORKFLOW)
            .build();
    appPermissions.add(workflowPermission);

    AppPermission deploymentPermission =
        AppPermission.builder()
            .actions(Sets.newHashSet(Action.READ, Action.EXECUTE))
            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .entityFilter(new EnvFilter(null, Sets.newHashSet(envFilterType)))
            .permissionType(PermissionType.DEPLOYMENT)
            .build();
    appPermissions.add(deploymentPermission);

    AppPermission pipelinePermission = AppPermission.builder()
                                           .actions(actions)
                                           .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                           .entityFilter(new EnvFilter(null, Sets.newHashSet(envFilterType)))
                                           .permissionType(PermissionType.PIPELINE)
                                           .build();
    appPermissions.add(pipelinePermission);

    UserGroupBuilder userGroupBuilder = UserGroup.builder()
                                            .accountId(accountId)
                                            .name(userGroupName)
                                            .isDefault(isDefault)
                                            .appPermissions(appPermissions)
                                            .accountPermissions(accountPermissions)
                                            .description(description);

    return userGroupBuilder.build();
  }

  public UserGroup buildProdSupportUserGroup(String accountId) {
    return buildSupportUserGroup(
        accountId, PROD, DEFAULT_PROD_SUPPORT_USER_GROUP_NAME, DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION, true);
  }

  public UserGroup buildNonProdSupportUserGroup(String accountId) {
    return buildSupportUserGroup(accountId, NON_PROD, DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME,
        DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION, true);
  }

  private Set<PermissionType> getAllAccountPermissions() {
    return Sets.newHashSet(USER_PERMISSION_MANAGEMENT, ACCOUNT_MANAGEMENT, APPLICATION_CREATE_DELETE,
        TEMPLATE_MANAGEMENT, USER_PERMISSION_READ, AUDIT_VIEWER, TAG_MANAGEMENT);
  }

  private Set<Action> getAllActions() {
    return Sets.newHashSet(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE);
  }

  private Set<Action> getAllNonDeploymentActions() {
    return Sets.newHashSet(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE);
  }

  public void addUserToDefaultAccountAdminUserGroup(User user, Account account, boolean sendNotification) {
    if (account == null) {
      logger.info("account is null, continuing....");
      return;
    }

    String accountId = account.getUuid();

    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter("accountId", EQ, accountId)
                                             .addFilter("name", EQ, DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME)
                                             .build();
    PageResponse<UserGroup> userGroups = userGroupService.list(accountId, pageRequest, true);
    UserGroup userGroup = null;
    if (CollectionUtils.isNotEmpty(userGroups)) {
      userGroup = userGroups.get(0);
    }

    if (userGroup == null) {
      logger.info("UserGroup doesn't exist in account {}", accountId);
      userGroup = buildDefaultAdminUserGroup(accountId, user);

      UserGroup savedUserGroup = userGroupService.save(userGroup);
      logger.info("Created default user group {} for account {}", savedUserGroup.getUuid(), accountId);
    } else {
      logger.info("UserGroup already exists in account {}", accountId);
      logger.info(
          "Checking if user {} exists in user group {} in account {}", user.getName(), userGroup.getUuid(), accountId);

      List<String> memberIds = userGroup.getMemberIds();
      boolean userMemberOfGroup;
      if (isEmpty(memberIds)) {
        userMemberOfGroup = false;
      } else {
        userMemberOfGroup = memberIds.contains(user.getUuid());
      }

      if (!userMemberOfGroup) {
        logger.info("User {} is not part of the user group in account {}, adding now ", user.getName(), accountId);
        List<User> members = userGroup.getMembers();
        if (members == null) {
          members = new ArrayList<>();
        }

        members.add(user);
        userGroup.setMembers(members);

        userGroupService.updateMembers(userGroup, sendNotification);

        logger.info("User {} is added to the user group in account {}", user.getName(), accountId);
      }
    }
  }

  public void createDefaultUserGroups(Account account) {
    UserGroup defaultAdminUserGroup = buildDefaultAdminUserGroup(account.getUuid(), null);
    userGroupService.save(defaultAdminUserGroup);

    // We don't create prodSupportUserGroup and nonProdSupportUserGroup for
    // Community account
    if (!account.isCommunity()) {
      // By default, we don't associate any users to the support groups
      UserGroup prodSupportUserGroup = buildProdSupportUserGroup(account.getUuid());
      userGroupService.save(prodSupportUserGroup);
      UserGroup nonProdSupportUserGroup = buildNonProdSupportUserGroup(account.getUuid());
      userGroupService.save(nonProdSupportUserGroup);
    }
  }

  public void authorizeAccountPermission(List<PermissionAttribute> requiredPermissionAttributes) {
    User user = UserThreadLocal.get();
    if (user != null) {
      UserRequestContext userRequestContext = user.getUserRequestContext();
      // UserRequestContext is null if rbac enabled is false
      if (userRequestContext != null) {
        authorizeAccountPermission(userRequestContext, requiredPermissionAttributes);
      }
    }
  }

  public void authorizeAccountPermission(
      UserRequestContext userRequestContext, List<PermissionAttribute> requiredPermissionAttributes) {
    UserPermissionInfo userPermissionInfo = userRequestContext.getUserPermissionInfo();
    if (userPermissionInfo == null) {
      throw new InvalidRequestException("User not authorized", USER);
    }

    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    if (accountPermissionSummary == null) {
      throw new InvalidRequestException("User not authorized", USER);
    }

    Set<PermissionType> accountPermissions = accountPermissionSummary.getPermissions();
    if (accountPermissions == null) {
      throw new InvalidRequestException("User not authorized", USER);
    }

    if (isAuthorized(requiredPermissionAttributes, accountPermissions)) {
      return;
    } else {
      throw new InvalidRequestException("User not authorized", USER);
    }
  }

  private boolean isAuthorized(List<PermissionAttribute> permissionAttributes, Set<PermissionType> accountPermissions) {
    return permissionAttributes.stream().anyMatch(
        permissionAttribute -> accountPermissions.contains(permissionAttribute.getPermissionType()));
  }

  public void authorizeScimApi(ContainerRequestContext containerRequestContext) {
    String apiKey = getToken(containerRequestContext.getHeaderString("Authorization"), "Bearer");
    String accountId = getRequestParamFromContext("accountId", containerRequestContext.getUriInfo().getPathParameters(),
        containerRequestContext.getUriInfo().getQueryParameters());

    ApiKeyEntry apiKeyEntry = apiKeyService.getByKey(apiKey, accountId, true);
    if (apiKeyEntry == null) {
      throw new InvalidRequestException("User not authorized", USER);
    }

    UserPermissionInfo userPermissionInfo = apiKeyService.getApiKeyPermissions(apiKeyEntry, accountId);

    logger.info("SCIM permissions: {}", userPermissionInfo.getAccountPermissionSummary().getPermissions());
    if (!userPermissionInfo.getAccountPermissionSummary().getPermissions().contains(USER_PERMISSION_MANAGEMENT)) {
      throw new InvalidRequestException("User not authorized", USER);
    }
  }

  private String getToken(String authorizationHeader, String prefix) {
    if (!authorizationHeader.contains(prefix)) {
      throw new InvalidRequestException("User not authorized", USER);
    }
    return authorizationHeader.substring(prefix.length()).trim();
  }

  private String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    return queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : pathParameters.getFirst(key);
  }
}
