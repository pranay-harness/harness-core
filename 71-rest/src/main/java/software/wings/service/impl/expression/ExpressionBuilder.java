package software.wings.service.impl.expression;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.noop;
import static io.harness.k8s.model.K8sExpressions.canaryDestination;
import static io.harness.k8s.model.K8sExpressions.canaryWorkload;
import static io.harness.k8s.model.K8sExpressions.stableDestination;
import static io.harness.k8s.model.K8sExpressions.virtualServiceName;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SERVICE_TEMPLATE;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_REGISTRY_URL_KEY;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_REPOSITORY_NAME_KEY;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_USER_NAME_KEY;
import static software.wings.common.Constants.DEPLOYMENT_TRIGGERED_BY;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;
import static software.wings.common.Constants.PCF_APP_NAME;
import static software.wings.common.Constants.PCF_OLD_APP_NAME;
import static software.wings.common.Constants.WINGS_BACKUP_PATH;
import static software.wings.common.Constants.WINGS_RUNTIME_PATH;
import static software.wings.common.Constants.WINGS_STAGING_PATH;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.INFRA_ROUTE_PCF;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.INFRA_TEMP_ROUTE_PCF;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.MASKED;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.sm.ContextElement.DEPLOYMENT_URL;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import software.wings.beans.EntityType;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SubEntityType;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.sm.StateType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
/**
 * Created by sgurubelli on 8/7/17.
 */
public abstract class ExpressionBuilder {
  protected static final String APP_NAME = "app.name";
  protected static final String APP_DESCRIPTION = "app.description";

  protected static final String ARTIFACT_DISPLAY_NAME = "artifact.displayName";
  protected static final String ARTIFACT_DESCRIPTION = "artifact.description";
  protected static final String ARTIFACT_BUILDNO = "artifact.buildNo";
  protected static final String ARTIFACT_REVISION = "artifact.revision";
  protected static final String ARTIFACT_FILE_NAME = "ARTIFACT_FILE_NAME";
  protected static final String ARTIFACT_ARTIFACT_FILE_NAME = "artifact.fileName";
  protected static final String ARTIFACT_BUCKET_NAME = "artifact.bucketName";
  protected static final String ARTIFACT_BUCKET_KEY = "artifact.key";
  protected static final String ARTIFACT_URL = "artifact.url";
  protected static final String ARTIFACT_BUILD_FULL_DISPLAYNAME = "artifact.buildFullDisplayName";
  protected static final String ARTIFACT_METADATA_IMAGE = "artifact." + ArtifactKeys.metadata_image;
  protected static final String ARTIFACT_METADATA_TAG = "artifact." + ArtifactKeys.metadata_tag;
  protected static final String ARTIFACT_PATH = "artifact.artifactPath";
  protected static final String ARTIFACT_SOURCE_USER_NAME = "artifact.source." + ARTIFACT_SOURCE_USER_NAME_KEY;
  protected static final String ARTIFACT_SOURCE_REGISTRY_URL = "artifact.source." + ARTIFACT_SOURCE_REGISTRY_URL_KEY;
  protected static final String ARTIFACT_SOURCE_REPOSITORY_NAME =
      "artifact.source." + ARTIFACT_SOURCE_REPOSITORY_NAME_KEY;

  protected static final String ENV_NAME = "env.name";
  protected static final String ENV_DESCRIPTION = "env.description";

  protected static final String SERVICE_NAME = "service.name";
  protected static final String SERVICE_DESCRIPTION = "service.description";
  protected static final String INFRA_NAME = "infra.name";

  protected static final String WORKFLOW_NAME = "workflow.name";
  protected static final String WORKFLOW_START_TS = "workflow.startTs";
  protected static final String WORKFLOW_DESCRIPTION = "workflow.description";
  protected static final String WORKFLOW_DISPLAY_NAME = "workflow.displayName";
  protected static final String WORKFLOW_RELEASE_NO = "workflow.releaseNo";
  protected static final String WORKFLOW_LAST_GOOD_RELEASE_NO = "workflow.lastGoodReleaseNo";
  protected static final String WORKFLOW_LAST_GOOD_DEPLOYMENT_DISPLAY_NAME = "workflow.lastGoodDeploymentDisplayName";
  protected static final String WORKFLOW_PIPELINE_DEPLOYMENT_UUID = "workflow.pipelineDeploymentUuid";

  protected static final String PIPELINE_NAME = "pipeline.name";
  protected static final String PIPELINE_DESCRIPTION = "pipeline.description";
  protected static final String PIPELINE_START_TS = "pipeline.startTs";

  protected static final String INSTANCE_NAME = "instance.name";
  protected static final String INSTANCE_HOSTNAME = "instance.hostName";
  protected static final String INSTANCE_HOST_PUBLICDNS = "instance.host.publicDns";

