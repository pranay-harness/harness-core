package software.wings.service.impl.security.auth;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.common.Constants.DEFAULT_USER_GROUP_NAME;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.security.GenericEntityFilter.FilterType.SELECTED;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.UserRequestContext.EntityInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.HttpMethod;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupBuilder;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AccountPermissionSummary.AccountPermissionSummaryBuilder;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.AppPermissionSummaryBuilder;
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
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowService workflowService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private UserGroupService userGroupService;

  public UserPermissionInfo getUserPermissionInfo(String accountId, List<UserGroup> userGroups) {
    Map<String, AppPermissionSummaryForUI> appPermissionMap = new HashMap<>();
    UserPermissionInfoBuilder userPermissionInfoBuilder =
        UserPermissionInfo.builder().accountId(accountId).appPermissionMap(appPermissionMap);

    boolean enabled = featureFlagService.isEnabled(FeatureName.RBAC, accountId);
    userPermissionInfoBuilder.isRbacEnabled(enabled);

    Set<PermissionType> accountPermissionSet = new HashSet<>();
    AccountPermissionSummaryBuilder accountPermissionSummaryBuilder =
        AccountPermissionSummary.builder().permissions(accountPermissionSet);

    userGroups.stream().forEach(userGroup -> {

      AccountPermissions accountPermissions = userGroup.getAccountPermissions();
      if (accountPermissions != null) {
        Set<PermissionType> permissions = accountPermissions.getPermissions();
        if (CollectionUtils.isNotEmpty(permissions)) {
          accountPermissionSet.addAll(permissions);
        }
      }

      Set<AppPermission> appPermissions = userGroup.getAppPermissions();
      if (CollectionUtils.isEmpty(appPermissions)) {
        return;
      }

      appPermissions.stream().forEach(appPermission -> {
        Set<String> appIdSet = getAppIdsByFilter(accountId, appPermission.getAppFilter());
        appIdSet.stream().forEach(appId -> {
          Set<String> entityIdSet;
          Filter entityFilter = appPermission.getEntityFilter();
          PermissionType permissionType = appPermission.getPermissionType();
          Set<Action> actions = appPermission.getActions();

          if (CollectionUtils.isEmpty(actions)) {
            logger.error("Actions empty for app: {}", appId);
            return;
          }

          if (permissionType == PermissionType.ALL_APP_ENTITIES) {
            entityIdSet = getServiceIdsByFilter(appId, null);
            setPermissions(appPermissionMap, appId, entityIdSet, PermissionType.SERVICE, actions);

            entityIdSet = getEnvIdsByFilter(appId, null);
            setPermissions(appPermissionMap, appId, entityIdSet, PermissionType.ENV, actions);

            entityIdSet = getWorkflowIdsByFilter(appId, null);
            setPermissions(appPermissionMap, appId, entityIdSet, PermissionType.WORKFLOW, actions);

            entityIdSet = getPipelineIdsByFilter(appId, null);
            setPermissions(appPermissionMap, appId, entityIdSet, PermissionType.PIPELINE, actions);

            entityIdSet = getDeploymentIdsByFilter(appId, null);
            setPermissions(appPermissionMap, appId, entityIdSet, PermissionType.DEPLOYMENT, actions);
            return;
          }

          if (permissionType == PermissionType.SERVICE) {
            entityIdSet =
                getServiceIdsByFilter(appId, entityFilter != null ? (GenericEntityFilter) entityFilter : null);
          } else if (permissionType == PermissionType.ENV) {
            entityIdSet = getEnvIdsByFilter(appId, entityFilter != null ? (EnvFilter) entityFilter : null);
          } else if (permissionType == PermissionType.WORKFLOW) {
            entityIdSet = getWorkflowIdsByFilter(appId, entityFilter != null ? (WorkflowFilter) entityFilter : null);
          } else if (permissionType == PermissionType.PIPELINE) {
            entityIdSet = getPipelineIdsByFilter(appId, entityFilter != null ? (EnvFilter) entityFilter : null);
          } else if (permissionType == PermissionType.DEPLOYMENT) {
            entityIdSet = getDeploymentIdsByFilter(appId, entityFilter != null ? (EnvFilter) entityFilter : null);
          } else {
            String msg = "Unsupported permission type: " + permissionType.name();
            logger.error(msg);
            throw new WingsException(msg);
          }

          setPermissions(appPermissionMap, appId, entityIdSet, permissionType, actions);
        });
      });
    });

    userPermissionInfoBuilder.accountPermissionSummary(accountPermissionSummaryBuilder.build());

    UserPermissionInfo userPermissionInfo = userPermissionInfoBuilder.build();
    setAppPermissionMapInternal(userPermissionInfo);
    return userPermissionInfo;
  }

  private Set<String> getAppIdsByFilter(String accountId, GenericEntityFilter appFilter) {
    if (appFilter == null) {
      appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    }

    String appFilterType = appFilter.getFilterType();
    if (FilterType.ALL.equals(appFilterType)) {
      return new HashSet<>(appService.getAppIdsByAccountId(accountId));
    } else if (FilterType.SELECTED.equals(appFilterType)) {
      return appFilter.getIds();
    } else {
      String msg = "Unknown app filter type: " + appFilterType;
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  public void setEntityIdFilterIfUserAction(
      List<PermissionAttribute> requiredPermissionAttributes, List<String> appIds) {
    User user = UserThreadLocal.get();
    if (user != null && user.getUserRequestContext() != null) {
      setEntityIdFilter(requiredPermissionAttributes, user.getUserRequestContext(), appIds);
    }
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
        if (permissionType == PermissionType.SERVICE) {
          entityPermissions = appPermissionSummary.getServicePermissions();
        } else if (permissionType == PermissionType.ENV) {
          entityPermissions = appPermissionSummary.getEnvPermissions();
        } else if (permissionType == PermissionType.WORKFLOW) {
          entityPermissions = appPermissionSummary.getWorkflowPermissions();
        } else if (permissionType == PermissionType.PIPELINE) {
          entityPermissions = appPermissionSummary.getPipelinePermissions();
        } else if (permissionType == PermissionType.DEPLOYMENT) {
          entityPermissions = appPermissionSummary.getDeploymentPermissions();
        }

        if (entityPermissions == null) {
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
              if (permissionType == PermissionType.SERVICE) {
                className = Service.class.getName();
              } else if (permissionType == PermissionType.ENV) {
                className = Environment.class.getName();
              } else if (permissionType == PermissionType.WORKFLOW) {
                className = Workflow.class.getName();
              } else if (permissionType == PermissionType.PIPELINE) {
                className = Pipeline.class.getName();
              } else if (permissionType == PermissionType.DEPLOYMENT) {
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

  private Set<String> getServiceIdsByFilter(String appId, GenericEntityFilter serviceFilter) {
    if (serviceFilter == null) {
      serviceFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    }

    if (FilterType.ALL.equals(serviceFilter.getFilterType())) {
      PageRequest<Service> pageRequest =
          aPageRequest().addFilter("appId", Operator.EQ, appId).addFieldsIncluded("_id").build();
      PageResponse<Service> pageResponse = serviceResourceService.list(pageRequest, false, false);
      List<Service> serviceList = pageResponse.getResponse();
      return serviceList.stream().map(service -> service.getUuid()).collect(Collectors.toSet());

    } else if (SELECTED.equals(serviceFilter.getFilterType())) {
      return new HashSet<>(serviceFilter.getIds());
    } else {
      String msg = "Unknown service filter type: " + serviceFilter.getFilterType();
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  private EnvFilter getDefaultEnvFilterIfNull(EnvFilter envFilter) {
    if (envFilter == null || CollectionUtils.isEmpty(envFilter.getFilterTypes())) {
      envFilter = new EnvFilter();
      envFilter.setFilterTypes(Sets.newHashSet(EnvFilter.FilterType.PROD, EnvFilter.FilterType.NON_PROD));
    }
    return envFilter;
  }

  private Set<String> getEnvIdsByFilter(String appId, EnvFilter envFilter) {
    envFilter = getDefaultEnvFilterIfNull(envFilter);

    Set<String> filterTypes = envFilter.getFilterTypes();

    boolean selected = hasEnvSelectedType(envFilter);
    if (selected) {
      return envFilter.getIds();
    }

    boolean allEnv = isAllEnv(filterTypes);
    PageRequest<Environment> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", Operator.EQ, appId);
    pageRequest.addFieldsIncluded("_id");

    if (!allEnv) {
      Optional<String> envFilterTypeOptional =
          filterTypes.stream().filter(filterType -> isEnvType(filterType)).findFirst();
      if (envFilterTypeOptional.isPresent()) {
        pageRequest.addFilter("environmentType", Operator.EQ, envFilterTypeOptional.get());
      }
    }

    PageResponse<Environment> pageResponse = environmentService.list(pageRequest, false);
    List<Environment> envList = pageResponse.getResponse();
    return envList.stream().map(environment -> environment.getUuid()).collect(Collectors.toSet());
  }

  private Set<String> getPipelineIdsByFilter(String appId, EnvFilter envFilter) {
    Set<String> pipelineIds;
    Set<String> envIds = getEnvIdsByFilter(appId, envFilter);

    PageRequest<Pipeline> pageRequest = aPageRequest().addFilter("appId", Operator.EQ, appId).build();
    PageResponse<Pipeline> pageResponse = pipelineService.listPipelines(pageRequest);
    List<Pipeline> pipelineList = pageResponse.getResponse();
    final Set<String> envIdsFinal = envIds;
    pipelineIds = pipelineList.stream()
                      .filter(pipeline -> {
                        final AtomicInteger envStageCount = new AtomicInteger();
                        final AtomicInteger envCount = new AtomicInteger();
                        pipeline.getPipelineStages().stream().forEach(pipelineStage -> {
                          pipelineStage.getPipelineStageElements().stream().forEach(pipelineStageElement -> {

                            // The stage type is called ENV_STATE in pipeline. The other stage type is Approval stage.
                            if (pipelineStageElement.getType().equals(StateType.ENV_STATE.name())) {
                              envStageCount.incrementAndGet();
                            }

                            Map<String, Object> properties = pipelineStageElement.getProperties();
                            if (properties != null) {
                              String envId = (String) properties.get("envId");
                              if (envIdsFinal.contains(envId)) {
                                envCount.incrementAndGet();
                              }
                            }
                          });
                        });
                        return envStageCount == envCount;
                      })
                      .map(pipeline -> pipeline.getUuid())
                      .collect(Collectors.toSet());

    return pipelineIds;
  }

  private Set<String> getWorkflowIdsByFilter(String appId, WorkflowFilter workflowFilter) {
    if (workflowFilter == null || CollectionUtils.isEmpty(workflowFilter.getFilterTypes())) {
      workflowFilter = new WorkflowFilter();
      workflowFilter.setFilterTypes(Sets.newHashSet(
          WorkflowFilter.FilterType.PROD, WorkflowFilter.FilterType.NON_PROD, WorkflowFilter.FilterType.TEMPLATES));
    }

    Set<String> envIds = getEnvIdsByFilter(appId, workflowFilter);

    if (CollectionUtils.isEmpty(envIds)) {
      logger.info("No environments matched the filter for app {}. Returning empty set of workflows", appId);
      return new HashSet<>();
    }

    PageRequestBuilder pageRequestBuilder = aPageRequest()
                                                .addFilter("appId", Operator.EQ, appId)
                                                .addFieldsIncluded("_id", "templateExpressions")
                                                .addFilter("envId", Operator.IN, envIds.toArray());

    PageRequest<Workflow> pageRequest = pageRequestBuilder.build();
    PageResponse<Workflow> pageResponse = workflowService.listWorkflows(pageRequest);
    List<Workflow> workflowList = pageResponse.getResponse();

    Set<String> workflowIdSet = workflowList.stream().map(workflow -> workflow.getUuid()).collect(Collectors.toSet());

    if (!hasTemplateFilterType(workflowFilter.getFilterTypes())) {
      Set<String> workflowIdsWithEnvTemplatized = workflowList.stream()
                                                      .filter(workflow -> isEnvTemplatized(workflow))
                                                      .map(workflow -> workflow.getUuid())
                                                      .collect(Collectors.toSet());
      workflowIdSet.removeAll(workflowIdsWithEnvTemplatized);
    }

    return workflowIdSet;
  }

  private Set<String> getDeploymentIdsByFilter(String appId, EnvFilter envFilter) {
    Set<String> envIds = getEnvIdsByFilter(appId, envFilter);
    if (CollectionUtils.isEmpty(envIds)) {
      logger.info("No environments matched the filter for app {}. Returning empty set of deployments", appId);
      return new HashSet<>();
    }

    PageRequestBuilder pageRequestBuilder = aPageRequest().addFilter("appId", Operator.EQ, appId);
    if (CollectionUtils.isNotEmpty(envIds)) {
      pageRequestBuilder.addFilter("envId", Operator.IN, envIds.toArray());
    }

    PageRequest<Workflow> pageRequest = pageRequestBuilder.build();
    PageResponse<Workflow> pageResponse = workflowService.listWorkflows(pageRequest);
    List<Workflow> workflowList = pageResponse.getResponse();

    return workflowList.stream().map(workflow -> workflow.getUuid()).collect(Collectors.toSet());
  }

  private boolean isEnvType(String filterType) {
    return EnvFilter.FilterType.PROD.equals(filterType) || EnvFilter.FilterType.NON_PROD.equals(filterType);
  }

  private boolean hasTemplateFilterType(Set<String> workflowFilterTypes) {
    if (CollectionUtils.isEmpty(workflowFilterTypes)) {
      return false;
    }

    return workflowFilterTypes.stream()
        .filter(filterType -> WorkflowFilter.FilterType.TEMPLATES.equals(filterType))
        .findFirst()
        .isPresent();
  }

  private boolean isAllEnv(Set<String> envFilterTypes) {
    boolean prodPresent = envFilterTypes.stream()
                              .filter(envFilterType -> EnvFilter.FilterType.PROD.equals(envFilterType))
                              .findFirst()
                              .isPresent();
    boolean nonProdPresent = envFilterTypes.stream()
                                 .filter(envFilterType -> EnvFilter.FilterType.NON_PROD.equals(envFilterType))
                                 .findFirst()
                                 .isPresent();
    return prodPresent && nonProdPresent;
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
    if (CollectionUtils.isEmpty(filterTypes)) {
      return false;
    }

    return filterTypes.stream()
        .filter(filterType -> filterType.equals(EnvFilter.FilterType.SELECTED))
        .findFirst()
        .isPresent();
  }

  /**
   * Sets the app permissions into the map for the given entities
   * Not using the builder pattern for AppPermissionSummary since we need to read and write in this code and builder
   * doesn't expose read methods.
   * @param appPermissionMap
   * @param appId
   * @param entityIds
   * @param permissionType
   * @param actions
   */
  private void setPermissions(Map<String, AppPermissionSummaryForUI> appPermissionMap, String appId,
      Set<String> entityIds, PermissionType permissionType, Set<Action> actions) {
    AppPermissionSummaryForUI appPermissionSummary = appPermissionMap.get(appId);

    if (appPermissionSummary == null) {
      appPermissionSummary = new AppPermissionSummaryForUI();
      appPermissionMap.put(appId, appPermissionSummary);
    }

    boolean hasCreatePermission = hasCreateAction(actions);

    if (permissionType == PermissionType.SERVICE) {
      appPermissionSummary.setCanCreateService(hasCreatePermission);
    } else if (permissionType == PermissionType.ENV) {
      appPermissionSummary.setCanCreateEnvironment(hasCreatePermission);
    } else if (permissionType == PermissionType.WORKFLOW) {
      appPermissionSummary.setCanCreateWorkflow(hasCreatePermission);
    } else if (permissionType == PermissionType.PIPELINE) {
      appPermissionSummary.setCanCreatePipeline(hasCreatePermission);
    } else if (permissionType != PermissionType.DEPLOYMENT) {
      String msg = "Unsupported app permission entity type: " + permissionType;
      logger.error(msg);
      throw new WingsException(msg);
    }

    final AppPermissionSummaryForUI appPermissionSummaryFinal = appPermissionSummary;
    entityIds.stream().forEach(entityId -> {

      Map<String, Set<Action>> map;
      if (permissionType == PermissionType.SERVICE) {
        map = addActions(appPermissionSummaryFinal.getServicePermissions(), entityId, actions);
        appPermissionSummaryFinal.setServicePermissions(map);
      } else if (permissionType == PermissionType.ENV) {
        map = addActions(appPermissionSummaryFinal.getEnvPermissions(), entityId, actions);
        appPermissionSummaryFinal.setEnvPermissions(map);
      } else if (permissionType == PermissionType.WORKFLOW) {
        map = addActions(appPermissionSummaryFinal.getWorkflowPermissions(), entityId, actions);
        appPermissionSummaryFinal.setWorkflowPermissions(map);
      } else if (permissionType == PermissionType.PIPELINE) {
        map = addActions(appPermissionSummaryFinal.getPipelinePermissions(), entityId, actions);
        appPermissionSummaryFinal.setPipelinePermissions(map);
      } else if (permissionType == PermissionType.DEPLOYMENT) {
        map = addActions(appPermissionSummaryFinal.getDeploymentPermissions(), entityId, actions);
        appPermissionSummaryFinal.setDeploymentPermissions(map);
      } else {
        String msg = "Unsupported app permission entity type: " + permissionType;
        logger.error(msg);
        throw new WingsException(msg);
      }
    });
  }

  private Map<String, Set<Action>> addActions(
      Map<String, Set<Action>> entityIdPermissionMap, String entityId, Set<Action> actions) {
    if (entityIdPermissionMap == null) {
      entityIdPermissionMap = new HashMap<>();
    }

    Set<Action> actionSet = entityIdPermissionMap.get(entityId);
    if (actionSet == null) {
      actionSet = new HashSet<>();
      entityIdPermissionMap.put(entityId, actionSet);
    }
    actionSet.addAll(actions);

    return entityIdPermissionMap;
  }

  private boolean hasCreateAction(Set<Action> actions) {
    return actions.stream().filter(action -> Action.CREATE == action).findFirst().isPresent();
  }

  private void setAppPermissionMapInternal(UserPermissionInfo userPermissionInfo) {
    Map<String, AppPermissionSummaryForUI> fromAppPermissionMap = userPermissionInfo.getAppPermissionMap();
    Map<String, AppPermissionSummary> toAppPermissionSummaryMap = new HashMap<>();
    if (MapUtils.isEmpty(fromAppPermissionMap)) {
      userPermissionInfo.setAppPermissionMapInternal(toAppPermissionSummaryMap);
    }

    fromAppPermissionMap.entrySet().stream().forEach(entry -> {
      AppPermissionSummary toAppPermissionSummary = convertToAppSummaryInternal(entry.getValue());
      toAppPermissionSummaryMap.put(entry.getKey(), toAppPermissionSummary);
    });

    userPermissionInfo.setAppPermissionMapInternal(toAppPermissionSummaryMap);
  }

  private AppPermissionSummary convertToAppSummaryInternal(AppPermissionSummaryForUI fromSummary) {
    AppPermissionSummaryBuilder toAppPermissionSummaryBuilder =
        AppPermissionSummary.builder()
            .canCreateService(fromSummary.isCanCreateService())
            .canCreateEnvironment(fromSummary.isCanCreateEnvironment())
            .canCreateWorkflow(fromSummary.isCanCreateWorkflow())
            .canCreatePipeline(fromSummary.isCanCreatePipeline())
            .servicePermissions(convertToInternal(fromSummary.getServicePermissions()))
            .envPermissions(convertToInternal(fromSummary.getEnvPermissions()))
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

    fromMap.entrySet().stream().forEach(entry -> {
      Collection<Action> actions = entry.getValue();
      final String entityId = entry.getKey();
      if (CollectionUtils.isNotEmpty(actions)) {
        actions.stream().forEach(action -> {
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
                                            .name(DEFAULT_USER_GROUP_NAME)
                                            .accountPermissions(accountPermissions)
                                            .appPermissions(appPermissions)
                                            .description("Default account admin user group");
    if (user != null) {
      userGroupBuilder.memberIds(Arrays.asList(user.getUuid()));
    }

    return userGroupBuilder.build();
  }

  private Set<PermissionType> getAllAccountPermissions() {
    return Sets.newHashSet(USER_PERMISSION_MANAGEMENT, ACCOUNT_MANAGEMENT, APPLICATION_CREATE_DELETE);
  }

  private Set<Action> getAllActions() {
    return Sets.newHashSet(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE);
  }

  public void addUserToUserGroup(User user, Account account) {
    if (account == null) {
      logger.info("account is null, continuing....");
      return;
    }

    String accountId = account.getUuid();

    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter("accountId", EQ, accountId)
                                             .addFilter("name", EQ, Constants.DEFAULT_USER_GROUP_NAME)
                                             .build();
    PageResponse<UserGroup> userGroups = userGroupService.list(accountId, pageRequest);
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
      if (CollectionUtils.isEmpty(memberIds)) {
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

        userGroupService.updateMembers(userGroup);

        logger.info("User {} is added to the user group in account {}", user.getName(), accountId);
      }
    }
  }
}
