package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SU_APP_USER;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.StringValue.Builder.aStringValue;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.Encryptable;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Validator;

import java.util.List;
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
    delete(appId, GLOBAL_ENV_ID, varId);
  }

  @Override
  public void delete(String appId, String envId, String varId) {
    wingsPersistence.delete(wingsPersistence.createQuery(SettingAttribute.class)
                                .field("appId")
                                .equal(appId)
                                .field("envId")
                                .equal(envId)
                                .field(ID_KEY)
                                .equal(varId));
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
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(GLOBAL_APP_ID)
            .withAccountId(accountId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("User/Password")
            .withValue(aHostConnectionAttributes().withConnectionType(SSH).withAccessType(USER_PASSWORD).build())
            .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(GLOBAL_APP_ID)
            .withAccountId(accountId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("User/Password :: su - <app-account>")
            .withValue(
                aHostConnectionAttributes().withConnectionType(SSH).withAccessType(USER_PASSWORD_SU_APP_USER).build())
            .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(GLOBAL_APP_ID)
            .withAccountId(accountId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("User/Password :: sudo - <app-account>")

            .withValue(
                aHostConnectionAttributes().withConnectionType(SSH).withAccessType(USER_PASSWORD_SUDO_APP_USER).build())
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
    PageRequest<SettingAttribute> pageRequest = aPageRequest()
                                                    .addFilter("appId", Operator.EQ, GLOBAL_APP_ID)
                                                    .addFilter("envId", Operator.EQ, GLOBAL_ENV_ID)
                                                    .addFilter("value.type", Operator.EQ, type)
                                                    .build();
    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }

  @Override
  public List<SettingAttribute> getGlobalSettingAttributesByType(String accountId, String type) {
    PageRequest<SettingAttribute> pageRequest = aPageRequest()
                                                    .addFilter("accountId", Operator.EQ, accountId)
                                                    .addFilter("value.type", Operator.EQ, type)
                                                    .build();
    return wingsPersistence.query(SettingAttribute.class, pageRequest).getResponse();
  }
}
