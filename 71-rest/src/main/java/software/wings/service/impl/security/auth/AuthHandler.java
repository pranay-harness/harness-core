package software.wings.service.impl.security.auth;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.noop;
import static java.util.Arrays.asList;
import static software.wings.beans.FeatureName.RBAC;
import static software.wings.common.Constants.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;
import static software.wings.common.Constants.DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION;
import static software.wings.common.Constants.DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.common.Constants.DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION;
import static software.wings.common.Constants.DEFAULT_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.GenericEntityFilter.FilterType.SELECTED;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.PIPELINE;
import static software.wings.security.PermissionAttribute.PermissionType.PROVISIONER;
import static software.wings.security.PermissionAttribute.PermissionType.SERVICE;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.WORKFLOW;
import static software.wings.security.UserRequestContext.EntityInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
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
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupBuilder;
import software.wings.expression.ExpressionEvaluator;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AccountPermissionSummary.AccountPermissionSummaryBuilder;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.AppPermissionSummaryBuilder;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.AppPermissionSummaryForUI;
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
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
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

/**
 * @author rktummala on 3/7/18
 */
@Singleton
public class AuthHandler {
  private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);

  @Inject private PipelineService pipelineService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowService workflowService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private UserGroupService userGroupService;
  @Inject private AuthService authService;

  public UserPermissionInfo getUserPermissionInfo(String accountId, List<UserGroup> userGroups) {
    UserPermissionInfoBuilder userPermissionInfoBuilder = UserPermissionInfo.builder().accountId(accountId);

    boolean enabled = featureFlagService.isEnabled(RBAC, accountId);
    userPermissionInfoBuilder.isRbacEnabled(enabled);

    Set<PermissionType> accountPermissionSet = new HashSet<>();
    AccountPermissionSummaryBuilder accountPermissionSummaryBuilder =
        AccountPermissionSummary.builder().permissions(accountPermissionSet);

    populateRequiredAccountPermissions(userGroups, accountPermissionSet);

    // Get all app ids
    HashSet<String> allAppIds = new HashSet<>(appService.getAppIdsByAccountId(accountId));

    // Cache all the entities by app id first
    Map<PermissionType, Set<String>> permissionTypeAppIdSetMap = collectRequiredAppIds(userGroups, allAppIds);

    // Let's fetch all entities by appIds
    Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap =
        fetchRequiredEntities(permissionTypeAppIdSetMap);

    // Filter and assign permissions
    Map<String, AppPermissionSummaryForUI> appPermissionMap =
        populateAppPermissions(userGroups, permissionTypeAppIdEntityMap, allAppIds);

    userPermissionInfoBuilder.appPermissionMap(appPermissionMap)
        .accountPermissionSummary(accountPermissionSummaryBuilder.build());

    UserPermissionInfo userPermissionInfo = userPermissionInfoBuilder.build();
    setAppPermissionMapInternal(userPermissionInfo, permissionTypeAppIdEntityMap.get(PermissionType.ENV));
    return userPermissionInfo;
  }

  private Map<String, AppPermissionSummaryForUI> populateAppPermissions(List<UserGroup> userGroups,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, HashSet<String> allAppIds) {
    Map<String, AppPermissionSummaryForUI> appPermissionMap = new HashMap<>();
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

  private void attachPermission(Map<String, AppPermissionSummaryForUI> appPermissionMap,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, Set<String> appIds,
      PermissionType permissionType, Filter entityFilter, Set<Action> actions) {
    final HashSet<Action> fixedEntityActions =
        Sets.newHashSet(Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE);
    appIds.forEach(appId -> {
      AppPermissionSummaryForUI appPermissionSummaryForUI = appPermissionMap.get(appId);
      if (appPermissionSummaryForUI == null) {
        appPermissionSummaryForUI = new AppPermissionSummaryForUI();
        appPermissionMap.put(appId, appPermissionSummaryForUI);
      }

      SetView<Action> intersection = Sets.intersection(fixedEntityActions, actions);
      Set<Action> entityActions = new HashSet<>(intersection);

      switch (permissionType) {
        case SERVICE: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummaryForUI.setCanCreateService(true);
          }
          if (isEmpty(entityActions)) {
            break;
          }

          Set<String> entityIds = getServiceIdsByFilter(
              permissionTypeAppIdEntityMap.get(permissionType).get(appId), (GenericEntityFilter) entityFilter);
          AppPermissionSummaryForUI finalAppPermissionSummaryForUI = appPermissionSummaryForUI;
          entityIds.forEach(entityId -> {
            Map<String, Set<Action>> permissionMap =
                setActionsForEntity(finalAppPermissionSummaryForUI.getServicePermissions(), entityId, entityActions);
            finalAppPermissionSummaryForUI.setServicePermissions(permissionMap);
          });

          break;
        }
        case PROVISIONER: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummaryForUI.setCanCreateProvisioner(true);
          }
          if (isEmpty(entityActions)) {
            break;
          }
          Set<String> entityIds = getProvisionerIdsByFilter(
              permissionTypeAppIdEntityMap.get(permissionType).get(appId), (GenericEntityFilter) entityFilter);
          AppPermissionSummaryForUI finalAppPermissionSummaryForUI = appPermissionSummaryForUI;
          entityIds.forEach(entityId -> {
            Map<String, Set<Action>> permissionMap = setActionsForEntity(
                finalAppPermissionSummaryForUI.getProvisionerPermissions(), entityId, entityActions);
            finalAppPermissionSummaryForUI.setProvisionerPermissions(permissionMap);
          });

          break;
        }
        case ENV: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummaryForUI.setCanCreateEnvironment(true);
          }
          if (isEmpty(entityActions)) {
            break;
          }
          Set<String> entityIds =
              getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(permissionType).get(appId), (EnvFilter) entityFilter);
          AppPermissionSummaryForUI finalAppPermissionSummaryForUI = appPermissionSummaryForUI;

          entityIds.forEach(entityId -> {
            Map<String, Set<Action>> permissionMap =
                setActionsForEntity(finalAppPermissionSummaryForUI.getEnvPermissions(), entityId, entityActions);
            finalAppPermissionSummaryForUI.setEnvPermissions(permissionMap);
          });

          break;
        }
        case WORKFLOW: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummaryForUI.setCanCreateWorkflow(true);
          }
          if (isEmpty(entityActions)) {
            break;
          }

          Set<String> entityIds = getWorkflowIdsByFilter(permissionTypeAppIdEntityMap.get(permissionType).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (WorkflowFilter) entityFilter);
          AppPermissionSummaryForUI finalAppPermissionSummaryForUI = appPermissionSummaryForUI;

          entityIds.forEach(entityId -> {
            Map<String, Set<Action>> permissionMap =
                setActionsForEntity(finalAppPermissionSummaryForUI.getWorkflowPermissions(), entityId, entityActions);
            finalAppPermissionSummaryForUI.setWorkflowPermissions(permissionMap);
          });

          break;
        }
        case DEPLOYMENT: {
          if (isEmpty(entityActions)) {
            break;
          }
          Set<String> entityIds = getDeploymentIdsByFilter(permissionTypeAppIdEntityMap.get(WORKFLOW).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, appId);

          AppPermissionSummaryForUI finalAppPermissionSummaryForUI = appPermissionSummaryForUI;
          entityIds.forEach(entityId -> {
            Map<String, Set<Action>> permissionMap =
                setActionsForEntity(finalAppPermissionSummaryForUI.getDeploymentPermissions(), entityId, entityActions);
            finalAppPermissionSummaryForUI.setDeploymentPermissions(permissionMap);
          });
          break;
        }
        default:
          noop();
      }
    });
  }

  private void attachPipelinePermission(Multimap<String, Action> envActionMap,
      Map<String, AppPermissionSummaryForUI> appPermissionMap,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, Set<String> appIds,
      PermissionType permissionType, Filter entityFilter, Set<Action> actions) {
    final HashSet<Action> fixedEntityActions =
        Sets.newHashSet(Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE);
    appIds.forEach(appId -> {
      AppPermissionSummaryForUI appPermissionSummaryForUI = appPermissionMap.get(appId);
      if (appPermissionSummaryForUI == null) {
        appPermissionSummaryForUI = new AppPermissionSummaryForUI();
        appPermissionMap.put(appId, appPermissionSummaryForUI);
      }

      SetView<Action> intersection = Sets.intersection(fixedEntityActions, actions);
      Set<Action> entityActions = new HashSet<>(intersection);
      AppPermissionSummaryForUI finalAppPermissionSummaryForUI = appPermissionSummaryForUI;
      Multimap<String, Action> pipelineIdActionMap;
      switch (permissionType) {
        case PIPELINE:
          if (actions.contains(Action.CREATE)) {
            appPermissionSummaryForUI.setCanCreatePipeline(true);
          }

          if (isEmpty(entityActions)) {
            break;
          }

          pipelineIdActionMap = getPipelineIdsByFilter(permissionTypeAppIdEntityMap.get(PIPELINE).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, envActionMap, entityActions);
          pipelineIdActionMap.asMap().forEach((pipelineId, action) -> {
            Map<String, Set<Action>> permissionMap =
                setActionsForEntity(finalAppPermissionSummaryForUI.getPipelinePermissions(), pipelineId, action);
            finalAppPermissionSummaryForUI.setPipelinePermissions(permissionMap);
          });
          break;

        case DEPLOYMENT:
          if (isEmpty(entityActions)) {
            break;
          }

          pipelineIdActionMap = getPipelineIdsByFilter(permissionTypeAppIdEntityMap.get(PIPELINE).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, envActionMap, entityActions);

          pipelineIdActionMap.asMap().forEach((pipelineId, action) -> {
            Map<String, Set<Action>> permissionMap =
                setActionsForEntity(finalAppPermissionSummaryForUI.getDeploymentPermissions(), pipelineId, action);
            finalAppPermissionSummaryForUI.setDeploymentPermissions(permissionMap);
          });
          break;

        default:
          noop();
      }
    });
  }

  private Map<PermissionType, Map<String, List<Base>>> fetchRequiredEntities(
      Map<PermissionType, Set<String>> permissionTypeAppIdSetMap) {
    Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap = new HashMap<>();
    permissionTypeAppIdSetMap.keySet().forEach(permissionType -> {
      switch (permissionType) {
        case SERVICE: {
          permissionTypeAppIdEntityMap.put(
              permissionType, getAppIdServiceMap(permissionTypeAppIdSetMap.get(permissionType)));
          break;
        }
        case PROVISIONER: {
          permissionTypeAppIdEntityMap.put(
              permissionType, getAppIdProvisionerMap(permissionTypeAppIdSetMap.get(permissionType)));
          break;
        }
        case ENV: {
          permissionTypeAppIdEntityMap.put(
              permissionType, getAppIdEnvMap(permissionTypeAppIdSetMap.get(permissionType)));
          break;
        }
        case WORKFLOW: {
          permissionTypeAppIdEntityMap.put(
              permissionType, getAppIdWorkflowMap(permissionTypeAppIdSetMap.get(permissionType)));
          break;
        }
        case PIPELINE: {
          permissionTypeAppIdEntityMap.put(
              permissionType, getAppIdPipelineMap(permissionTypeAppIdSetMap.get(permissionType)));
          break;
        }
        default: { noop(); }
      }
    });
    return permissionTypeAppIdEntityMap;
  }

  private Map<String, List<Base>> getAppIdServiceMap(Set<String> appIds) {
    if (isEmpty(appIds)) {
      return new HashMap<>();
    }
    PageRequest<Service> pageRequest =
        aPageRequest().addFilter("appId", Operator.IN, appIds.toArray()).addFieldsIncluded("_id", "appId").build();
    List<Service> list = getAllEntities(pageRequest, () -> serviceResourceService.list(pageRequest, false, false));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdProvisionerMap(Set<String> appIds) {
    if (isEmpty(appIds)) {
      return new HashMap<>();
    }
    PageRequest<InfrastructureProvisioner> pageRequest =
        aPageRequest().addFilter("appId", Operator.IN, appIds.toArray()).addFieldsIncluded("_id", "appId").build();

    List<InfrastructureProvisioner> list =
        getAllEntities(pageRequest, () -> infrastructureProvisionerService.list(pageRequest));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdEnvMap(Set<String> appIds) {
    if (isEmpty(appIds)) {
      return new HashMap<>();
    }
    PageRequest<Environment> pageRequest = aPageRequest()
                                               .addFilter("appId", Operator.IN, appIds.toArray())
                                               .addFieldsIncluded("_id", "appId", "environmentType")
                                               .build();

    List<Environment> list = getAllEntities(pageRequest, () -> environmentService.list(pageRequest, false));

    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  <T extends Base> List<T> getAllEntities(PageRequest<T> pageRequest, Callable<PageResponse<T>> callable) {
    List<T> result = Lists.newArrayList();
    long currentPageSize;
    long total = 0;
    long countSoFar = 0;

    try {
      do {
        pageRequest.setOffset(Long.toString(countSoFar));
        PageResponse<T> pageResponse = callable.call();
        total = pageResponse.getTotal();
        List<T> listFromResponse = pageResponse.getResponse();
        if (isEmpty(listFromResponse)) {
          break;
        }
        currentPageSize = listFromResponse.size();
        countSoFar += currentPageSize;
        result.addAll(listFromResponse);
      } while (countSoFar < total);

    } catch (Exception e) {
      logger.error("Error while retrieving the entities for auth mgmt");
      throw new WingsException(e);
    }
    return result;
  }

  private Map<String, List<Base>> getAppIdWorkflowMap(Set<String> appIds) {
    if (isEmpty(appIds)) {
      return new HashMap<>();
    }

    PageRequest<Workflow> pageRequest =
        aPageRequest()
            .addFilter("appId", Operator.IN, appIds.toArray())
            .addFieldsIncluded("_id", "appId", "envId", "templatized", "templateExpressions")
            .build();

    List<Workflow> list =
        getAllEntities(pageRequest, () -> workflowService.listWorkflowsWithoutOrchestration(pageRequest));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdPipelineMap(Set<String> appIds) {
    if (isEmpty(appIds)) {
      return new HashMap<>();
    }
    PageRequest<Pipeline> pageRequest = aPageRequest().addFilter("appId", Operator.IN, appIds.toArray()).build();
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

    // pipeline will need workflow
    permissionTypeAppIdSetMap.get(WORKFLOW).addAll(permissionTypeAppIdSetMap.get(PIPELINE));

    // workflow will need env
    permissionTypeAppIdSetMap.get(ENV).addAll(permissionTypeAppIdSetMap.get(WORKFLOW));

    // DEPLOYMENT will need env
    permissionTypeAppIdSetMap.get(ENV).addAll(permissionTypeAppIdSetMap.get(DEPLOYMENT));

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
      String msg = "Unknown app filter type: " + appFilter.getFilterType();
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  public Set<String> getAppIdsByFilter(String accountId, GenericEntityFilter appFilter) {
    List<String> appIdsByAccountId = appService.getAppIdsByAccountId(accountId);
    return getAppIdsByFilter(Sets.newHashSet(appIdsByAccountId), appFilter);
  }

  public Set<String> getEnvIdsByFilter(Set<Environment> appIds, EnvFilter envFilter) {
    PageRequest<Environment> pageRequest = aPageRequest()
                                               .addFilter("appId", Operator.IN, appIds.toArray(new String[0]))
                                               .addFieldsIncluded("_id", "environmentType")
                                               .build();
    List<Environment> envList = getAllEntities(pageRequest, () -> environmentService.list(pageRequest, false));

    return getEnvIdsByFilter(envList, envFilter);
  }

  public Set<String> getEnvIdsByFilter(String appId, EnvFilter envFilter) {
    PageRequest<Environment> pageRequest =
        aPageRequest().addFilter("appId", Operator.EQ, appId).addFieldsIncluded("_id", "environmentType").build();
    List<Environment> envList = getAllEntities(pageRequest, () -> environmentService.list(pageRequest, false));

    return getEnvIdsByFilter(envList, envFilter);
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

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE") // TODO
  public boolean authorize(
      List<PermissionAttribute> requiredPermissionAttributes, List<String> appIds, String entityId) {
    User user = UserThreadLocal.get();
    // UserRequestContext is null if rbac enabled is false
    UserRequestContext userRequestContext = user.getUserRequestContext();
    if (user != null && userRequestContext != null) {
      authService.authorize(userRequestContext.getAccountId(), appIds, entityId, user, requiredPermissionAttributes);
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
                throw new WingsException("Invalid permission type: " + permissionType);
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
      throw new WingsException(msg);
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
      throw new WingsException(msg);
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

  private Set<String> getWorkflowIdsByFilter(
      List<Base> workflows, List<Base> environments, WorkflowFilter workflowFilter) {
    if (workflows == null) {
      return new HashSet<>();
    }

    if (workflowFilter == null || isEmpty(workflowFilter.getFilterTypes())) {
      workflowFilter = new WorkflowFilter();
      workflowFilter.setFilterTypes(Sets.newHashSet(PROD, NON_PROD, WorkflowFilter.FilterType.TEMPLATES));
    }

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
                     if (ExpressionEvaluator.matchesVariablePattern(stageEnvId)) {
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

  private boolean isEnvTemplatized(Workflow workflow) {
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

  private void setAppPermissionMapInternal(UserPermissionInfo userPermissionInfo, Map<String, List<Base>> appIdEnvMap) {
    Map<String, AppPermissionSummaryForUI> fromAppPermissionMap = userPermissionInfo.getAppPermissionMap();
    Map<String, AppPermissionSummary> toAppPermissionSummaryMap = new HashMap<>();
    if (MapUtils.isEmpty(fromAppPermissionMap)) {
      userPermissionInfo.setAppPermissionMapInternal(toAppPermissionSummaryMap);
    }

    fromAppPermissionMap.forEach((key, summary) -> {
      AppPermissionSummary toAppPermissionSummary = convertToAppSummaryInternal(summary, appIdEnvMap.get(key));
      toAppPermissionSummaryMap.put(key, toAppPermissionSummary);
    });

    userPermissionInfo.setAppPermissionMapInternal(toAppPermissionSummaryMap);
  }

  private AppPermissionSummary convertToAppSummaryInternal(
      AppPermissionSummaryForUI fromSummary, List<? extends Base> envList) {
    AppPermissionSummaryBuilder toAppPermissionSummaryBuilder =
        AppPermissionSummary.builder()
            .canCreateService(fromSummary.isCanCreateService())
            .canCreateProvisioner(fromSummary.isCanCreateProvisioner())
            .canCreateEnvironment(fromSummary.isCanCreateEnvironment())
            .canCreateWorkflow(fromSummary.isCanCreateWorkflow())
            .canCreatePipeline(fromSummary.isCanCreatePipeline())
            .servicePermissions(convertToInternal(fromSummary.getServicePermissions()))
            .provisionerPermissions(convertToInternal(fromSummary.getProvisionerPermissions()))
            .envPermissions(convertEnvToInternal(fromSummary.getEnvPermissions(), envList))
            .workflowPermissions(convertToInternal(fromSummary.getWorkflowPermissions()))
            .pipelinePermissions(convertToInternal(fromSummary.getPipelinePermissions()))
            .deploymentPermissions(convertToInternal(fromSummary.getDeploymentPermissions()));
    return toAppPermissionSummaryBuilder.build();
  }

  private Map<Action, Set<String>> convertToInternal(Map<String, Set<Action>> fromMap) {
    Map<Action, Set<String>> toMap = new HashMap<>();
    final Set<String> readSet = new HashSet<>();
    final Set<String> updateSet = new HashSet<>();
    final Set<String> deleteSet = new HashSet<>();
    final Set<String> executeSet = new HashSet<>();

    if (fromMap == null) {
      return new HashMap<>();
    }

    fromMap.forEach((entityId, actions) -> {
      if (CollectionUtils.isNotEmpty(actions)) {
        actions.forEach(action -> {
          if (action == Action.READ) {
            readSet.add(entityId);
          } else if (action == Action.UPDATE) {
            updateSet.add(entityId);
          } else if (action == Action.DELETE) {
            deleteSet.add(entityId);
          } else if (action == Action.EXECUTE) {
            executeSet.add(entityId);
          }
        });
      }
    });

    toMap.put(Action.READ, readSet);
    toMap.put(Action.UPDATE, updateSet);
    toMap.put(Action.DELETE, deleteSet);
    toMap.put(Action.EXECUTE, executeSet);
    return toMap;
  }

  private Map<Action, Set<EnvInfo>> convertEnvToInternal(
      Map<String, Set<Action>> fromMap, List<? extends Base> envList) {
    Map<Action, Set<EnvInfo>> toMap = new HashMap<>();
    final Set<EnvInfo> readSet = new HashSet<>();
    final Set<EnvInfo> updateSet = new HashSet<>();
    final Set<EnvInfo> deleteSet = new HashSet<>();
    final Set<EnvInfo> executeSet = new HashSet<>();

    if (fromMap == null || isEmpty(envList)) {
      return new HashMap<>();
    }

    Map<String, EnvironmentType> envIdTypeMap = envList.stream().collect(
        Collectors.toMap(env -> env.getUuid(), env -> ((Environment) env).getEnvironmentType()));

    fromMap.forEach((envId, actions) -> {
      if (CollectionUtils.isNotEmpty(actions)) {
        EnvironmentType environmentType = envIdTypeMap.get(envId);
        if (environmentType == null) {
          logger.error("Error while looking up env info for Id: {}", envId);
          return;
        }
        EnvInfo envInfo = EnvInfo.builder().envId(envId).envType(environmentType.name()).build();
        actions.forEach(action -> {
          if (action == Action.READ) {
            readSet.add(envInfo);
          } else if (action == Action.UPDATE) {
            updateSet.add(envInfo);
          } else if (action == Action.DELETE) {
            deleteSet.add(envInfo);
          } else if (action == Action.EXECUTE) {
            executeSet.add(envInfo);
          }
        });
      }
    });

    toMap.put(Action.READ, readSet);
    toMap.put(Action.UPDATE, updateSet);
    toMap.put(Action.DELETE, deleteSet);
    toMap.put(Action.EXECUTE, executeSet);
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

    UserGroupBuilder userGroupBuilder = UserGroup.builder()
                                            .accountId(accountId)
                                            .name(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME)
                                            .accountPermissions(accountPermissions)
                                            .appPermissions(appPermissions)
                                            .description("Default account admin user group");
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
                                            .appPermissions(appPermissions)
                                            .description("Default account admin user group");
    if (user != null) {
      userGroupBuilder.memberIds(asList(user.getUuid()));
    }

    return userGroupBuilder.build();
  }

  private UserGroup buildSupportUserGroup(
      String accountId, String envFilterType, String userGroupName, String description) {
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
                                            .appPermissions(appPermissions)
                                            .description(description);

    return userGroupBuilder.build();
  }

  public UserGroup buildProdSupportUserGroup(String accountId) {
    return buildSupportUserGroup(
        accountId, PROD, DEFAULT_PROD_SUPPORT_USER_GROUP_NAME, DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION);
  }

  public UserGroup buildNonProdSupportUserGroup(String accountId) {
    return buildSupportUserGroup(
        accountId, NON_PROD, DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME, DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION);
  }

  private Set<PermissionType> getAllAccountPermissions() {
    return Sets.newHashSet(USER_PERMISSION_MANAGEMENT, ACCOUNT_MANAGEMENT, APPLICATION_CREATE_DELETE);
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

  public void createDefaultUserGroups(Account account, User user) {
    UserGroup defaultAdminUserGroup = buildDefaultAdminUserGroup(account.getUuid(), user);
    userGroupService.save(defaultAdminUserGroup);

    // By default, we don't associate any users to the support groups
    UserGroup prodSupportUserGroup = buildProdSupportUserGroup(account.getUuid());
    userGroupService.save(prodSupportUserGroup);
    UserGroup nonProdSupportUserGroup = buildNonProdSupportUserGroup(account.getUuid());
    userGroupService.save(nonProdSupportUserGroup);
  }

  public <T extends Base> void setFilter(String accountId, String appId, PageRequest<T> pageRequest) {}

  public <T extends Base> void setFilter(String accountId, List<String> appIdList, PageRequest<T> pageRequest) {}
}
