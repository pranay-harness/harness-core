package software.wings.integration.migration.legacy;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.ServiceVariable;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.AppService;

import java.util.List;

/**
 * Created by rsingh on 10/17/17.
 */
@Integration
@Ignore
public class ServiceVariableEncryptionUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(ServiceVariableEncryptionUtil.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

  @Test
  public void migrateServiceVariableEncryption() throws InterruptedException {
    List<ServiceVariable> serviceVariables = wingsPersistence.createQuery(ServiceVariable.class).asList();

    logger.info("will update " + serviceVariables.size() + " records");

    int updated = 0;
    for (ServiceVariable serviceVariable : serviceVariables) {
      String appId = serviceVariable.getAppId();
      if (!appService.exist(appId)) {
        logger.info("\nDeleting orphan service var: " + serviceVariable.getName());
        //        wingsPersistence.delete(serviceVariable);
        continue;
      }
      String accountId = appService.get(appId).getAccountId();
      logger.info("\naccountId = " + accountId);
      logger.info("appId = " + appId);
      logger.info("chars = " + new String(serviceVariable.getValue()));
      try {
        SimpleEncryption simpleEncryption = new SimpleEncryption(appId);
        char[] decryptedValue = simpleEncryption.decryptChars(serviceVariable.getValue());
        logger.info("decrypted chars : " + new String(decryptedValue));
        serviceVariable.setAccountId(accountId);
        serviceVariable.setValue(decryptedValue);
      } catch (Exception e) {
        logger.error("", e);
      }
      updated++;

      //      wingsPersistence.save(serviceVariable);
    }

    logger.info("Complete. Updated " + updated + " service vars.");
  }
}
