package software.wings.service.impl.security;

import static software.wings.utils.WingsReflectionUtils.getEncryptedFields;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;

import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.FindOptions;
import software.wings.annotation.Encryptable;
import software.wings.beans.ConfigFile;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/30/17.
 */
public class SecretManagerImpl implements SecretManager {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public List<SecretUsageLog> getUsageLogs(String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    final List<SecretUsageLog> secretUsageLogs = new ArrayList<>();
    final List<String> secretIds = getSecretIds(entityId, variableType);

    Iterator<SecretUsageLog> usageLogQuery =
        wingsPersistence.createQuery(SecretUsageLog.class).field("encryptedDataId").hasAnyOf(secretIds).fetch();
    while (usageLogQuery.hasNext()) {
      SecretUsageLog usageLog = usageLogQuery.next();
      Workflow workflow = wingsPersistence.get(Workflow.class, usageLog.getWorkflowId());
      usageLog.setWorkflowName(workflow.getName());

      secretUsageLogs.add(usageLog);
    }
    return secretUsageLogs;
  }

  @Override
  public List<Pair<Long, EmbeddedUser>> getChangeLogs(String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);
    List<Pair<Long, EmbeddedUser>> rv = new ArrayList<>();
    Iterator<EncryptedData> encryptedDataQuery =
        wingsPersistence.createQuery(EncryptedData.class).field("_id").hasAnyOf(secretIds).fetch();
    while (encryptedDataQuery.hasNext()) {
      EncryptedData encryptedData = encryptedDataQuery.next();
      rv.addAll(encryptedData.getAllUpdates());
    }

    return rv;
  }

  private List<String> getSecretIds(String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = new ArrayList<>();
    switch (variableType) {
      case SERVICE_VARIABLE:
        Iterator<ServiceVariable> serviceVaribaleQuery = wingsPersistence.createQuery(ServiceVariable.class)
                                                             .field("_id")
                                                             .equal(entityId)
                                                             .fetch(new FindOptions().limit(1));
        if (serviceVaribaleQuery.hasNext()) {
          ServiceVariable serviceVariable = serviceVaribaleQuery.next();
          List<Field> encryptedFields = getEncryptedFields(serviceVariable.getClass());

          for (Field field : encryptedFields) {
            Field encryptedRefField = getEncryptedRefField(field, serviceVariable);
            encryptedRefField.setAccessible(true);
            secretIds.add((String) encryptedRefField.get(serviceVariable));
          }
        }
        break;

      case CONFIG_FILE:
        secretIds.add(entityId);
        break;

      default:
        Iterator<SettingAttribute> settingAttributeQuery = wingsPersistence.createQuery(SettingAttribute.class)
                                                               .field("_id")
                                                               .equal(entityId)
                                                               .fetch(new FindOptions().limit(1));
        if (settingAttributeQuery.hasNext()) {
          SettingAttribute settingAttribute = settingAttributeQuery.next();

          List<Field> encryptedFields = getEncryptedFields(settingAttribute.getValue().getClass());

          for (Field field : encryptedFields) {
            Field encryptedRefField = getEncryptedRefField(field, (Encryptable) settingAttribute.getValue());
            encryptedRefField.setAccessible(true);
            secretIds.add((String) encryptedRefField.get(settingAttribute.getValue()));
          }
        }
    }
    return secretIds;
  }
}
