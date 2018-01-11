package software.wings.service.impl.security;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.mongodb.morphia.query.FindOptions;
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

import java.util.Iterator;

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
        Iterator<ServiceVariable> serviceVaribaleQuery = wingsPersistence.createQuery(ServiceVariable.class)
                                                             .field("accountId")
                                                             .equal(accountId)
                                                             .field("appId")
                                                             .equal(appId)
                                                             .field("name")
                                                             .equal(entityName)
                                                             .fetch(new FindOptions().limit(1));
        if (serviceVaribaleQuery.hasNext()) {
          ServiceVariable serviceVariable = serviceVaribaleQuery.next();
          rv = serviceVariable;
        }
        break;

      case CONFIG_FILE:
        Iterator<ConfigFile> configFileQuery = wingsPersistence.createQuery(ConfigFile.class)
                                                   .field("accountId")
                                                   .equal(accountId)
                                                   .field("appId")
                                                   .equal(appId)
                                                   .field("name")
                                                   .equal(entityName)
                                                   .fetch(new FindOptions().limit(1));
        if (configFileQuery.hasNext()) {
          rv = configFileQuery.next();
        }
        break;

      default:
        Iterator<SettingAttribute> settingAttributeQuery = wingsPersistence.createQuery(SettingAttribute.class)
                                                               .field("accountId")
                                                               .equal(accountId)
                                                               .field("name")
                                                               .equal(entityName)
                                                               .field("value.type")
                                                               .equal(variableType)
                                                               .fetch(new FindOptions().limit(1));
        if (settingAttributeQuery.hasNext()) {
          rv = (Encryptable) settingAttributeQuery.next().getValue();
        }
        break;
    }

    Preconditions.checkNotNull(
        rv, "Could not find entity accountId: " + accountId + " type: " + variableType + " name: " + entityName);
    return rv;
  }

  protected abstract EncryptionConfig getSecretConfig(String accountId);
}
