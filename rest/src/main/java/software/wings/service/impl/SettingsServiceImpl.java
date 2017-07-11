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
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.encryption.Encryptable;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/17/16.
 */
@ValidateOnExecution
@Singleton
public class SettingsServiceImpl implements SettingsService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingValidationService settingValidationService;
  @Inject private AppService appService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<SettingAttribute> list(PageRequest<SettingAttribute> req) {
    return wingsPersistence.query(SettingAttribute.class, req);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#save(software.wings.beans.SettingAttribute)
   */
  @Override
  public SettingAttribute save(SettingAttribute settingAttribute) {
    settingValidationService.validate(settingAttribute);
    if (settingAttribute.getValue() != null) {
      if (settingAttribute.getValue() instanceof Encryptable) {
        ((Encryptable) settingAttribute.getValue()).setAccountId(settingAttribute.getAccountId());
      }
    }
    return Validator.duplicateCheck(()
                                        -> wingsPersistence.saveAndGet(SettingAttribute.class, settingAttribute),
        "name", settingAttribute.getName());
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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#update(software.wings.beans.SettingAttribute)
   */
  @Override
  public SettingAttribute update(SettingAttribute settingAttribute) {
    settingValidationService.validate(settingAttribute);

    ImmutableMap.Builder<String, Object> fields =
        ImmutableMap.<String, Object>builder().put("name", settingAttribute.getName());
    if (settingAttribute.getValue() != null) {
      if (settingAttribute.getValue() instanceof Encryptable) {
        ((Encryptable) settingAttribute.getValue()).setAccountId(settingAttribute.getAccountId());
      }
      fields.put("value", settingAttribute.getValue());
    }
    wingsPersistence.updateFields(SettingAttribute.class, settingAttribute.getUuid(), fields.build());
    return wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#delete(java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String appId, String varId) {
    SettingAttribute settingAttribute = get(varId);
    Validator.notNullCheck("Setting Value", settingAttribute);
    ensureSettingAttributeSafeToDelete(settingAttribute);
    wingsPersistence.delete(settingAttribute);
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
                        .build())
              .getResponse();
      if (infrastructureMappings.size() > 0) {
        List<String> infraMappingNames =
            infrastructureMappings.stream().map(InfrastructureMapping::getDisplayName).collect(Collectors.toList());
        throw new WingsException(INVALID_REQUEST, "message",
            String.format("Connector [%s] is referenced by %s Service Infrastructure%s [%s].",
                connectorSetting.getName(), infraMappingNames.size(), infraMappingNames.size() == 1 ? "" : "s",
                Joiner.on(", ").join(infraMappingNames)));
      }
    } else {
      List<ArtifactStream> artifactStreams =
          artifactStreamService.list(aPageRequest().addFilter("settingId", EQ, connectorSetting.getUuid()).build())
              .getResponse();
      if (artifactStreams.size() > 0) {
        List<String> artifactStreamName = artifactStreams.stream()
                                              .map(ArtifactStream::getSourceName)
                                              .filter(java.util.Objects::nonNull)
                                              .collect(Collectors.toList());
        throw new WingsException(INVALID_REQUEST, "message",
            String.format("Connector [%s] is referenced by %s Artifact Source%s [%s].", connectorSetting.getName(),
                artifactStreamName.size(), artifactStreamName.size() == 1 ? "" : "s",
                Joiner.on(", ").join(artifactStreamName)));
      }
    }

    // TODO:: workflow scan for finding out usage in Steps ???
  }

  private void ensureCloudProviderSafeToDelete(SettingAttribute clodProviderSetting) {
    List<InfrastructureMapping> infrastructureMappings =
        infrastructureMappingService
            .list(aPageRequest()
                      .addFilter("computeProviderSettingId", EQ, clodProviderSetting.getUuid())
                      .withLimit(PageRequest.UNLIMITED)
                      .build())
            .getResponse();
    if (infrastructureMappings.size() > 0) {
      List<String> infraMappingNames =
          infrastructureMappings.stream().map(InfrastructureMapping::getDisplayName).collect(Collectors.toList());
      throw new WingsException(INVALID_REQUEST, "message",
          String.format("Cloud provider [%s] is referenced by %s Service Infrastructure%s [%s].",
              clodProviderSetting.getName(), infraMappingNames.size(), infraMappingNames.size() == 1 ? "" : "s",
              Joiner.on(", ").join(infraMappingNames)));
    }
    // TODO:: workflow scan for finding out usage in Steps ???
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#getByName(java.lang.String, java.lang.String)
   */
  @Override
  public SettingAttribute getByName(String appId, String attributeName) {
    return getByName(appId, GLOBAL_ENV_ID, attributeName);
  }

  @Override
  public SettingAttribute getByName(String appId, String envId, String attributeName) {
    return wingsPersistence.createQuery(SettingAttribute.class)
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
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withAccountId(accountId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("RUNTIME_PATH")
            .withValue(
                aStringValue().withValue("$HOME/${app.name}/${service.name}/${serviceTemplate.name}/runtime").build())
            .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withAccountId(accountId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("BACKUP_PATH")
            .withValue(aStringValue()
                           .withValue("$HOME/${app.name}/${service.name}/${serviceTemplate.name}/backup/${timestampId}")
                           .build())
            .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withAccountId(accountId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("STAGING_PATH")
            .withValue(
                aStringValue()
                    .withValue("$HOME/${app.name}/${service.name}/${serviceTemplate.name}/staging/${timestampId}")
                    .build())
            .build());
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
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(GLOBAL_APP_ID)
                              .withAccountId(accountId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName("User/Password :: sudo - <app-account>")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withAccessType(USER_PASSWORD_SUDO_APP_USER)
                                             .withAccountId(accountId)
                                             .build())
                              .build());
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
  public List<SettingAttribute> getGlobalSettingAttributesByType(String accountId, String type) {
    PageRequest<SettingAttribute> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("value.type", EQ, type).build();
    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class).field("accountId").equal(accountId));
  }
}
