package software.wings.service.impl.security;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.ConfigFile;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.core.queue.Queue;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Created by rsingh on 11/6/17.
 */
public abstract class AbstractSecretServiceImpl {
  protected static final Logger logger = LoggerFactory.getLogger(AbstractSecretServiceImpl.class);
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DelegateProxyFactory delegateProxyFactory;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;

  protected Encryptable getEntityByName(
      String accountId, String appId, SettingVariableTypes variableType, String entityName) {
    Encryptable rv = null;
    switch (variableType) {
      case SERVICE_VARIABLE:
        rv = wingsPersistence.createQuery(ServiceVariable.class)
                 .filter("accountId", accountId)
                 .filter("appId", appId)
                 .filter("name", entityName)
                 .get();
        break;

      case CONFIG_FILE:
        rv = wingsPersistence.createQuery(ConfigFile.class)
                 .filter("accountId", accountId)
                 .filter("appId", appId)
                 .filter("name", entityName)
                 .get();
        break;

      default:
        final SettingAttribute settingAttribute = wingsPersistence.createQuery(SettingAttribute.class)
                                                      .filter("accountId", accountId)
                                                      .filter("name", entityName)
                                                      .filter("value.type", variableType)
                                                      .get();
        if (settingAttribute != null) {
          rv = (Encryptable) settingAttribute.getValue();
        }
    }

    Preconditions.checkNotNull(
        rv, "Could not find entity accountId: " + accountId + " type: " + variableType + " name: " + entityName);
    return rv;
  }

  protected abstract EncryptionConfig getSecretConfig(String accountId);
}
