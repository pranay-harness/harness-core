package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.APP_ID_KEY;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.ECS_CONTAINER_TASK_YAML_FILE_NAME;
import static software.wings.beans.yaml.YamlConstants.ECS_SERVICE_SPEC_YAML_FILE_NAME;
import static software.wings.beans.yaml.YamlConstants.INDEX_YAML;
import static software.wings.beans.yaml.YamlConstants.KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME;
import static software.wings.beans.yaml.YamlConstants.LAMBDA_SPEC_YAML_FILE_NAME;
import static software.wings.beans.yaml.YamlConstants.PCF_MANIFEST_YAML_FILE_NAME;
import static software.wings.beans.yaml.YamlConstants.USER_DATA_SPEC_YAML_FILE_NAME;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.context.GlobalContextData;
import io.harness.exception.InvalidRequestException;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.globalcontex.EntityOperationIdentifier;
import io.harness.globalcontex.EntityOperationIdentifier.entityOperation;
import io.harness.globalcontex.PurgeGlobalContextData;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.UuidAccess;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.EntityAuditRecord.EntityAuditRecordBuilder;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.ConfigFile;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Event.Type;
import software.wings.beans.GcpConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JiraConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.Role;
import software.wings.beans.Service;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SftpConfig;
import software.wings.beans.SlackConfig;
import software.wings.beans.SmbConfig;
import software.wings.beans.SplunkConfig;
import software.wings.beans.StringValue;
import software.wings.beans.SumoConfig;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.Workflow;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.security.UserGroup;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.verification.CVConfiguration;

import java.util.List;

@Slf4j
@Singleton
public class EntityHelper {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlHelper yamlHelper;
  private static final String DEPLOYMENT_SPECIFICATION_YAML_PATH_FORMAT =
      "Setup/Application/%s/Services/%s/Deloyment Specifications/%s.yaml";
  private static final String COMMANDS_YAML_PATH_FORMAT = "Setup/Application/%s/Services/%s/Commands/%s.yaml";

  // Some Setting Attributes defined that do not have Yaml
  static final String SSH_KEYS_ENTITY_TYPE = "Host Connection Attributes";
  static final String WIN_RM_CONNECTION_ENTITY_TYPE = "Win Rm Connection Attributes";

