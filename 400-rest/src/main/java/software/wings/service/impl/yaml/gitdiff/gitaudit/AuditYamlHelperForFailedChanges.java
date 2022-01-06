/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.gitdiff.gitaudit;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ff.FeatureFlagService;
import io.harness.git.model.ChangeType;

import software.wings.audit.EntityAuditRecord;
import software.wings.audit.EntityAuditRecord.EntityAuditRecordBuilder;
import software.wings.beans.Application;
import software.wings.beans.CGConstants;
import software.wings.beans.ConfigFile;
import software.wings.beans.DeploymentSpecification;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.EntityHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.verification.CVConfiguration;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._940_CG_AUDIT_SERVICE)
public class AuditYamlHelperForFailedChanges {
  @Inject private YamlHelper yamlHelper;
  @Inject private EntityHelper entityHelper;

  @Value
  @Builder
  static class GitAuditDataWrapper {
    private String accountId;
    private String appId;
    private String appName;
    private String yamlFilePath;
    private String fileContent;
    private String YamlType;
    private EntityAuditRecordBuilder builder;
    private Type type;
  }

  public static class ProvisionerWithOnlyAuditNeededData extends InfrastructureProvisioner {
    @Override
    public String variableKey() {
      return null;
    }
  }

  public static class ArtifactStreamWithOnlyAuditNeededData extends ArtifactStream {
    public ArtifactStreamWithOnlyAuditNeededData(String artifactStreamType) {
      super(artifactStreamType);
    }

    @Override
    public String generateSourceName() {
      return null;
    }
    @Override
    public ArtifactStreamAttributes fetchArtifactStreamAttributes(FeatureFlagService featureFlagService) {
      return null;
    }
    @Override
    public String fetchArtifactDisplayName(String buildNo) {
      return null;
    }

    @Override
    public ArtifactStream cloneInternal() {
      return null;
    }
  }

  public static class InfraMappingWithOnlyAuditNeededData extends InfrastructureMapping {
    @Override
    public void applyProvisionerVariables(
        Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
      // Unsupported
    }
    @Override
    public String getDefaultName() {
      return null;
    }
    @Override
    public String getHostConnectionAttrs() {
      return null;
    }
  }

  public EntityAuditRecord generateEntityAuditRecordForFailedChanges(GitFileChange gitFileChange, String accountId) {
    EntityAuditRecordBuilder builder = EntityAuditRecord.builder();

    String yamlFilePath = EmptyPredicate.isEmpty(gitFileChange.getFilePath()) ? gitFileChange.getOldFilePath()
                                                                              : gitFileChange.getFilePath();

    // 1. Get application details from YamlPath. If its account level entity, use _GLOBAL_APP_ID
    Application application = getApplicationDetails(accountId, yamlFilePath);
    builder.operationType(gitFileChange.getChangeType().name());

    // 2. Generate GitAuditDataWrapper with all details for this change
    GitAuditDataWrapper requestData = GitAuditDataWrapper.builder()
                                          .appId(application.getUuid())
                                          .appName(application.getName())
                                          .accountId(accountId)
                                          .yamlFilePath(yamlFilePath)
                                          .fileContent(gitFileChange.getFileContent())
                                          .builder(builder)
                                          .type(getTypeFromGitOperation(gitFileChange.getChangeType()))
                                          .build();

    // 3. Generate Actual EntityAuditRecord that will be added AuditHeader
    EntityAuditRecord record = generateEntityAuditRecordForFailedChange(requestData);
    if (record != null) {
      record.setYamlPath(yamlFilePath);
      return record;
    }

    return null;
  }

  private EntityAuditRecord generateEntityAuditRecordForFailedChange(GitAuditDataWrapper auditRequestData) {
    // Change is for app level entity
    if (!CGConstants.GLOBAL_APP_ID.equals(auditRequestData.getAppId())) {
      return generateAuditRecordIfAppLevelEntityChange(auditRequestData);
    }

    // Seems change is for Account level entity
    return generateAuditRecordIfAccountLevelEntityChange(auditRequestData);
  }