  protected static final String INFRA_KUBERNETES_NAMESPACE = "infra.kubernetes.namespace";
  protected static final String INFRA_KUBERNETES_INFRAID = "infra.kubernetes.infraId";
  protected static final String INFRA_HELM_SHORTID = "infra.helm.shortId";
  protected static final String INFRA_HELM_RELEASENAME = "infra.helm.releaseName";

  protected static final String APPROVEDBY_NAME = "approvedBy.name";
  protected static final String APPROVEDBY_EMAIL = "approvedBy.email";

  protected static final String EMAIl_TO_ADDRESS = "toAddress";
  protected static final String EMAIL_CC_ADDRESS = "ccAddress";
  protected static final String EMAIL_SUBJECT = "subject";
  protected static final String EMAIL_BODY = "body";

  protected static final String ASSERTION_STATEMENT = "assertionStatement";
  protected static final String ASSERTION_STATUS = "assertionStatus";

  protected static final String XPATH = "xpath('//status/text()')";
  protected static final String JSONPATH = "jsonpath('health.status')";

  protected static final String HTTP_URL = "httpUrl";
  protected static final String HTTP_RESPONSE_METHOD = "httpResponseMethod";
  protected static final String HTTP_RESPONSE_CODE = "httpResponseCode";
  protected static final String HTTP_RESPONSE_BODY = "httpResponseBody";

  @Inject private ServiceVariableService serviceVariablesService;
  @Inject private ServiceTemplateService serviceTemplateService;

  public Set<String> getExpressions(String appId, String entityId, String serviceId) {
    return getExpressions(appId, entityId);
  }

  public Set<String> getExpressions(String appId, String entityId, String serviceId, StateType stateType) {
    return getExpressions(appId, entityId);
  }

  public Set<String> getExpressions(
      String appId, String entityId, String serviceId, StateType stateType, SubEntityType subEntityType) {
    return getExpressions(appId, entityId, serviceId, stateType);
  }

  public abstract Set<String> getExpressions(String appId, String entityId);

  public abstract Set<String> getDynamicExpressions(String appId, String entityId);

  public Set<String> getExpressions(String appId, String entityId, StateType stateType) {
    if (stateType == null) {
      return getExpressions(appId, entityId);
    }
    return new HashSet<>();
  }

  public static Set<String> getStaticExpressions() {
    Set<String> expressions = new TreeSet<>();
    expressions.addAll(asList(APP_NAME, APP_DESCRIPTION));
    expressions.addAll(asList(ARTIFACT_DISPLAY_NAME, ARTIFACT_BUILDNO, ARTIFACT_REVISION, ARTIFACT_DESCRIPTION,
        ARTIFACT_FILE_NAME, ARTIFACT_ARTIFACT_FILE_NAME, ARTIFACT_BUILD_FULL_DISPLAYNAME, ARTIFACT_BUCKET_NAME,
        ARTIFACT_BUCKET_KEY, ARTIFACT_PATH, ARTIFACT_URL, ARTIFACT_SOURCE_USER_NAME, ARTIFACT_SOURCE_REGISTRY_URL,
        ARTIFACT_SOURCE_REPOSITORY_NAME, ARTIFACT_METADATA_IMAGE, ARTIFACT_METADATA_TAG));
    expressions.addAll(asList(ENV_NAME, ENV_DESCRIPTION));
    expressions.addAll(asList(SERVICE_NAME, SERVICE_DESCRIPTION));
    expressions.addAll(asList(INFRA_NAME));
    expressions.addAll(asList(WORKFLOW_NAME, WORKFLOW_DESCRIPTION, WORKFLOW_DISPLAY_NAME, WORKFLOW_RELEASE_NO,
        WORKFLOW_LAST_GOOD_DEPLOYMENT_DISPLAY_NAME, WORKFLOW_LAST_GOOD_RELEASE_NO, WORKFLOW_PIPELINE_DEPLOYMENT_UUID,
        WORKFLOW_START_TS));
    expressions.addAll(asList(PIPELINE_NAME, PIPELINE_DESCRIPTION, PIPELINE_START_TS));

    expressions.addAll(asList(INSTANCE_NAME, INSTANCE_HOSTNAME, INSTANCE_HOST_PUBLICDNS));

    expressions.addAll(asList(INFRA_KUBERNETES_NAMESPACE, INFRA_KUBERNETES_INFRAID));
    expressions.addAll(asList(INFRA_ROUTE_PCF, INFRA_TEMP_ROUTE_PCF));
    expressions.add(DEPLOYMENT_TRIGGERED_BY);

    return expressions;
  }

