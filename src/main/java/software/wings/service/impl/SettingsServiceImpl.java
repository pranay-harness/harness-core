package software.wings.service.impl;

import static software.wings.beans.HostConnectionAttributes.AccessType.PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AccessType.PASSWORD_SUDO_APP_ACCOUNT;
import static software.wings.beans.HostConnectionAttributes.AccessType.PASSWORD_SU_APP_ACCOUNT;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.beans.SettingValue.SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by anubhaw on 5/17/16.
 */
public class SettingsServiceImpl implements SettingsService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<SettingAttribute> list(PageRequest<SettingAttribute> req) {
    return wingsPersistence.query(SettingAttribute.class, req);
  }

  @Override
  public SettingAttribute save(SettingAttribute envVar) {
    return wingsPersistence.saveAndGet(SettingAttribute.class, envVar);
  }

  @Override
  public SettingAttribute get(String appId, String varId) {
    return wingsPersistence.get(SettingAttribute.class, varId);
  }

  @Override
  public SettingAttribute update(SettingAttribute envVar) {
    wingsPersistence.updateFields(SettingAttribute.class, envVar.getUuid(),
        ImmutableMap.of("name", envVar.getName(), "value", envVar.getValue()));
    return wingsPersistence.get(SettingAttribute.class, envVar.getUuid());
  }

  @Override
  public void delete(String appId, String varId) {
    wingsPersistence.delete(SettingAttribute.class, varId);
  }

  @Override
  public SettingAttribute getByName(String appId, String attributeName) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .field("appId")
        .equal(appId)
        .field("name")
        .equal(attributeName)
        .get();
  }

  @Override
  public void createDefaultSettings(String appId) {
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withName("PASSWORD")
            .withValue(aHostConnectionAttributes().withConnectionType(SSH).withAccessType(PASSWORD).build())
            .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withName("PASSWORD_SU_APP_ACCOUNT")
            .withValue(
                aHostConnectionAttributes().withConnectionType(SSH).withAccessType(PASSWORD_SU_APP_ACCOUNT).build())
            .build());
    wingsPersistence.save(
        aSettingAttribute()
            .withAppId(appId)
            .withName("PASSWORD_SUDO_APP_ACCOUNT")
            .withValue(
                aHostConnectionAttributes().withConnectionType(SSH).withAccessType(PASSWORD_SUDO_APP_ACCOUNT).build())
            .build());
  }

  @Override
  public List<SettingAttribute> getSettingAttributesByType(String appId, SettingVariableTypes type) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .field("appId")
        .equal(appId)
        .field("value.type")
        .equal(type)
        .asList();
  }

  @Override
  public List<SettingAttribute> getGlobalSettingAttributesByType(SettingVariableTypes type) {
    return getSettingAttributesByType(SettingAttribute.GLOBAL_APP_ID, type);
  }
}