  public <T extends UuidAccess> void loadMetaDataForEntity(T entity, EntityAuditRecordBuilder builder, Type type) {
    String entityId = entity.getUuid();
    String entityType = EMPTY;
    String entityName = EMPTY;
    String appId = EMPTY;
    String appName = EMPTY;
    String affectedResourceId = EMPTY;
    String affectedResourceName = EMPTY;
    String affectedResourceType = EMPTY;

    String affectedResourceOperation = Type.UPDATE.name();

    if (entity instanceof Environment) {
      Environment environment = (Environment) entity;
      entityType = EntityType.ENVIRONMENT.name();
      entityName = environment.getName();
      appId = environment.getAppId();
      affectedResourceId = environment.getUuid();
      affectedResourceName = environment.getName();
      affectedResourceType = EntityType.ENVIRONMENT.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof UserGroup) {
      UserGroup userGroup = (UserGroup) entity;
      entityType = EntityType.USER_GROUP.name();
      entityName = userGroup.getName();
    } else if (entity instanceof Pipeline) {
      Pipeline pipeline = (Pipeline) entity;
      entityType = EntityType.PIPELINE.name();
      entityName = pipeline.getName();
      appId = pipeline.getAppId();
      affectedResourceId = pipeline.getUuid();
      affectedResourceName = pipeline.getName();
      affectedResourceType = EntityType.PIPELINE.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof Application) {
      Application application = (Application) entity;
      entityType = EntityType.APPLICATION.name();
      entityName = application.getName();
      appId = application.getAppId();
      affectedResourceId = application.getUuid();
      affectedResourceName = application.getName();
      affectedResourceType = EntityType.APPLICATION.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof InfrastructureMapping) {
      InfrastructureMapping mapping = (InfrastructureMapping) entity;
      entityType = EntityType.INFRASTRUCTURE_MAPPING.name();
      entityName = mapping.getName();
      appId = mapping.getAppId();
      affectedResourceId = mapping.getEnvId();
      affectedResourceName = getEnvironmentName(mapping.getEnvId(), appId);
      affectedResourceType = EntityType.ENVIRONMENT.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.ENVIRONMENT, affectedResourceId, affectedResourceName);
    } else if (entity instanceof Workflow) {
      Workflow workflow = (Workflow) entity;
      entityType = EntityType.WORKFLOW.name();
      entityName = workflow.getName();
      appId = workflow.getAppId();
      affectedResourceId = workflow.getUuid();
      affectedResourceName = workflow.getName();
      affectedResourceType = EntityType.WORKFLOW.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof InfrastructureProvisioner) {
      InfrastructureProvisioner provisioner = (InfrastructureProvisioner) entity;
      entityType = EntityType.PROVISIONER.name();
      entityName = provisioner.getName();
      appId = provisioner.getAppId();
      affectedResourceId = provisioner.getUuid();
      affectedResourceName = provisioner.getName();
      affectedResourceType = EntityType.PROVISIONER.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof Trigger) {
      Trigger trigger = (Trigger) entity;
      entityType = EntityType.TRIGGER.name();
      entityName = trigger.getName();
      appId = trigger.getAppId();
      affectedResourceId = trigger.getUuid();
      affectedResourceName = trigger.getName();
      affectedResourceType = EntityType.TRIGGER.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof ArtifactStream) {
      ArtifactStream artifactStream = (ArtifactStream) entity;
      entityType = EntityType.ARTIFACT_STREAM.name();
      entityName = artifactStream.getName();
      appId = artifactStream.getAppId();
      affectedResourceId = artifactStream.getServiceId();
      affectedResourceName = getServiceName(artifactStream.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof Service) {
      Service service = (Service) entity;
      entityType = EntityType.SERVICE.name();
      entityName = service.getName();
      appId = service.getAppId();
      affectedResourceId = service.getUuid();
      affectedResourceName = service.getName();
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof HelmChartSpecification) {
      HelmChartSpecification chartSpecification = (HelmChartSpecification) entity;
      entityType = EntityType.HELM_CHART_SPECIFICATION.name();
      entityName = chartSpecification.getChartName();
      appId = chartSpecification.getAppId();
      affectedResourceId = chartSpecification.getServiceId();
      affectedResourceName = getServiceName(chartSpecification.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof PcfServiceSpecification) {
      PcfServiceSpecification serviceSpecification = (PcfServiceSpecification) entity;
      entityType = EntityType.PCF_SERVICE_SPECIFICATION.name();
      entityName = PCF_MANIFEST_YAML_FILE_NAME;
      appId = serviceSpecification.getAppId();
      affectedResourceId = serviceSpecification.getServiceId();
      affectedResourceName = getServiceName(serviceSpecification.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof LambdaSpecification) {
      LambdaSpecification lambdaSpecification = (LambdaSpecification) entity;
      entityType = EntityType.LAMBDA_SPECIFICATION.name();
      entityName = LAMBDA_SPEC_YAML_FILE_NAME;
      appId = lambdaSpecification.getAppId();
      affectedResourceId = lambdaSpecification.getServiceId();
      affectedResourceName = getServiceName(lambdaSpecification.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof UserDataSpecification) {
      UserDataSpecification dataSpecification = (UserDataSpecification) entity;
      entityType = EntityType.USER_DATA_SPECIFICATION.name();
      entityName = USER_DATA_SPEC_YAML_FILE_NAME;
      appId = dataSpecification.getAppId();
      affectedResourceId = dataSpecification.getServiceId();
      affectedResourceName = getServiceName(dataSpecification.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof EcsContainerTask) {
      EcsContainerTask task = (EcsContainerTask) entity;
      entityType = EntityType.ECS_CONTAINER_SPECIFICATION.name();
      entityName = ECS_CONTAINER_TASK_YAML_FILE_NAME;
      appId = task.getAppId();
      affectedResourceId = task.getServiceId();
      affectedResourceName = getServiceName(task.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof EcsServiceSpecification) {
      EcsServiceSpecification ecsServiceSpecification = (EcsServiceSpecification) entity;
      entityType = EntityType.ECS_SERVICE_SPECIFICATION.name();
      entityName = ECS_SERVICE_SPEC_YAML_FILE_NAME;
      appId = ecsServiceSpecification.getAppId();
      affectedResourceId = ecsServiceSpecification.getServiceId();
      affectedResourceName = getServiceName(ecsServiceSpecification.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof KubernetesContainerTask) {
      KubernetesContainerTask task = (KubernetesContainerTask) entity;
      entityType = EntityType.K8S_CONTAINER_SPECIFICATION.name();
      entityName = KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME;
      appId = task.getAppId();
      affectedResourceId = task.getServiceId();
      affectedResourceName = getServiceName(task.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof ConfigFile) {
      ConfigFile configFile = (ConfigFile) entity;
      entityType = EntityType.CONFIG_FILE.name();
      entityName = configFile.getRelativeFilePath();
      appId = configFile.getAppId();
      String envId = configFile.getEnvId();
      if (Environment.GLOBAL_ENV_ID.equals(envId)) {
        EntityType entityTypeForFile = configFile.getEntityType();
        affectedResourceId = configFile.getEntityId();
        // Config file defined in service
        if (EntityType.SERVICE.equals(entityTypeForFile)) {
          affectedResourceName = getServiceName(affectedResourceId, appId);
          affectedResourceType = EntityType.SERVICE.name();
          affectedResourceOperation =
              getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
        } else if (EntityType.ENVIRONMENT.equals(entityTypeForFile)) {
          // Config file override in ENV for all services
          affectedResourceName = getEnvironmentName(affectedResourceId, appId);
          affectedResourceType = EntityType.ENVIRONMENT.name();
          affectedResourceOperation =
              getAffectedResourceOperation(EntityType.ENVIRONMENT, affectedResourceId, affectedResourceName);
        }
      } else {
        // Config file override in ENV for specific service.
        affectedResourceId = envId;
        affectedResourceName = getEnvironmentName(envId, appId);
        affectedResourceType = EntityType.ENVIRONMENT.name();
        affectedResourceOperation =
            getAffectedResourceOperation(EntityType.ENVIRONMENT, affectedResourceId, affectedResourceName);
      }
    } else if (entity instanceof SettingAttribute) {
      SettingAttribute settingAttribute = (SettingAttribute) entity;
      SettingValue value = settingAttribute.getValue();
      entityType = getEntityTypeForSettingValue(value);
      entityName = settingAttribute.getName();
      if (value != null && SettingVariableTypes.STRING.name().equals(value.getType())) {
        // For String value, appId != __GLOBAL_APP_ID__
        // We need this appId for Yaml handling
        appId = settingAttribute.getAppId();
      }
      affectedResourceId = settingAttribute.getUuid();
      affectedResourceName = settingAttribute.getName();
      affectedResourceType = settingAttribute.getCategory() != null ? settingAttribute.getCategory().name() : EMPTY;
      affectedResourceOperation = type.name();
    } else if (entity instanceof ServiceCommand) {
      ServiceCommand serviceCommand = (ServiceCommand) entity;
      entityType = EntityType.SERVICE_COMMAND.name();
      entityName = serviceCommand.getName();
      appId = serviceCommand.getAppId();
      affectedResourceId = serviceCommand.getServiceId();
      affectedResourceName = getServiceName(serviceCommand.getServiceId(), appId);
      affectedResourceType = EntityType.SERVICE.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
    } else if (entity instanceof ManifestFile) {
      ManifestFile manifestFile = (ManifestFile) entity;
      entityType = EntityType.MANIFEST_FILE.name();
      entityName = manifestFile.getFileName();
      appId = manifestFile.getAppId();
      ApplicationManifest manifest =
          wingsPersistence.get(ApplicationManifest.class, manifestFile.getApplicationManifestId());
      if (manifest != null) {
        String envId = manifest.getEnvId();
        if (isNotEmpty(envId)) {
          affectedResourceId = envId;
          affectedResourceName = getEnvironmentName(envId, appId);
          affectedResourceType = EntityType.ENVIRONMENT.name();
          affectedResourceOperation =
              getAffectedResourceOperation(EntityType.ENVIRONMENT, affectedResourceId, affectedResourceName);
        } else {
          String serviceId = manifest.getServiceId();
          if (isNotEmpty(serviceId)) {
            affectedResourceId = serviceId;
            affectedResourceName = getServiceName(serviceId, appId);
            affectedResourceType = EntityType.SERVICE.name();
            affectedResourceOperation =
                getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
          }
        }
      }
    } else if (entity instanceof ApplicationManifest) {
      ApplicationManifest applicationManifest = (ApplicationManifest) entity;
      entityType = EntityType.APPLICATION_MANIFEST.name();
      entityName = format("Application Manifest: [%s]", applicationManifest.getKind().name());
      appId = applicationManifest.getAppId();
      String envId = applicationManifest.getEnvId();
      if (isNotEmpty(envId)) {
        affectedResourceId = envId;
        affectedResourceName = getEnvironmentName(envId, appId);
        affectedResourceType = EntityType.ENVIRONMENT.name();
        affectedResourceOperation =
            getAffectedResourceOperation(EntityType.ENVIRONMENT, affectedResourceId, affectedResourceName);
      } else {
        String serviceId = applicationManifest.getServiceId();
        if (isNotEmpty(serviceId)) {
          affectedResourceId = serviceId;
          affectedResourceName = getServiceName(serviceId, appId);
          affectedResourceType = EntityType.SERVICE.name();
          affectedResourceOperation =
              getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
        }
      }
    } else if (entity instanceof ServiceVariable) {
      ServiceVariable variable = (ServiceVariable) entity;
      entityType = EntityType.SERVICE_VARIABLE.name();
      entityName = variable.getName();
      appId = variable.getAppId();
      String envId = variable.getEnvId();
      if (Environment.GLOBAL_ENV_ID.equals(envId)) {
        EntityType entityTypeForVariable = variable.getEntityType();
        if (EntityType.SERVICE.equals(entityTypeForVariable)) {
          affectedResourceId = variable.getEntityId();
          affectedResourceName = getServiceName(affectedResourceId, appId);
          affectedResourceOperation =
              getAffectedResourceOperation(EntityType.SERVICE, affectedResourceId, affectedResourceName);
          affectedResourceType = EntityType.SERVICE.name();
        } else if (EntityType.ENVIRONMENT.equals(entityTypeForVariable)) {
          affectedResourceId = variable.getEntityId();
          affectedResourceName = getEnvironmentName(affectedResourceId, appId);
          affectedResourceType = EntityType.ENVIRONMENT.name();
          affectedResourceOperation =
              getAffectedResourceOperation(EntityType.ENVIRONMENT, affectedResourceId, affectedResourceName);
        }
      } else {
        affectedResourceId = envId;
        affectedResourceName = getEnvironmentName(envId, appId);
        affectedResourceType = EntityType.ENVIRONMENT.name();
        affectedResourceOperation =
            getAffectedResourceOperation(EntityType.ENVIRONMENT, affectedResourceId, affectedResourceName);
      }
    } else if (entity instanceof Role) {
      Role role = (Role) entity;
      entityType = EntityType.ROLE.name();
      entityName = role.getName();
      appId = role.getAppId();
      affectedResourceId = role.getUuid();
      affectedResourceName = role.getName();
      affectedResourceType = EntityType.ROLE.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof Template) {
      Template template = (Template) entity;
      entityType = EntityType.TEMPLATE.name();
      entityName = template.getName();
      appId = template.getAppId();
      affectedResourceId = template.getUuid();
      affectedResourceName = template.getName();
      affectedResourceType = EntityType.TEMPLATE.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof TemplateFolder) {
      TemplateFolder templateFolder = (TemplateFolder) entity;
      entityType = EntityType.TEMPLATE_FOLDER.name();
      entityName = templateFolder.getName();
      appId = templateFolder.getAppId();
      affectedResourceId = templateFolder.getUuid();
      affectedResourceName = templateFolder.getName();
      affectedResourceType = EntityType.TEMPLATE_FOLDER.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof EncryptedData) {
      EncryptedData encryptedData = (EncryptedData) entity;
      entityType = encryptedData.getType().name();
      entityName = encryptedData.getName();
      affectedResourceId = encryptedData.getUuid();
      affectedResourceName = encryptedData.getName();
      affectedResourceType = EntityType.ENCRYPTED_RECORDS.name();
      affectedResourceOperation = type.name();
    } else if (entity instanceof CVConfiguration) {
      CVConfiguration cvConfiguration = (CVConfiguration) entity;
      entityType = EntityType.CV_CONFIGURATION.name();
      entityName = cvConfiguration.getName();
      appId = cvConfiguration.getAppId();
      affectedResourceId = cvConfiguration.getEnvId();
      affectedResourceName = getEnvironmentName(cvConfiguration.getEnvId(), appId);
      affectedResourceType = EntityType.ENVIRONMENT.name();
      affectedResourceOperation =
          getAffectedResourceOperation(EntityType.ENVIRONMENT, affectedResourceId, affectedResourceName);
    } else {
      logger.error(format("Unhandled class for auditing: [%s]", entity.getClass().getSimpleName()));
      entityType = format("Object of class: [%s]", entity.getClass().getSimpleName());
      entityName = format("Name of class: [%s]", entity.getClass().getSimpleName());
    }

    if (isNotEmpty(appId)) {
      appName = getApplicationName(appId);
    }

    builder.entityId(entityId)
        .entityName(entityName)
        .entityType(entityType)
        .appId(appId)
        .appName(appName)
        .operationType(type.name())
        .affectedResourceId(affectedResourceId)
        .affectedResourceName(affectedResourceName)
        .affectedResourceType(affectedResourceType)
        .affectedResourceOperation(affectedResourceOperation);
  }

  private String getAffectedResourceOperation(
      EntityType entityType, String affectedResourceId, String affectedResourceName) {
    if (isPurgeActivity()) {
      return Type.DELETE.name();
    }

    AuditGlobalContextData auditGlobalContextData =
        (AuditGlobalContextData) GlobalContextManager.get(AuditGlobalContextData.AUDIT_ID);
    if (auditGlobalContextData == null || auditGlobalContextData.getEntityOperationIdentifierSet() == null) {
      return Type.UPDATE.name();
    }

    EntityOperationIdentifier operationIdentifier = EntityOperationIdentifier.builder()
                                                        .entityId(affectedResourceId)
                                                        .entityName(affectedResourceName)
                                                        .entityType(entityType.name())
                                                        .operation(entityOperation.CREATE)
                                                        .build();
    if (auditGlobalContextData.getEntityOperationIdentifierSet().contains(operationIdentifier)) {
      return Type.CREATE.name();
    }

    return Type.UPDATE.name();
  }

  private boolean isPurgeActivity() {
    boolean purgeActivity = false;

    GlobalContextData globalContextData = GlobalContextManager.get(PurgeGlobalContextData.PURGE_OP);
    if (globalContextData != null) {
      purgeActivity = true;
    }

    return purgeActivity;
  }

  private String getApplicationName(String appId) {
    List<Application> applications = wingsPersistence.createQuery(Application.class)
                                         .filter(APP_ID_KEY, appId)
                                         .project(ApplicationKeys.name, true)
                                         .asList();
    if (isEmpty(applications)) {
      return "";
    }
    return applications.get(0).getName();
  }

  private String getServiceName(String serviceId, String appId) {
    List<Service> services = wingsPersistence.createQuery(Service.class)
                                 .filter(ID_KEY, serviceId)
                                 .filter(APP_ID_KEY, appId)
                                 .project(Service.NAME_KEY, true)
                                 .asList();
    if (isEmpty(services)) {
      return "";
    }
    return services.get(0).getName();
  }

  private String getEnvironmentName(String envId, String appId) {
    List<Environment> environments = wingsPersistence.createQuery(Environment.class)
                                         .filter(ID_KEY, envId)
                                         .filter(APP_ID_KEY, appId)
                                         .project(EnvironmentKeys.name, true)
                                         .asList();
    if (isEmpty(environments)) {
      return "";
    }
    return environments.get(0).getName();
  }

  private String getEntityTypeForSettingValue(SettingValue settingValue) {
    // This will be case when going through yaml path.
    if (settingValue == null) {
      return "Setting Attribute";
    }

    if (settingValue instanceof APMVerificationConfig) {
      return "APM Verification Config";
    } else if (settingValue instanceof AmazonS3HelmRepoConfig) {
      return "Amazon S3 Helm Repo Config";
    } else if (settingValue instanceof AppDynamicsConfig) {
      return "AppDynamics Config";
    } else if (settingValue instanceof ArtifactoryConfig) {
      return "Artifactory Config";
    } else if (settingValue instanceof AwsConfig) {
      return "Aws Config";
    } else if (settingValue instanceof AzureConfig) {
      return "Azure Config";
    } else if (settingValue instanceof BambooConfig) {
      return "Bamboo Config";
    } else if (settingValue instanceof BastionConnectionAttributes) {
      return "Bastion Connection Config";
    } else if (settingValue instanceof BugsnagConfig) {
      return "Bugsnag Config";
    } else if (settingValue instanceof DatadogConfig) {
      return "Datadog Config";
    } else if (settingValue instanceof DockerConfig) {
      return "Docker Config";
    } else if (settingValue instanceof DynaTraceConfig) {
      return "DynaTrace Config";
    } else if (settingValue instanceof ElasticLoadBalancerConfig) {
      return "Elastic Load Balancer Config";
    } else if (settingValue instanceof ElkConfig) {
      return "Elk Config";
    } else if (settingValue instanceof GcpConfig) {
      return "Gcp Config";
    } else if (settingValue instanceof HostConnectionAttributes) {
      return SSH_KEYS_ENTITY_TYPE;
    } else if (settingValue instanceof HttpHelmRepoConfig) {
      return "Http Helm Repo Config";
    } else if (settingValue instanceof JenkinsConfig) {
      return "Jenkins Config";
    } else if (settingValue instanceof JiraConfig) {
      return "Jira Config";
    } else if (settingValue instanceof KubernetesClusterConfig) {
      return "Kubernetes Cluster Config";
    } else if (settingValue instanceof KubernetesConfig) {
      return "Kubernetes Config";
    } else if (settingValue instanceof LogzConfig) {
      return "Logz Config";
    } else if (settingValue instanceof NewRelicConfig) {
      return "New Relic Config";
    } else if (settingValue instanceof NexusConfig) {
      return "Nexus Config";
    } else if (settingValue instanceof PcfConfig) {
      return "Pcf Config";
    } else if (settingValue instanceof PhysicalDataCenterConfig) {
      return "Physical Data Center Config";
    } else if (settingValue instanceof PrometheusConfig) {
      return "Prometheus Config";
    } else if (settingValue instanceof ServiceNowConfig) {
      return "Service Now Config";
    } else if (settingValue instanceof SftpConfig) {
      return "Sftp Config";
    } else if (settingValue instanceof SlackConfig) {
      return "Slack Config";
    } else if (settingValue instanceof SmbConfig) {
      return "Smb Config";
    } else if (settingValue instanceof SmtpConfig) {
      return "Smtp Config";
    } else if (settingValue instanceof SplunkConfig) {
      return "Splunk Config";
    } else if (settingValue instanceof SumoConfig) {
      return "Sumo Config";
    } else if (settingValue instanceof WinRmConnectionAttributes) {
      return WIN_RM_CONNECTION_ENTITY_TYPE;
    } else if (settingValue instanceof StringValue) {
      return "String Value";
    } else {
      return settingValue.getClass().getSimpleName();
    }
  }

  @VisibleForTesting
  String getYamlPathForDeploymentSpecification(Object entity, EntityAuditRecord record) {
    if (entity instanceof HelmChartSpecification) {
      // In the case of HelmChartSpecification, the actual entity is
      // ApplicationManifest. Here we return empty
      return EMPTY;
    } else if (entity instanceof PcfServiceSpecification) {
      return format(DEPLOYMENT_SPECIFICATION_YAML_PATH_FORMAT, record.getAppName(), record.getAffectedResourceName(),
          PCF_MANIFEST_YAML_FILE_NAME);
    } else if (entity instanceof LambdaSpecification) {
      return format(DEPLOYMENT_SPECIFICATION_YAML_PATH_FORMAT, record.getAppName(), record.getAffectedResourceName(),
          LAMBDA_SPEC_YAML_FILE_NAME);
    } else if (entity instanceof UserDataSpecification) {
      return format(DEPLOYMENT_SPECIFICATION_YAML_PATH_FORMAT, record.getAppName(), record.getAffectedResourceName(),
          USER_DATA_SPEC_YAML_FILE_NAME);
    } else if (entity instanceof EcsServiceSpecification) {
      return format(DEPLOYMENT_SPECIFICATION_YAML_PATH_FORMAT, record.getAppName(), record.getAffectedResourceName(),
          ECS_SERVICE_SPEC_YAML_FILE_NAME);
    } else if (entity instanceof EcsContainerTask) {
      return format(DEPLOYMENT_SPECIFICATION_YAML_PATH_FORMAT, record.getAppName(), record.getAffectedResourceName(),
          ECS_CONTAINER_TASK_YAML_FILE_NAME);
    } else if (entity instanceof KubernetesContainerTask) {
      return format(DEPLOYMENT_SPECIFICATION_YAML_PATH_FORMAT, record.getAppName(), record.getAffectedResourceName(),
          KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME);
    } else if (entity instanceof ServiceCommand) {
      return format(COMMANDS_YAML_PATH_FORMAT, record.getAppName(), record.getAffectedResourceName(),
          ((ServiceCommand) entity).getName());
    } else {
      String errorMessage =
          format("Unrecognized class name while getting Yaml path for class: [%s]", entity.getClass().getSimpleName());
      logger.error(errorMessage, new Exception());
      return EMPTY;
    }
  }

  public String getFullYamlPathForEntity(Object entity, EntityAuditRecord record) {
    try {
      if (entity == null) {
        return EMPTY;
      }

      // Deployment specs
      if (entity instanceof HelmChartSpecification || entity instanceof PcfServiceSpecification
          || entity instanceof LambdaSpecification || entity instanceof UserDataSpecification
          || entity instanceof EcsContainerTask || entity instanceof EcsServiceSpecification
          || entity instanceof KubernetesContainerTask || entity instanceof ServiceCommand) {
        return getYamlPathForDeploymentSpecification(entity, record);
      }

      String yamlPrefix = yamlHelper.getYamlPathForEntity(entity);
      if (isEmpty(yamlPrefix)) {
        return EMPTY;
      }
      String finalYaml = EMPTY;
      if (entity instanceof Environment) {
        Environment environment = (Environment) entity;
        finalYaml = format("%s/%s", yamlPrefix, INDEX_YAML);
      } else if (entity instanceof Pipeline) {
        Pipeline pipeline = (Pipeline) entity;
        finalYaml = format("%s/%s%s", yamlPrefix, pipeline.getName(), YAML_EXTENSION);
      } else if (entity instanceof Application) {
        Application application = (Application) entity;
        finalYaml = format("%s/%s", yamlPrefix, INDEX_YAML);
      } else if (entity instanceof InfrastructureMapping) {
        InfrastructureMapping mapping = (InfrastructureMapping) entity;
        finalYaml = format("%s/%s%s", yamlPrefix, mapping.getName(), YAML_EXTENSION);
      } else if (entity instanceof Workflow) {
        Workflow workflow = (Workflow) entity;
        finalYaml = format("%s/%s%s", yamlPrefix, workflow.getName(), YAML_EXTENSION);
      } else if (entity instanceof InfrastructureProvisioner) {
        InfrastructureProvisioner provisioner = (InfrastructureProvisioner) entity;
        finalYaml = format("%s/%s%s", yamlPrefix, provisioner.getName(), YAML_EXTENSION);
      } else if (entity instanceof ArtifactStream) {
        ArtifactStream artifactStream = (ArtifactStream) entity;
        finalYaml = format("%s/%s%s", yamlPrefix, artifactStream.getName(), YAML_EXTENSION);
      } else if (entity instanceof Service) {
        Service service = (Service) entity;
        finalYaml = format("%s/%s", yamlPrefix, INDEX_YAML);
      } else if (entity instanceof ConfigFile) {
        ConfigFile configFile = (ConfigFile) entity;
        finalYaml = format("%s/%s%s", yamlPrefix, configFile.getFileName(), YAML_EXTENSION);
      } else if (entity instanceof SettingAttribute) {
        SettingAttribute settingAttribute = (SettingAttribute) entity;
        SettingValue settingValue = settingAttribute.getValue();
        if (SettingVariableTypes.STRING.name().equals(settingValue.getType())) {
          finalYaml = format("Setup/Applications/%s/%s", record.getAppName(), DEFAULTS_YAML);
        } else {
          finalYaml = format("%s/%s%s", yamlPrefix, settingAttribute.getName(), YAML_EXTENSION);
        }
      } else if (entity instanceof ApplicationManifest) {
        ApplicationManifest applicationManifest = (ApplicationManifest) entity;
        finalYaml = format("%s/%s", yamlPrefix, INDEX_YAML);
      } else if (entity instanceof ManifestFile) {
        ManifestFile manifestFile = (ManifestFile) entity;
        finalYaml = format("%s/%s", yamlPrefix, manifestFile.getFileName());
      } else if (entity instanceof CVConfiguration) {
        CVConfiguration cvConfiguration = (CVConfiguration) entity;
        finalYaml = format("%s/%s%s", yamlPrefix, cvConfiguration.getName(), YAML_EXTENSION);
      } else {
        finalYaml = yamlPrefix;
      }
      return finalYaml;
    } catch (InvalidRequestException ex) {
      logger.error(format("Exception while getting Yaml path for entity id: [%s] of class: [%s]",
          ((UuidAccess) entity).getUuid(), entity.getClass().getSimpleName()));
      return EMPTY;
    }
  }
}