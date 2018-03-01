package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SU_APP_USER;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.StringValue.Builder.aStringValue;
import static software.wings.common.Constants.BACKUP_PATH;
import static software.wings.common.Constants.DEFAULT_BACKUP_PATH;
import static software.wings.common.Constants.DEFAULT_RUNTIME_PATH;
import static software.wings.common.Constants.DEFAULT_STAGING_PATH;
import static software.wings.common.Constants.RUNTIME_PATH;
import static software.wings.common.Constants.STAGING_PATH;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;

import java.util.List;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/17/16.
 */
@ValidateOnExecution
@Singleton
public class SettingsServiceImpl implements SettingsService {
  private static final Logger logger = LoggerFactory.getLogger(SettingsServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingValidationService settingValidationService;
  @Inject private AppService appService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private YamlChangeSetHelper yamlChangeSetHelper;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<SettingAttribute> list(PageRequest<SettingAttribute> req) {
    try {
      return wingsPersistence.query(SettingAttribute.class, req);
    } catch (Exception e) {
      logger.error("Error getting setting attributes. " + e.getMessage(), e);
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#save(software.wings.beans.SettingAttribute)
   */
  @Override
  public SettingAttribute save(SettingAttribute settingAttribute) {
    return save(settingAttribute, true);
  }

  @Override
  public SettingAttribute save(SettingAttribute settingAttribute, boolean pushToGit) {
    settingValidationService.validate(settingAttribute);
    if (settingAttribute.getValue() != null) {
      if (settingAttribute.getValue() instanceof Encryptable) {
        ((Encryptable) settingAttribute.getValue()).setAccountId(settingAttribute.getAccountId());
      }
    }

    SettingAttribute newSettingAttribute =
        Validator.duplicateCheck(()
                                     -> wingsPersistence.saveAndGet(SettingAttribute.class, settingAttribute),
            "name", settingAttribute.getName());
    if (shouldBeSynced(newSettingAttribute, pushToGit)) {
      yamlChangeSetHelper.queueSettingYamlChangeAsync(newSettingAttribute, ChangeType.ADD);
    }

    return newSettingAttribute;
  }

  private boolean shouldBeSynced(SettingAttribute settingAttribute, boolean pushToGit) {
    String type = settingAttribute.getValue().getType();

    boolean skip = SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES.name().equals(type)
        || SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES.name().equals(type);

    return pushToGit && !skip;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#get(java.lang.String, java.lang.String)
   */

  @Override
  public SettingAttribute get(String appId, String varId) {
    return get(appId, GLOBAL_ENV_ID, varId);
  }

  @Override
  public SettingAttribute get(String appId, String envId, String varId) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field(ID_KEY)
        .equal(varId)
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#get(java.lang.String)
   */

  @Override
  public SettingAttribute get(String varId) {
    return wingsPersistence.get(SettingAttribute.class, varId);
  }

  @Override
  public SettingAttribute getSettingAttributeByName(String accountId, String settingAttributeName) {
    return wingsPersistence.createQuery(SettingAttribute.class).field("name").equal(settingAttributeName).get();
  }

  @Override
  public SettingAttribute update(SettingAttribute settingAttribute, boolean pushToGit) {
    SettingAttribute existingSetting = get(settingAttribute.getAppId(), settingAttribute.getUuid());
    Validator.notNullCheck("Setting", existingSetting);
    settingAttribute.setAccountId(existingSetting.getAccountId());
    settingAttribute.setAppId(existingSetting.getAppId());

    settingValidationService.validate(settingAttribute);

    SettingAttribute savedSettingAttributes = get(settingAttribute.getUuid());

    ImmutableMap.Builder<String, Object> fields =
        ImmutableMap.<String, Object>builder().put("name", settingAttribute.getName());
    if (settingAttribute.getValue() != null) {
      if (settingAttribute.getValue() instanceof Encryptable) {
        ((Encryptable) settingAttribute.getValue()).setAccountId(settingAttribute.getAccountId());
      }
      fields.put("value", settingAttribute.getValue());
    }
    wingsPersistence.updateFields(SettingAttribute.class, settingAttribute.getUuid(), fields.build());

    SettingAttribute updatedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());

    if (shouldBeSynced(updatedSettingAttribute, pushToGit)) {
      yamlChangeSetHelper.queueSettingUpdateYamlChangeAsync(savedSettingAttributes, updatedSettingAttribute);
    }
    return updatedSettingAttribute;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#update(software.wings.beans.SettingAttribute)
   */

  @Override
  public SettingAttribute update(SettingAttribute settingAttribute) {
    return update(settingAttribute, true);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#delete(java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String appId, String varId) {
    this.delete(appId, varId, true);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#delete(java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String appId, String varId, boolean pushToGit) {
    SettingAttribute settingAttribute = get(varId);
    Validator.notNullCheck("Setting Value", settingAttribute);
    ensureSettingAttributeSafeToDelete(settingAttribute);
    boolean deleted = wingsPersistence.delete(settingAttribute);
    if (deleted && shouldBeSynced(settingAttribute, pushToGit)) {
      yamlChangeSetHelper.queueSettingYamlChangeAsync(settingAttribute, ChangeType.DELETE);
    }
  }

  private void ensureSettingAttributeSafeToDelete(SettingAttribute settingAttribute) {
    if (settingAttribute.getCategory().equals(Category.CLOUD_PROVIDER)) {
      ensureCloudProviderSafeToDelete(settingAttribute);
    } else if (settingAttribute.getCategory().equals(Category.CONNECTOR)) {
      ensureConnectorSafeToDelete(settingAttribute);
    } else if (settingAttribute.getCategory().equals(Category.SETTING)) {
      ensureSettingSafeToDelete(settingAttribute);
    }
  }

  private void ensureSettingSafeToDelete(SettingAttribute settingAttribute) {
    // TODO:: workflow scan for finding out usage in Steps/expression ???
  }

  private void ensureConnectorSafeToDelete(SettingAttribute connectorSetting) {
    if (SettingVariableTypes.ELB.name().equals(connectorSetting.getValue().getType())) {
      List<InfrastructureMapping> infrastructureMappings =
          infrastructureMappingService
              .list(aPageRequest()
                        .addFilter("loadBalancerId", EQ, connectorSetting.getUuid())
                        .withLimit(PageRequest.UNLIMITED)
                        .build(),
                  true)
              .getResponse();

      List<String> infraMappingNames =
          infrastructureMappings.stream().map(InfrastructureMapping::getName).collect(Collectors.toList());
      if (!infraMappingNames.isEmpty()) {
        throw new WingsException(INVALID_REQUEST)
            .addParam("message",
                String.format("Connector [%s] is referenced by %s Service Infrastructure%s [%s].",
                    connectorSetting.getName(), infraMappingNames.size(), infraMappingNames.size() == 1 ? "" : "s",
                    Joiner.on(", ").join(infraMappingNames)));
      }
    } else {
      List<ArtifactStream> artifactStreams =
          artifactStreamService.list(aPageRequest().addFilter("settingId", EQ, connectorSetting.getUuid()).build())
              .getResponse();
      if (!artifactStreams.isEmpty()) {
        List<String> artifactStreamName = artifactStreams.stream()
                                              .map(ArtifactStream::getSourceName)
                                              .filter(java.util.Objects::nonNull)
                                              .collect(Collectors.toList());
        throw new WingsException(INVALID_REQUEST, ReportTarget.USER)
            .addParam("message",
                String.format("Connector [%s] is referenced by %s Artifact Source%s [%s].", connectorSetting.getName(),
                    artifactStreamName.size(), artifactStreamName.size() == 1 ? "" : "s",
                    Joiner.on(", ").join(artifactStreamName)));
      }
    }

    // TODO:: workflow scan for finding out usage in Steps ???
  }

  private void ensureCloudProviderSafeToDelete(SettingAttribute cloudProviderSetting) {
    List<InfrastructureMapping> infrastructureMappings =
        infrastructureMappingService
            .list(aPageRequest()
                      .addFilter("computeProviderSettingId", EQ, cloudProviderSetting.getUuid())
                      .withLimit(PageRequest.UNLIMITED)
                      .build())
            .getResponse();
    if (!infrastructureMappings.isEmpty()) {
      List<String> infraMappingNames =
          infrastructureMappings.stream().map(InfrastructureMapping::getName).collect(Collectors.toList());
      throw new WingsException(INVALID_REQUEST, ReportTarget.USER)
          .addParam("message",
              String.format("Cloud provider [%s] is referenced by %s Service Infrastructure%s [%s].",
                  cloudProviderSetting.getName(), infraMappingNames.size(), infraMappingNames.size() == 1 ? "" : "s",
                  Joiner.on(", ").join(infraMappingNames)));
    }

    List<ArtifactStream> artifactStreams = artifactStreamService
                                               .list(aPageRequest()
                                                         .addFilter("settingId", EQ, cloudProviderSetting.getUuid())
                                                         .withLimit(PageRequest.UNLIMITED)
                                                         .build())
                                               .getResponse();
    if (!artifactStreams.isEmpty()) {
      List<String> artifactStreamNames =
          artifactStreams.stream().map(ArtifactStream::getName).collect(Collectors.toList());
      throw new WingsException(INVALID_REQUEST, ReportTarget.USER)
          .addParam("message",
              String.format("Cloud provider [%s] is referenced by %s Artifact Stream%s [%s].",
                  cloudProviderSetting.getName(), artifactStreamNames.size(),
                  artifactStreamNames.size() == 1 ? "" : "s", Joiner.on(", ").join(artifactStreamNames)));
    }

    // TODO:: workflow scan for finding out usage in Steps ???
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#getByName(java.lang.String, java.lang.String)
   */
  @Override
  public SettingAttribute getByName(String accountId, String appId, String attributeName) {
    return getByName(accountId, appId, GLOBAL_ENV_ID, attributeName);
  }

  @Override
  public SettingAttribute getByName(String accountId, String appId, String envId, String attributeName) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .field("accountId")
        .equal(accountId)
        .field("appId")
        .in(asList(appId, GLOBAL_APP_ID))
        .field("envId")
        .in(asList(envId, GLOBAL_ENV_ID))
        .field("name")
        .equal(attributeName)
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#createDefaultApplicationSettings(java.lang.String)
   */
  @Override
  public void createDefaultApplicationSettings(String appId, String accountId) {
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName(RUNTIME_PATH)
                              .withValue(aStringValue().withValue(DEFAULT_RUNTIME_PATH).build())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName(STAGING_PATH)
                              .withValue(aStringValue().withValue(DEFAULT_STAGING_PATH).build())
                              .build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId(appId)
                                            .withAccountId(accountId)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withName(BACKUP_PATH)
                                            .withValue(aStringValue().withValue(DEFAULT_BACKUP_PATH).build())
                                            .build();
    wingsPersistence.save(settingAttribute);

    // We only need to queue one of them since it will fetch all the setting attributes and pushes them
    yamlChangeSetHelper.queueSettingYamlChangeAsync(settingAttribute, ChangeType.ADD);
  }

  @Override
  public void createDefaultAccountSettings(String accountId) {
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(GLOBAL_APP_ID)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName("User/Password")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withAccessType(USER_PASSWORD)
                                             .withAccountId(accountId)
                                             .build())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(GLOBAL_APP_ID)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName("User/Password :: su - <app-account>")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withAccessType(USER_PASSWORD_SU_APP_USER)
                                             .withAccountId(accountId)
                                             .build())
                              .build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId(GLOBAL_APP_ID)
                                            .withAccountId(accountId)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withName("User/Password :: sudo - <app-account>")
                                            .withValue(aHostConnectionAttributes()
                                                           .withConnectionType(SSH)
                                                           .withAccessType(USER_PASSWORD_SUDO_APP_USER)
                                                           .withAccountId(accountId)
                                                           .build())
                                            .build();
    wingsPersistence.save(settingAttribute);

    // We only need to queue one of them since it will fetch all the setting attributes and pushes them
    yamlChangeSetHelper.queueSettingYamlChangeAsync(settingAttribute, ChangeType.ADD);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#getSettingAttributesByType(java.lang.String,
   * software.wings.settings.SettingValue.SettingVariableTypes)
   */
  @Override
  public List<SettingAttribute> getSettingAttributesByType(String appId, String type) {
    return getSettingAttributesByType(appId, GLOBAL_ENV_ID, type);
  }

  @Override
  public List<SettingAttribute> getSettingAttributesByType(String appId, String envId, String type) {
    PageRequest<SettingAttribute> pageRequest;
    if (appId == null || appId.equals(GLOBAL_APP_ID)) {
      pageRequest = aPageRequest()
                        .addFilter("appId", EQ, GLOBAL_APP_ID)
                        .addFilter("envId", EQ, GLOBAL_ENV_ID)
                        .addFilter("value.type", EQ, type)
                        .build();
    } else {
      Application application = appService.get(appId);
      pageRequest = aPageRequest()
                        .addFilter("accountId", EQ, application.getAccountId())
                        .addFilter("envId", EQ, GLOBAL_ENV_ID)
                        .addFilter("value.type", EQ, type)
                        .build();
    }

    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }

  @Override
  public List<SettingAttribute> getSettingAttributesByType(String accountId, String appId, String envId, String type) {
    PageRequest<SettingAttribute> pageRequest = aPageRequest()
                                                    .addFilter("accountId", EQ, accountId)
                                                    .addFilter("appId", EQ, appId)
                                                    .addFilter("envId", EQ, envId)
                                                    .addFilter("value.type", EQ, type)
                                                    .build();
    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }

  @Override
  public List<SettingAttribute> getGlobalSettingAttributesByType(String accountId, String type) {
    PageRequest<SettingAttribute> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("value.type", EQ, type).build();
    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }

  @Override
  public SettingAttribute getGlobalSettingAttributesById(String accountId, String id) {
    PageRequest<SettingAttribute> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("_id", EQ, id).build();
    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse().get(0);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class).field("accountId").equal(accountId));
  }

  @Override
  public void deleteSettingAttributesByType(String accountId, String appId, String envId, String type) {
    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class)
                                .field("accountId")
                                .equal(accountId)
                                .field("appId")
                                .equal(appId)
                                .field("envId")
                                .equal(envId)
                                .field("value.type")
                                .equal(type));
  }
}
