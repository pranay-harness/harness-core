package migrations.all;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.ServiceVariableKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.service.YamlHelper;

@Slf4j
public class AmendCorruptedEncryptedServiceVariable implements Migration {
  @Inject private YamlHelper yamlHelper;
  @Inject private WingsPersistence wingsPersistence;

  private static final String DEBUG_LINE = "AMEND_SERVICE_VARIABLES: ";

  @Override
  public void migrate() {
    int updations = 0;
    logger.info(
        HarnessStringUtils.join(StringUtils.SPACE, DEBUG_LINE, "Starting amending corrupted service variables"));
    try (HIterator<ServiceVariable> serviceVariableHIterator =
             new HIterator<>(wingsPersistence.createQuery(ServiceVariable.class)
                                 .field(ServiceVariableKeys.accountId)
                                 .exists()
                                 .field(ServiceVariableKeys.encryptedValue)
                                 .contains(":")
                                 .fetch())) {
      for (ServiceVariable serviceVariable : serviceVariableHIterator) {
        /*
         encrypted values in hashicorp vault are stored differently from others. Their yaml
         reference does not have the same format
         */
        if (serviceVariable.getEncryptedValue() != null && serviceVariable.getEncryptedValue().contains(":")
            && !serviceVariable.getEncryptedValue().startsWith("hashicorpvault:")) {
          String corruptedValue = serviceVariable.getEncryptedValue();
          String correctValue = yamlHelper.extractEncryptedRecordId(corruptedValue, serviceVariable.getAccountId());
          logger.info(HarnessStringUtils.join(StringUtils.SPACE, DEBUG_LINE,
              format("Updating Service Variable from %s-> %s", corruptedValue, correctValue)));
          serviceVariable.setEncryptedValue(correctValue);
          wingsPersistence.save(serviceVariable);
          updations++;
        }
      }
    }
    logger.info(HarnessStringUtils.join(StringUtils.SPACE, DEBUG_LINE,
        "Finished amending corrupted "
            + "service variables total:",
        String.valueOf(updations)));
  }
}
