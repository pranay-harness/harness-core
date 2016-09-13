package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SU_APP_USER;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.beans.StringValue.Builder.aStringValue;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;

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
  public SettingAttribute save(SettingAttribute envVar) {
    return wingsPersistence.saveAndGet(SettingAttribute.class, envVar);
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
  public SettingAttribute update(SettingAttribute envVar) {
    wingsPersistence.updateFields(SettingAttribute.class, envVar.getUuid(),
        ImmutableMap.of("name", envVar.getName(), "value", envVar.getValue()));
    return wingsPersistence.get(SettingAttribute.class, envVar.getUuid());
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
   * @see software.wings.service.intfc.SettingsService#createDefaultSettings(java.lang.String)
   */
  @Override
  public void createDefaultSettings(String appId) {
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName("User/Password")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withType(HOST_CONNECTION_ATTRIBUTES)
                                             .withAccessType(USER_PASSWORD)
                                             .build())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName("User/Password :: su - <app-account>")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withType(HOST_CONNECTION_ATTRIBUTES)
                                             .withAccessType(USER_PASSWORD_SU_APP_USER)
                                             .build())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withEnvId(GLOBAL_ENV_ID)
                              .withName("User/Password :: sudo - <app-account>")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withType(HOST_CONNECTION_ATTRIBUTES)
                                             .withAccessType(USER_PASSWORD_SUDO_APP_USER)
                                             .build())
                              .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("RUNTIME_PATH")
            .withValue(
                aStringValue().withValue("$HOME/${app.name}/${service.name}/${serviceTemplate.name}/runtime").build())
            .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("BACKUP_PATH")
            .withValue(aStringValue()
                           .withValue("$HOME/${app.name}/${service.name}/${serviceTemplate.name}/backup/${timestampId}")
                           .build())
            .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withEnvId(GLOBAL_ENV_ID)
            .withName("STAGING_PATH")
            .withValue(
                aStringValue()
                    .withValue("$HOME/${app.name}/${service.name}/${serviceTemplate.name}/staging/${timestampId}")
                    .build())
            .build());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#getSettingAttributesByType(java.lang.String,
   * software.wings.beans.SettingValue.SettingVariableTypes)
   */
  @Override
  public List<SettingAttribute> getSettingAttributesByType(String appId, SettingVariableTypes type) {
    return getSettingAttributesByType(appId, GLOBAL_ENV_ID, type);
  }

  @Override
  public List<SettingAttribute> getSettingAttributesByType(String appId, String envId, SettingVariableTypes type) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .field("appId")
        .in(asList(appId, GLOBAL_APP_ID))
        .field("envId")
        .in(asList(envId, GLOBAL_ENV_ID))
        .field("value.type")
        .equal(type)
        .asList();
  }

  /* (non-Javadoc)
   * @see
   * software.wings.service.intfc.SettingsService#getGlobalSettingAttributesByType(software.wings.beans.SettingValue.SettingVariableTypes)
   */
  @Override
  public List<SettingAttribute> getGlobalSettingAttributesByType(SettingVariableTypes type) {
    return getSettingAttributesByType(GLOBAL_APP_ID, type);
  }
}