  // Service / ENV / Workflow / Pipeline / Provisioner / Nested Entity under Service or Env
  private EntityAuditRecord generateAuditRecordIfAppLevelEntityChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    if (matchWithRegex(YamlType.APPLICATION.getPathExpression(), auditRequestData.getYamlFilePath())
        || matchWithRegex(YamlType.APPLICATION_DEFAULTS.getPathExpression(), auditRequestData.getYamlFilePath())) {
      return handleApplicationChange(auditRequestData).build();
    }

    // We are using getPathExpression (and not getPrefixExpression), means this is Env/index.yaml file
    if (matchWithRegex(YamlType.ENVIRONMENT.getPathExpression(), auditRequestData.getYamlFilePath())) {
      return handleEnvironmentChange(auditRequestData).build();
    }

    // We are using getPathExpression (and not getPrefixExpression), means this is Service/index.yaml file
    if (matchWithRegex(YamlType.SERVICE.getPathExpression(), yamlFilePath)) {
      return handleServiceChange(auditRequestData).build();
    }

    if (matchWithRegex(YamlType.WORKFLOW.getPathExpression(), yamlFilePath)) {
      return handleWorkflowChange(auditRequestData).build();
    }

    if (matchWithRegex(YamlType.TRIGGER.getPathExpression(), yamlFilePath)) {
      return handleTriggerChange(auditRequestData).build();
    }

    if (matchWithRegex(YamlType.PIPELINE.getPathExpression(), yamlFilePath)) {
      return handlePipelineChange(auditRequestData).build();
    }

    if (matchWithRegex(YamlType.PROVISIONER.getPathExpression(), yamlFilePath)) {
      return handleProvisionerChange(auditRequestData).build();
    }

    if (matchWithRegex(YamlType.CV_CONFIGURATION.getPathExpression(), yamlFilePath)) {
      return handleCvConfigChange(auditRequestData).build();
    }

