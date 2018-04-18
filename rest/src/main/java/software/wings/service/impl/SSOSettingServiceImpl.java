package software.wings.service.impl;

import com.google.inject.Inject;

import software.wings.beans.sso.SamlSettings;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SSOSettingService;

import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class SSOSettingServiceImpl implements SSOSettingService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public SamlSettings getSamlSettingsByIdpUrl(String idpUrl) {
    return wingsPersistence.executeGetOneQuery(
        wingsPersistence.createQuery(SamlSettings.class).field("url").equal(idpUrl));
  }

  public SamlSettings getSamlSettingsByAccountId(String accountId) {
    return wingsPersistence.executeGetOneQuery(
        wingsPersistence.createQuery(SamlSettings.class).field("accountId").equal(accountId));
  }

  @Override
  public SamlSettings saveSamlSettings(SamlSettings settings) {
    SamlSettings queriedSettings = getSamlSettingsByAccountId(settings.getAccountId());
    if (queriedSettings != null) {
      queriedSettings.setUrl(settings.getUrl());
      queriedSettings.setMetaDataFile(settings.getMetaDataFile());
      queriedSettings.setDisplayName(settings.getDisplayName());
      queriedSettings.setOrigin(settings.getOrigin());
      return wingsPersistence.saveAndGet(SamlSettings.class, queriedSettings);
    } else {
      return wingsPersistence.saveAndGet(SamlSettings.class, settings);
    }
  }

  @Override
  public boolean deleteSamlSettings(String accountId) {
    SamlSettings samlSettings = getSamlSettingsByAccountId(accountId);
    if (samlSettings != null) {
      return wingsPersistence.delete(samlSettings);
    }
    return false;
  }

  @Override
  public SamlSettings getSamlSettingsByOrigin(String origin) {
    return wingsPersistence.executeGetOneQuery(
        wingsPersistence.createQuery(SamlSettings.class).field("origin").equal(origin));
  }
}