  public static Set<String> getStateTypeExpressions(StateType stateType) {
    Set<String> expressions = new TreeSet<>(asList(DEPLOYMENT_URL));
    switch (stateType) {
      case SHELL_SCRIPT:
        expressions.addAll(asList(WINGS_RUNTIME_PATH, WINGS_STAGING_PATH, WINGS_BACKUP_PATH, HARNESS_KUBE_CONFIG_PATH));
        break;
      case HTTP:
        expressions.addAll(asList(HTTP_URL, HTTP_RESPONSE_METHOD, HTTP_RESPONSE_CODE, HTTP_RESPONSE_BODY,
            ASSERTION_STATEMENT, ASSERTION_STATUS, XPATH, JSONPATH));
        break;
      case APPROVAL:
        expressions.addAll(asList(APPROVEDBY_NAME, APPROVEDBY_EMAIL));
        break;
      case EMAIL:
        expressions.addAll(asList(EMAIl_TO_ADDRESS, EMAIL_CC_ADDRESS, EMAIL_SUBJECT, EMAIL_BODY));
        break;
      case COMMAND:
        expressions.addAll(asList(WINGS_RUNTIME_PATH, WINGS_STAGING_PATH, WINGS_BACKUP_PATH));
        break;
      case AWS_CODEDEPLOY_STATE:
        expressions.addAll(asList(ARTIFACT_BUCKET_NAME, ARTIFACT_BUCKET_KEY, ARTIFACT_URL));
        break;
      case AWS_LAMBDA_STATE:
      case ECS_SERVICE_SETUP:
      case JENKINS:
      case BAMBOO:
      case KUBERNETES_SETUP:
      case NEW_RELIC_DEPLOYMENT_MARKER:
      case KUBERNETES_DEPLOY:
        noop();
        break;
      case PCF_SETUP:
      case PCF_RESIZE:
      case PCF_ROLLBACK:
      case PCF_MAP_ROUTE:
      case PCF_UNMAP_ROUTE:
        expressions.addAll(asList(INFRA_ROUTE_PCF, INFRA_TEMP_ROUTE_PCF, PCF_APP_NAME, PCF_OLD_APP_NAME));
        break;
      case HELM_DEPLOY:
      case HELM_ROLLBACK:
        expressions.addAll(asList(INFRA_HELM_SHORTID, INFRA_HELM_RELEASENAME));
        break;

      case K8S_TRAFFIC_SPLIT:
        expressions.addAll(asList(canaryDestination, stableDestination, virtualServiceName));
        break;

      case K8S_SCALE:
        expressions.add(canaryWorkload);
        break;

      case K8S_DELETE:
        expressions.add(canaryWorkload);
        break;

      default:
        break;
    }

    return expressions;
  }

  protected Set<String> getServiceVariables(String appId, List<String> entityIds) {
    return getServiceVariables(appId, entityIds, null);
  }

  protected Set<String> getServiceVariables(String appId, List<String> entityIds, EntityType entityType) {
    if (isEmpty(entityIds)) {
      return new TreeSet<>();
    }
    PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                  .withLimit(PageRequest.UNLIMITED)
                                                                  .addFilter(ServiceVariable.APP_ID_KEY, EQ, appId)
                                                                  .addFilter("entityId", IN, entityIds.toArray())
                                                                  .build();
    if (entityType != null) {
      serviceVariablePageRequest.addFilter("entityType", EQ, entityType);
    }
    List<ServiceVariable> serviceVariables = serviceVariablesService.list(serviceVariablePageRequest, MASKED);

    return serviceVariables.stream()
        .map(serviceVariable -> "serviceVariable." + serviceVariable.getName())
        .collect(Collectors.toSet());
  }

  protected Set<String> getServiceVariablesOfTemplates(
      String appId, PageRequest<ServiceTemplate> pageRequest, EntityType entityType) {
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.list(pageRequest, false, OBTAIN_VALUE);
    SortedSet<String> serviceVariables = new TreeSet<>();
    if (SERVICE.equals(entityType)) {
      return getServiceVariables(
          appId, serviceTemplates.stream().map(ServiceTemplate::getServiceId).collect(toList()), SERVICE);
    } else if (ENVIRONMENT.equals(entityType)) {
      serviceVariables.addAll(getServiceVariables(
          appId, serviceTemplates.stream().map(ServiceTemplate::getEnvId).collect(toList()), ENVIRONMENT));
    }
    serviceVariables.addAll(getServiceVariables(
        appId, serviceTemplates.stream().map(ServiceTemplate::getUuid).collect(toList()), SERVICE_TEMPLATE));
    return serviceVariables;
  }

  /**
   * All expression suggestions without context specific
   * @return
   */
  public static Set<String> getExpressionSuggestions(StateType stateType) {
    Set<String> allContextExpressions = getStaticExpressions();
    if (stateType != null) {
      allContextExpressions.addAll(getStateTypeExpressions(stateType));
    }
    return allContextExpressions;
  }
}