    // As we reached here, This seems to be some nested Service or Environment level entity
    EntityAuditRecordBuilder entityAuditRecordBuilder = handleServiceOrEnvNestedEntities(auditRequestData);
    return entityAuditRecordBuilder != null ? entityAuditRecordBuilder.build() : null;
  }

  private EntityAuditRecordBuilder handleServiceOrEnvNestedEntities(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    if (matchWithRegex(YamlType.INFRA_MAPPING.getPathExpression(), yamlFilePath)) {
      return handleInfraMappingChange(auditRequestData);
    }

    if (matchWithRegex(YamlType.DEPLOYMENT_SPECIFICATION.getPathExpression(), yamlFilePath)) {
      return handleDeploymentSpecificationChange(auditRequestData);
    }

    if (matchWithRegex(YamlType.ARTIFACT_STREAM.getPathExpression(), yamlFilePath)) {
      return handleArtifactStreamChange(auditRequestData);
    }

    if (matchWithRegex(YamlType.COMMAND.getPathExpression(), yamlFilePath)) {
      return handleServiceCommandChange(auditRequestData);
    }

    if (matchWithRegex(YamlType.CONFIG_FILE.getPathExpression(), yamlFilePath)) {
      return handleServiceConfigFileChange(auditRequestData);
    }

    if (matchWithRegex(YamlType.CONFIG_FILE_OVERRIDE.getPathExpression(), yamlFilePath)) {
      return handleEnvironmentConfigFileChange(auditRequestData);
    }

    return null;
  }

  private EntityAuditRecord generateAuditRecordIfAccountLevelEntityChange(GitAuditDataWrapper auditRequestData) {
    YamlType type = getSettingAttributeType(auditRequestData.getYamlFilePath());
    if (type != null) {
      return handleSettingAttributeEntityUpdate(auditRequestData, type).build();
    }

    // Add if any different types of account entities exist
    return null;
  }

  private EntityAuditRecordBuilder handleApplicationChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    Application application = null;
    try {
      application = yamlHelper.getApp(auditRequestData.getAccountId(), yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(yamlFilePath, auditRequestData.getAccountId(), EntityType.APPLICATION.name()));
    }

    if (application == null) {
      String name = yamlHelper.extractEntityNameFromYamlPath(
          YamlType.APPLICATION.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
      application = anApplication()
                        .appId(auditRequestData.getAppId())
                        .accountId(auditRequestData.getAccountId())
                        .name(name)
                        .build();
    }

    entityHelper.loadMetaDataForEntity(application, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleEnvironmentChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    Environment environment = null;
    try {
      environment = yamlHelper.getEnvironment(auditRequestData.getAppId(), yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(yamlFilePath, auditRequestData.getAccountId(), EntityType.ENVIRONMENT.name()));
    }

    if (environment == null) {
      String name =
          yamlHelper.extractParentEntityName(YamlType.ENVIRONMENT.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
      environment = anEnvironment()
                        .appId(auditRequestData.getAppId())
                        .accountId(auditRequestData.getAccountId())
                        .name(name)
                        .build();
    }

    entityHelper.loadMetaDataForEntity(environment, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleServiceChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    Service service = null;
    try {
      service = yamlHelper.getService(auditRequestData.getAppId(), yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(yamlFilePath, auditRequestData.getAccountId(), EntityType.SERVICE.name()));
    }

    if (service == null) {
      String name =
          yamlHelper.extractParentEntityName(YamlType.SERVICE.getPrefixExpression(), yamlFilePath, PATH_DELIMITER);
      service = Service.builder()
                    .appId(auditRequestData.getAppId())
                    .accountId(auditRequestData.getAccountId())
                    .name(name)
                    .build();
    }

    entityHelper.loadMetaDataForEntity(service, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleWorkflowChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    Workflow workflow = null;
    try {
      workflow = yamlHelper.getWorkflow(auditRequestData.getAccountId(), yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(yamlFilePath, auditRequestData.getAccountId(), EntityType.WORKFLOW.name()));
    }

    if (workflow == null) {
      String name =
          yamlHelper.extractEntityNameFromYamlPath(YamlType.WORKFLOW.getPathExpression(), yamlFilePath, PATH_DELIMITER);

      workflow =
          aWorkflow().appId(auditRequestData.getAppId()).accountId(auditRequestData.getAccountId()).name(name).build();
    }

    entityHelper.loadMetaDataForEntity(workflow, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleTriggerChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    Trigger trigger = null;
    try {
      trigger = yamlHelper.getTrigger(auditRequestData.getAppId(), yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(yamlFilePath, auditRequestData.getAccountId(), EntityType.TRIGGER.name()));
    }

    if (trigger == null) {
      String name =
          yamlHelper.extractEntityNameFromYamlPath(YamlType.TRIGGER.getPathExpression(), yamlFilePath, PATH_DELIMITER);

      trigger = Trigger.builder().appId(auditRequestData.getAppId()).name(name).build();
    }

    entityHelper.loadMetaDataForEntity(trigger, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handlePipelineChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    Pipeline pipeline = null;
    try {
      pipeline = yamlHelper.getPipeline(auditRequestData.getAccountId(), yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(yamlFilePath, auditRequestData.getAccountId(), EntityType.PIPELINE.name()));
    }

    if (pipeline == null) {
      String name =
          yamlHelper.extractEntityNameFromYamlPath(YamlType.PIPELINE.getPathExpression(), yamlFilePath, PATH_DELIMITER);
      pipeline = Pipeline.builder()
                     .appId(auditRequestData.getAppId())
                     .accountId(auditRequestData.getAccountId())
                     .name(name)
                     .build();
    }

    entityHelper.loadMetaDataForEntity(pipeline, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleProvisionerChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();
    InfrastructureProvisioner infrastructureProvisioner = null;
    try {
      infrastructureProvisioner =
          yamlHelper.getInfrastructureProvisioner(auditRequestData.getAccountId(), yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(yamlFilePath, auditRequestData.getAccountId(), EntityType.PROVISIONER.name()));
    }

    if (infrastructureProvisioner == null) {
      String name = yamlHelper.extractEntityNameFromYamlPath(
          YamlType.PROVISIONER.getPathExpression(), yamlFilePath, PATH_DELIMITER);

      // We just need InfrastructureProvisioner object to retrieve appId and Name.
      // So this
      infrastructureProvisioner = new ProvisionerWithOnlyAuditNeededData();

      infrastructureProvisioner.setAppId(auditRequestData.getAppId());
      infrastructureProvisioner.setName(name);
    }

    entityHelper.loadMetaDataForEntity(
        infrastructureProvisioner, auditRequestData.getBuilder(), auditRequestData.getType());

    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleCvConfigChange(GitAuditDataWrapper auditRequestData) {
    String yamlFilePath = auditRequestData.getYamlFilePath();

    CVConfiguration cvConfiguration = null;
    try {
      cvConfiguration = yamlHelper.getCVConfiguration(auditRequestData.getAccountId(), yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(
          yamlFilePath, auditRequestData.getAccountId(), EntityType.VERIFICATION_CONFIGURATION.name()));
    }

    if (cvConfiguration == null) {
      String name = yamlHelper.extractEntityNameFromYamlPath(
          YamlType.CV_CONFIGURATION.getPathExpression(), yamlFilePath, PATH_DELIMITER);

      cvConfiguration = new CVConfiguration();
      cvConfiguration.setAccountId(auditRequestData.getAccountId());
      cvConfiguration.setAppId(auditRequestData.getAppId());
      cvConfiguration.setName(name);
    }

    entityHelper.loadMetaDataForEntity(cvConfiguration, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleDeploymentSpecificationChange(GitAuditDataWrapper auditRequestData) {
    String serviceId = getServiceId(auditRequestData);
    String subType = getYamlSubType(auditRequestData.getFileContent(), auditRequestData.getYamlFilePath());

    if (isNotEmpty(subType) && isNotEmpty(serviceId)) {
      DeploymentSpecification deploymentSpecification;
      try {
        deploymentSpecification =
            yamlHelper.getDeploymentSpecification(auditRequestData.getAppId(), serviceId, subType);
      } catch (Exception e) {
        deploymentSpecification = null;
      }

      if (deploymentSpecification == null) {
        deploymentSpecification = generateDeploymentSpecification(auditRequestData, subType, serviceId);
      }

      if (deploymentSpecification != null) {
        entityHelper.loadMetaDataForEntity(
            deploymentSpecification, auditRequestData.getBuilder(), auditRequestData.getType());
      }
    }

    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleArtifactStreamChange(GitAuditDataWrapper auditRequestData) {
    ArtifactStream stream;
    try {
      stream = yamlHelper.getArtifactStream(auditRequestData.getAccountId(), auditRequestData.getYamlFilePath());
    } catch (Exception e) {
      stream = null;
    }

    if (stream == null) {
      String name = yamlHelper.extractEntityNameFromYamlPath(
          YamlType.ARTIFACT_STREAM.getPathExpression(), auditRequestData.getYamlFilePath(), PATH_DELIMITER);
      String serviceId = getServiceId(auditRequestData);

      // TODO: ASR: update when index added on setting_id + name
      // Here we don't care about actual details of ArtifactStream.
      // We just need Object of type ArtifactStream with appId, serviceId and Name set.
      stream = new ArtifactStreamWithOnlyAuditNeededData(StringUtils.EMPTY);
      stream.setName(name);
      stream.setAppId(auditRequestData.getAppId());
      stream.setServiceId(serviceId);
    }

    entityHelper.loadMetaDataForEntity(stream, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleServiceCommandChange(GitAuditDataWrapper auditRequestData) {
    ServiceCommand serviceCommand;
    try {
      serviceCommand =
          yamlHelper.getServiceCommand(auditRequestData.getAccountId(), auditRequestData.getYamlFilePath());
    } catch (Exception e) {
      serviceCommand = null;
    }

    if (serviceCommand == null) {
      String name = yamlHelper.extractEntityNameFromYamlPath(
          YamlType.COMMAND.getPathExpression(), auditRequestData.getYamlFilePath(), PATH_DELIMITER);
      String serviceId = getServiceId(auditRequestData);

      serviceCommand =
          aServiceCommand().withAppId(auditRequestData.getAppId()).withServiceId(serviceId).withName(name).build();
    }

    entityHelper.loadMetaDataForEntity(serviceCommand, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleServiceConfigFileChange(GitAuditDataWrapper auditRequestData) {
    ConfigFile configFile;
    String targetFilePath =
        getTargetFilePathFromYaml(auditRequestData.getFileContent(), auditRequestData.getYamlFilePath());
    try {
      configFile = yamlHelper.getServiceConfigFile(
          auditRequestData.getAccountId(), auditRequestData.getYamlFilePath(), targetFilePath);
    } catch (Exception e) {
      configFile = null;
    }

    if (configFile == null) {
      String name = yamlHelper.extractEntityNameFromYamlPath(
          YamlType.CONFIG_FILE.getPathExpression(), auditRequestData.getYamlFilePath(), PATH_DELIMITER);
      String serviceId = getServiceId(auditRequestData);

      configFile = ConfigFile.builder().envId(serviceId).entityType(EntityType.SERVICE).envId(GLOBAL_ENV_ID).build();
      configFile.setRelativeFilePath(targetFilePath);
      configFile.setAppId(auditRequestData.getAppId());
      configFile.setName(name);
    }

    entityHelper.loadMetaDataForEntity(configFile, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleEnvironmentConfigFileChange(GitAuditDataWrapper auditRequestData) {
    ConfigFile configFile;
    String targetFilePath =
        getTargetFilePathFromYaml(auditRequestData.getFileContent(), auditRequestData.getYamlFilePath());
    try {
      configFile = yamlHelper.getEnvironmentConfigFile(
          auditRequestData.getAccountId(), auditRequestData.getYamlFilePath(), targetFilePath);
    } catch (Exception e) {
      configFile = null;
    }

    if (configFile == null) {
      String name = yamlHelper.extractEntityNameFromYamlPath(
          YamlType.CONFIG_FILE_OVERRIDE.getPathExpression(), auditRequestData.getYamlFilePath(), PATH_DELIMITER);
      String envId = getEnvId(auditRequestData);

      String serviceNameOverriden = getServiceNameFromConfigFileYamlContext(
          auditRequestData.getFileContent(), auditRequestData.getYamlFilePath());

      // When env config file has setting override for all services, ENV_ID = GLOBAL_ENV_ID
      // else, when overriding for a particular service, its envId.
      // entitId is always envId in both of the above cases.
      configFile = ConfigFile.builder()
                       .entityId(envId)
                       .entityType(isBlank(serviceNameOverriden) ? EntityType.ENVIRONMENT : EntityType.SERVICE_TEMPLATE)
                       .envId(isBlank(serviceNameOverriden) ? GLOBAL_ENV_ID : envId)
                       .relativeFilePath(targetFilePath)
                       .build();
      configFile.setAppId(auditRequestData.getAppId());
      configFile.setName(name);
    }

    entityHelper.loadMetaDataForEntity(configFile, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleInfraMappingChange(GitAuditDataWrapper auditRequestData) {
    InfrastructureMapping mapping;
    try {
      mapping = yamlHelper.getInfraMapping(auditRequestData.getAccountId(), auditRequestData.getYamlFilePath());
    } catch (Exception e) {
      log.warn(getWarningMessage(
          auditRequestData.getYamlFilePath(), auditRequestData.getAccountId(), YamlType.INFRA_MAPPING.name()));
      mapping = null;
    }

    if (mapping == null) {
      String envId = getEnvId(auditRequestData);
      String name = yamlHelper.getNameFromYamlFilePath(auditRequestData.getYamlFilePath());
      // Here we don't care about actual details of InfraMapping.
      // We just need Object of type InfraMapping with appId, envId and Name set.
      mapping = new InfraMappingWithOnlyAuditNeededData();
      mapping.setAccountId(auditRequestData.getAccountId());
      mapping.setAppId(auditRequestData.getAppId());
      mapping.setEnvId(envId);
      mapping.setName(name);
    }

    entityHelper.loadMetaDataForEntity(mapping, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private EntityAuditRecordBuilder handleSettingAttributeEntityUpdate(
      GitAuditDataWrapper auditRequestData, YamlType type) {
    String yamlFilePath = auditRequestData.getYamlFilePath();
    SettingAttribute settingAttribute;
    try {
      settingAttribute = getSettingAttribute(auditRequestData.getAccountId(), type, yamlFilePath);
    } catch (Exception e) {
      log.warn(getWarningMessage(yamlFilePath, auditRequestData.getAccountId(), type.name()));
      settingAttribute = null;
    }

    if (settingAttribute == null) {
      String name = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
      settingAttribute = aSettingAttribute()
                             .withAppId(auditRequestData.getAppId())
                             .withName(name)
                             .withCategory(getCategory(type))
                             .build();
    }

    entityHelper.loadMetaDataForEntity(settingAttribute, auditRequestData.getBuilder(), auditRequestData.getType());
    return auditRequestData.getBuilder();
  }

  private SettingCategory getCategory(YamlType type) {
    if (YamlType.CLOUD_PROVIDER == type) {
      return SettingCategory.CLOUD_PROVIDER;
    }

    if (YamlType.VERIFICATION_PROVIDER == type || YamlType.ARTIFACT_SERVER == type
        || YamlType.COLLABORATION_PROVIDER == type || YamlType.LOADBALANCER_PROVIDER == type) {
      return SettingCategory.CONNECTOR;
    }

    return null;
  }

  private Application getApplicationDetails(String accountId, String yamlFilePath) {
    String appName = StringUtils.EMPTY;
    String appId = CGConstants.GLOBAL_APP_ID;

    if (matchWithRegex(YamlType.APPLICATION.getPrefixExpression(), yamlFilePath)) {
      try {
        appName = yamlHelper.getAppName(yamlFilePath);
        appId = yamlHelper.getAppId(accountId, yamlFilePath);
      } catch (Exception e) {
        appId = null;
      }
    }

    return anApplication().accountId(accountId).appId(appId).uuid(appId).name(appName).build();
  }

  private YamlType getSettingAttributeType(String yamlFilePath) {
    if (matchWithRegex(YamlType.VERIFICATION_PROVIDER.getPathExpression(), yamlFilePath)) {
      return YamlType.VERIFICATION_PROVIDER;
    }

    if (matchWithRegex(YamlType.CLOUD_PROVIDER.getPathExpression(), yamlFilePath)) {
      return YamlType.CLOUD_PROVIDER;
    }

    if (matchWithRegex(YamlType.LOADBALANCER_PROVIDER.getPathExpression(), yamlFilePath)) {
      return YamlType.LOADBALANCER_PROVIDER;
    }

    if (matchWithRegex(YamlType.COLLABORATION_PROVIDER.getPathExpression(), yamlFilePath)) {
      return YamlType.COLLABORATION_PROVIDER;
    }

    if (matchWithRegex(YamlType.ARTIFACT_SERVER.getPathExpression(), yamlFilePath)) {
      return YamlType.ARTIFACT_SERVER;
    }

    return null;
  }

  private SettingAttribute getSettingAttribute(String accountId, YamlType type, String yamlFilePath) {
    if (YamlType.COLLABORATION_PROVIDER == type) {
      return yamlHelper.getCollaborationProvider(accountId, yamlFilePath);
    }

    if (YamlType.CLOUD_PROVIDER == type) {
      return yamlHelper.getCloudProvider(accountId, yamlFilePath);
    }

    if (YamlType.VERIFICATION_PROVIDER == type) {
      return yamlHelper.getVerificationProvider(accountId, yamlFilePath);
    }

    if (YamlType.LOADBALANCER_PROVIDER == type) {
      return yamlHelper.getLoadBalancerProvider(accountId, yamlFilePath);
    }

    if (YamlType.ARTIFACT_SERVER == type) {
      return yamlHelper.getArtifactServer(accountId, yamlFilePath);
    }

    return null;
  }

  private DeploymentSpecification generateDeploymentSpecification(
      GitAuditDataWrapper auditRequestData, String subType, String serviceId) {
    if ("PCF".equals(subType)) {
      PcfServiceSpecification pcfServiceSpecification = PcfServiceSpecification.builder().serviceId(serviceId).build();
      pcfServiceSpecification.setAppId(auditRequestData.getAppId());
      return pcfServiceSpecification;
    }

    if ("HELM".equals(subType)) {
      HelmChartSpecification helmChartSpecification = HelmChartSpecification.builder().serviceId(serviceId).build();
      helmChartSpecification.setAppId(auditRequestData.getAppId());
      return helmChartSpecification;
    }

    if ("AMI".equals(subType)) {
      UserDataSpecification userDataSpecification = UserDataSpecification.builder().serviceId(serviceId).build();
      userDataSpecification.setAppId(auditRequestData.getAppId());
      return userDataSpecification;
    }

    if ("AWS_LAMBDA".equals(subType)) {
      LambdaSpecification lambdaSpecification = LambdaSpecification.builder().serviceId(serviceId).build();
      lambdaSpecification.setAppId(auditRequestData.getAppId());
      return lambdaSpecification;
    }

    if ("ECS_SERVICE_SPEC".equals(subType)) {
      EcsServiceSpecification specification = EcsServiceSpecification.builder().serviceId(serviceId).build();
      specification.setAppId(auditRequestData.getAppId());
      return specification;
    }

    if ("ECS".equals(subType)) {
      EcsContainerTask ecsContainerTask = new EcsContainerTask();
      ecsContainerTask.setAppId(auditRequestData.getAppId());
      ecsContainerTask.setServiceId(serviceId);
      return ecsContainerTask;
    }

    if ("KUBERNETES".equals(subType)) {
      KubernetesContainerTask containerTask = new KubernetesContainerTask();
      containerTask.setAppId(auditRequestData.getAppId());
      containerTask.setServiceId(serviceId);
      return containerTask;
    }

    return null;
  }

  private String getWarningMessage(String yamlFilePath, String accountId, String type) {
    return new StringBuilder(128)
        .append("Failed to retrieve ")
        .append(type)
        .append(" for YamlPath: ")
        .append(yamlFilePath)
        .append(" during Auditing (Git Path). EntityMay not exist")
        .toString();
  }

  private String getYamlSubType(String fileContent, String yamlPath) {
    try {
      YamlReader reader = new YamlReader(fileContent);
      Object object = reader.read();
      Map map = (Map) object;
      return (String) map.get("type");
    } catch (Exception e) {
      log.warn(new StringBuilder(128)
                   .append("Failed to get YamlSubtype for Path: ")
                   .append(yamlPath)
                   .append(" during git change audit...")
                   .toString());
      return null;
    }
  }

  private String getTargetFilePathFromYaml(String fileContent, String yamlPath) {
    try {
      YamlReader reader = new YamlReader(fileContent);
      Object object = reader.read();
      Map map = (Map) object;
      return (String) map.get("targetFilePath");
    } catch (Exception e) {
      log.warn(new StringBuilder(128)
                   .append("Failed to get targetFilePath for Path: ")
                   .append(yamlPath)
                   .append(" during git change audit...")
                   .toString());
      return null;
    }
  }

  private String getServiceNameFromConfigFileYamlContext(String fileContent, String yamlPath) {
    try {
      YamlReader reader = new YamlReader(fileContent);
      Object object = reader.read();
      Map map = (Map) object;
      return (String) map.get("serviceName");
    } catch (Exception e) {
      log.warn(new StringBuilder(128)
                   .append("Failed to get serviceName for Path: ")
                   .append(yamlPath)
                   .append(" during git change audit...")
                   .toString());
      return null;
    }
  }

  private boolean matchWithRegex(String prefixRegex, String yamlFilePath) {
    Pattern pattern = Pattern.compile(prefixRegex);
    Matcher matcher = pattern.matcher(yamlFilePath);

    // Lets use this example, i want to extract service name from the path
    // regex - Setup/Applications/*/Services/*/
    // yamlFilePath - Setup/Applications/App1/Services/service1/Commands/command1
    if (matcher.find()) {
      return true;
    }
    return false;
  }

  private Type getTypeFromGitOperation(ChangeType changeType) {
    switch (changeType) {
      case ADD: {
        return Type.CREATE;
      }

      case MODIFY: {
        return Type.UPDATE;
      }

      case DELETE: {
        return Type.DELETE;
      }

      default: {
        return Type.UPDATE;
      }
    }
  }

  private String getEnvId(GitAuditDataWrapper auditRequestData) {
    String envId;
    try {
      envId = yamlHelper.getEnvironmentId(auditRequestData.getAppId(), auditRequestData.getYamlFilePath());
    } catch (Exception e) {
      envId = StringUtils.EMPTY;
    }
    return envId;
  }

  private String getServiceId(GitAuditDataWrapper auditRequestData) {
    String serviceId;
    try {
      serviceId = yamlHelper.getServiceId(auditRequestData.getAppId(), auditRequestData.getYamlFilePath());
    } catch (Exception e) {
      serviceId = StringUtils.EMPTY;
    }
    return serviceId;
  }
}
