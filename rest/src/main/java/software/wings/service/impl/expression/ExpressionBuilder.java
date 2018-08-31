package software.wings.service.impl.expression;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.noop;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SERVICE_TEMPLATE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.common.Constants.ARTIFACT_SOURCE_REGISTRY_URL_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_REPOSITORY_NAME_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_USER_NAME_KEY;
import static software.wings.common.Constants.ASSERTION_STATEMENT;
import static software.wings.common.Constants.ASSERTION_STATUS;
import static software.wings.common.Constants.DEPLOYMENT_TRIGGERED_BY;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;
import static software.wings.common.Constants.HTTP_RESPONSE_BODY;
import static software.wings.common.Constants.HTTP_RESPONSE_CODE;
import static software.wings.common.Constants.HTTP_RESPONSE_METHOD;
import static software.wings.common.Constants.HTTP_URL;
import static software.wings.common.Constants.INFRA_ROUTE;
import static software.wings.common.Constants.INFRA_TEMP_ROUTE;
import static software.wings.common.Constants.JSONPATH;
import static software.wings.common.Constants.PCF_APP_NAME;
import static software.wings.common.Constants.PCF_OLD_APP_NAME;
import static software.wings.common.Constants.WINGS_BACKUP_PATH;
import static software.wings.common.Constants.WINGS_RUNTIME_PATH;
import static software.wings.common.Constants.WINGS_STAGING_PATH;
import static software.wings.common.Constants.XPATH;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ContextElement.DEPLOYMENT_URL;

import com.google.inject.Inject;

