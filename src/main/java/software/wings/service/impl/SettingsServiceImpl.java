package software.wings.service.impl;

import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SUDO_APP_USER;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD_SU_APP_USER;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Base;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/17/16.
 */
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
    return wingsPersistence.get(SettingAttribute.class, appId, varId);
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
    wingsPersistence.delete(SettingAttribute.class, varId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#getByName(java.lang.String, java.lang.String)
   */
  @Override
  public SettingAttribute getByName(String appId, String attributeName) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .field("appId")
        .equal(appId)
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
                              .withName("USER_PASSWORD")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withType(HOST_CONNECTION_ATTRIBUTES)
                                             .withAccessType(USER_PASSWORD)
                                             .build())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withName("USER_PASSWORD_SU_APP_USER")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withType(HOST_CONNECTION_ATTRIBUTES)
                                             .withAccessType(USER_PASSWORD_SU_APP_USER)
                                             .build())
                              .build());
    wingsPersistence.save(aSettingAttribute()
                              .withAppId(appId)
                              .withName("USER_PASSWORD_SUDO_APP_USER")
                              .withValue(aHostConnectionAttributes()
                                             .withConnectionType(SSH)
                                             .withType(HOST_CONNECTION_ATTRIBUTES)
                                             .withAccessType(USER_PASSWORD_SUDO_APP_USER)
                                             .build())
                              .build());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.SettingsService#getSettingAttributesByType(java.lang.String,
   * software.wings.beans.SettingValue.SettingVariableTypes)
   */
  @Override
  public List<SettingAttribute> getSettingAttributesByType(String appId, SettingVariableTypes type) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .field("appId")
        .in(Arrays.asList(appId, Base.GLOBAL_APP_ID))
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
    return getSettingAttributesByType(Base.GLOBAL_APP_ID, type);
  }
}
