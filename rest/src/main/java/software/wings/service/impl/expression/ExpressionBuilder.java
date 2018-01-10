package software.wings.service.impl.expression;

import static java.util.Arrays.asList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.common.Constants.ASSERTION_STATEMENT;
import static software.wings.common.Constants.ASSERTION_STATUS;
import static software.wings.common.Constants.HTTP_RESPONSE_BODY;
import static software.wings.common.Constants.HTTP_RESPONSE_CODE;
import static software.wings.common.Constants.HTTP_RESPONSE_METHOD;
import static software.wings.common.Constants.HTTP_URL;
import static software.wings.common.Constants.JSONPATH;
import static software.wings.common.Constants.WINGS_BACKUP_PATH;
import static software.wings.common.Constants.WINGS_RUNTIME_PATH;
import static software.wings.common.Constants.WINGS_STAGING_PATH;
import static software.wings.common.Constants.XPATH;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.Switch.noop;
import static software.wings.utils.Switch.unhandled;

import com.google.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
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
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 8/7/17.
 */
public abstract class ExpressionBuilder {
  protected static final String APP_NAME = "app.name";
  protected static final String APP_DESCRIPTION = "app.description";

  protected static final String ARTIFACT_NAME = "artifact.displayName";
  protected static final String ARTIFACT_DESCRIPTION = "artifact.description";
  protected static final String ARTIFACT_BUILDNO = "artifact.buildNo";
  protected static final String ARTIFACT_REVISION = "artifact.revision";
  protected static final String ARTIFACT_FILE_NAME = "ARTIFACT_FILE_NAME";
  protected static final String ARTIFACT_BUCKET_NAME = "artifact.bucketName";
  protected static final String ARTIFACT_BUCKET_KEY = "artifact.key";
  protected static final String ARTIFACT_BUCKET_URL = "artifact.url";

  protected static final String ENV_NAME = "env.name";
  protected static final String ENV_DESCRIPTION = "env.description";

  protected static final String SERVICE_NAME = "service.name";
  protected static final String SERVICE_DESCRIPTION = "service.description";

  protected static final String WORKFLOW_NAME = "workflow.name";
  protected static final String WORKFLOW_DESCRIPTION = "workflow.description";

  protected static final String INSTANCE_NAME = "instance.name";
  protected static final String INSTANCE_HOSTNAME = "instance.hostName";
  protected static final String INSTANCE_HOST_PUBLICDNS = "instance.host.publicDns";

  protected static final String HOST_NAME = "host.name";
  protected static final String HOST_HOSTNAME = "host.hostName";
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

  protected static final String START_TS = "startTs";
  protected static final String END_TS = "endTs";
  protected static final String STATUS = "status";
  protected static final String ERROR_MSG = "errorMsg";

  protected static final String APPROVEDBY_NAME = "approvedBy.name";
  protected static final String APPROVEDBY_EMAIL = "approvedBy.email";

  protected static final String EMAIl_TO_ADDRESS = "toAddress";
  protected static final String EMAIL_CC_ADDRESS = "ccAddress";
  protected static final String EMAIL_SUBJECT = "subject";
  protected static final String EMAIL_BODY = "body";

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

  Set<String> getStaticExpressions() {
    Set<String> expressions = new TreeSet<>();
    expressions.addAll(asList(APP_NAME, APP_DESCRIPTION));
    expressions.addAll(
        asList(ARTIFACT_NAME, ARTIFACT_BUILDNO, ARTIFACT_REVISION, ARTIFACT_DESCRIPTION, ARTIFACT_FILE_NAME));
    expressions.addAll(asList(ENV_NAME, ENV_DESCRIPTION));
    expressions.addAll(asList(SERVICE_NAME, SERVICE_DESCRIPTION));
    expressions.addAll(asList(WORKFLOW_NAME, WORKFLOW_DESCRIPTION));

    expressions.addAll(asList(INSTANCE_NAME, INSTANCE_HOSTNAME, INSTANCE_HOST_PUBLICDNS));

    expressions.addAll(asList(HOST_NAME, HOST_HOSTNAME, HOST_PUBLICDNS, HOST_INSTANCE_ID, HOST_INSTANCE_TYPE,
        HOST_INSTANCE_IMAGEID, HOST_INSTANCE_ARCH, HOST_INSTANCE_KERNELID, HOST_INSTANCE_KEY_NAME, HOST_INSTANCE_PVTDNS,
        HOST_INSTANCE_PRIVATEIP, HOST_INSTANCE_PUBLICDNS, HOST_INSTANCE_PUBLICIP, HOST_INSTANCE_SUBNETID,
        HOST_INSTANCE_VPCID));

    return expressions;
  }

  protected Set<String> getStateTypeExpressions(StateType stateType) {
    Set<String> expressions = new TreeSet<>(asList(START_TS, END_TS, STATUS, ERROR_MSG));
    switch (stateType) {
      case SHELL_SCRIPT:
        expressions.addAll(asList(WINGS_RUNTIME_PATH, WINGS_STAGING_PATH, WINGS_BACKUP_PATH));
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
        expressions.addAll(asList(ARTIFACT_BUCKET_NAME, ARTIFACT_BUCKET_KEY, ARTIFACT_BUCKET_URL));
        break;
      case AWS_LAMBDA_STATE:
      case ECS_SERVICE_SETUP:
      case JENKINS:
      case KUBERNETES_REPLICATION_CONTROLLER_SETUP:
      case NEW_RELIC_DEPLOYMENT_MARKER:
        noop();
        break;
      default:
        unhandled(stateType);
    }

    return expressions;
  }

  protected Set<String> getServiceVariables(String appId, List<String> entityIds) {
    return getServiceVariables(appId, entityIds, null);
  }

  protected Set<String> getServiceVariables(String appId, List<String> entityIds, EntityType entityType) {
    if (CollectionUtils.isEmpty(entityIds)) {
      return new TreeSet<>();
    }
    PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                  .withLimit(PageRequest.UNLIMITED)
                                                                  .addFilter(ServiceVariable.APP_ID_KEY, EQ, appId)
                                                                  .addFilter("entityId", IN, entityIds.toArray())
                                                                  .build();
    if (entityType != null) {
      serviceVariablePageRequest.addFilter("entityType", entityType, EQ);
    }
    List<ServiceVariable> serviceVariables = serviceVariablesService.list(serviceVariablePageRequest, true);

    return serviceVariables.stream()
        .map(serviceVariable -> "serviceVariable." + serviceVariable.getName())
        .collect(Collectors.toSet());
  }

  protected Set<String> getServiceVariablesOfTemplates(
      String appId, PageRequest<ServiceTemplate> pageRequest, EntityType entityType) {
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.list(pageRequest, false, false);
    if (SERVICE.equals(entityType)) {
      return getServiceVariables(
          appId, serviceTemplates.stream().map(ServiceTemplate::getServiceId).collect(Collectors.toList()), SERVICE);
    } else if (ENVIRONMENT.equals(entityType)) {
      return getServiceVariables(
          appId, serviceTemplates.stream().map(ServiceTemplate::getEnvId).collect(Collectors.toList()), ENVIRONMENT);
    }
    return new TreeSet<>();
  }
}