import software.wings.beans.EntityType;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
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
  protected static final String ARTIFACT_BUCKET_NAME = "artifact.bucketName";
  protected static final String ARTIFACT_BUCKET_KEY = "artifact.key";
  protected static final String ARTIFACT_URL = "artifact.url";
  protected static final String ARTIFACT_BUILD_FULL_DISPLAYNAME = "artifact.buildFullDisplayName";
  protected static final String ARTIFACT_PATH = "artifact.artifactPath";
  protected static final String ARTIFACT_SOURCE_USER_NAME = "artifact.source." + ARTIFACT_SOURCE_USER_NAME_KEY;
  protected static final String ARTIFACT_SOURCE_REGISTRY_URL = "artifact.source." + ARTIFACT_SOURCE_REGISTRY_URL_KEY;
  protected static final String ARTIFACT_SOURCE_REPOSITORY_NAME =
      "artifact.source." + ARTIFACT_SOURCE_REPOSITORY_NAME_KEY;

  protected static final String ENV_NAME = "env.name";
  protected static final String ENV_DESCRIPTION = "env.description";

  protected static final String SERVICE_NAME = "service.name";
  protected static final String SERVICE_DESCRIPTION = "service.description";

  protected static final String WORKFLOW_NAME = "workflow.name";
  protected static final String WORKFLOW_DESCRIPTION = "workflow.description";
  protected static final String WORKFLOW_DISPLAY_NAME = "workflow.displayName";
  protected static final String WORKFLOW_RELEASE_NO = "workflow.releaseNo";
  protected static final String WORKFLOW_LAST_GOOD_RELEASE_NO = "workflow.lastGoodReleaseNo";
  protected static final String WORKFLOW_LAST_GOOD_DEPLOYMENT_DISPLAY_NAME = "workflow.lastGoodDeploymentDisplayName";
  protected static final String WORKFLOW_PIPELINE_DEPLOYMENT_UUID = "workflow.pipelineDeploymentUuid";

  protected static final String INSTANCE_NAME = "instance.name";
  protected static final String INSTANCE_HOSTNAME = "instance.hostName";
  protected static final String INSTANCE_HOST_PUBLICDNS = "instance.host.publicDns";

  protected static final String HOST_NAME = "host.name";
  protected static final String HOST_HOSTNAME = "host.hostName";
  protected static final String HOST_IP = "host.ip";
  protected static final String HOST_PUBLICDNS = "host.publicDns";
  protected static final String HOST_INSTANCE_ID = "host.ec2Instance.instanceId";
  protected static final String HOST_INSTANCE_TYPE = "host.ec2Instance.instanceType";
  protected static final String HOST_INSTANCE_IMAGEID = "host.ec2Instance.imageId";
  protected static final String HOST_INSTANCE_ARCH = "host.ec2Instance.architecture";
  protected static final String HOST_INSTANCE_KERNELID = "host.ec2Instance.kernelId";
  protected static final String HOST_INSTANCE_KEY_NAME = "host.ec2Instance.keyName";
  protected static final String HOST_INSTANCE_PVTDNS = "host.ec2Instance.privateDnsName";
  protected static final String HOST_INSTANCE_PRIVATEIP = "host.ec2Instance.privateIpAddress";
  protected static final String HOST_INSTANCE_PUBLICDNS = "host.ec2Instance.publicDnsName";
  protected static final String HOST_INSTANCE_PUBLICIP = "host.ec2Instance.publicIpAddress";
  protected static final String HOST_INSTANCE_SUBNETID = "host.ec2Instance.subnetId";
  protected static final String HOST_INSTANCE_VPCID = "host.ec2Instance.vpcId";

  protected static final String INFRA_KUBERNETES_NAMESPACE = "infra.kubernetes.namespace";

  protected static final String APPROVEDBY_NAME = "approvedBy.name";
  protected static final String APPROVEDBY_EMAIL = "approvedBy.email";

  protected static final String EMAIl_TO_ADDRESS = "toAddress";
  protected static final String EMAIL_CC_ADDRESS = "ccAddress";
  protected static final String EMAIL_SUBJECT = "subject";
  protected static final String EMAIL_BODY = "body";

  public static final String WORKFLOW_VARIABLE_PREFIX = "workflow.variables";
  public static final String SERVICE_VARIABLE_PREFIX = "service.variables";

  @Inject private ServiceVariableService serviceVariablesService;
  @Inject private ServiceTemplateService serviceTemplateService;

  public Set<String> getExpressions(String appId, String entityId, String serviceId) {
    return getExpressions(appId, entityId);
  }

  public Set<String> getExpressions(String appId, String entityId, String serviceId, StateType stateType) {
    return getExpressions(appId, entityId);
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
        ARTIFACT_FILE_NAME, ARTIFACT_BUILD_FULL_DISPLAYNAME, ARTIFACT_BUCKET_NAME, ARTIFACT_BUCKET_KEY, ARTIFACT_PATH,
        ARTIFACT_URL, ARTIFACT_SOURCE_USER_NAME, ARTIFACT_SOURCE_REGISTRY_URL, ARTIFACT_SOURCE_REPOSITORY_NAME));
    expressions.addAll(asList(ENV_NAME, ENV_DESCRIPTION));
    expressions.addAll(asList(SERVICE_NAME, SERVICE_DESCRIPTION));
    expressions.addAll(asList(WORKFLOW_NAME, WORKFLOW_DESCRIPTION, WORKFLOW_DISPLAY_NAME, WORKFLOW_RELEASE_NO,
        WORKFLOW_LAST_GOOD_DEPLOYMENT_DISPLAY_NAME, WORKFLOW_LAST_GOOD_RELEASE_NO, WORKFLOW_PIPELINE_DEPLOYMENT_UUID));

    expressions.addAll(asList(INSTANCE_NAME, INSTANCE_HOSTNAME, INSTANCE_HOST_PUBLICDNS));

    expressions.addAll(asList(HOST_NAME, HOST_HOSTNAME, HOST_IP, HOST_PUBLICDNS, HOST_INSTANCE_ID, HOST_INSTANCE_TYPE,
        HOST_INSTANCE_IMAGEID, HOST_INSTANCE_ARCH, HOST_INSTANCE_KERNELID, HOST_INSTANCE_KEY_NAME, HOST_INSTANCE_PVTDNS,
        HOST_INSTANCE_PRIVATEIP, HOST_INSTANCE_PUBLICDNS, HOST_INSTANCE_PUBLICIP, HOST_INSTANCE_SUBNETID,
        HOST_INSTANCE_VPCID));

    expressions.add(INFRA_KUBERNETES_NAMESPACE);

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
        expressions.addAll(asList(INFRA_ROUTE, INFRA_TEMP_ROUTE, PCF_APP_NAME, PCF_OLD_APP_NAME));
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
    List<ServiceVariable> serviceVariables = serviceVariablesService.list(serviceVariablePageRequest, true);

    return serviceVariables.stream()
        .map(serviceVariable -> "serviceVariable." + serviceVariable.getName())
        .collect(Collectors.toSet());
  }

  protected Set<String> getServiceVariablesOfTemplates(
      String appId, PageRequest<ServiceTemplate> pageRequest, EntityType entityType) {
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.list(pageRequest, false, false);
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
