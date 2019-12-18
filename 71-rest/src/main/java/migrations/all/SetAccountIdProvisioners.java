package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.join;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.SPACE;

import com.google.inject.Inject;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisioner.InfrastructureProvisionerKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SetAccountIdProvisioners implements Migration {
  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;

  private static String DEBUG_LINE = "SET_ACCOUNTID_PROVISIONERS";

  @Override
  public void migrate() {
    Map<String, Object> updateAccountId = new HashMap<>();
    int corrections = 0;
    int errors = 0;
    try (HIterator<InfrastructureProvisioner> provisioners =
             new HIterator<>(wingsPersistence.createQuery(InfrastructureProvisioner.class)
                                 .field(InfrastructureProvisionerKeys.accountId)
                                 .doesNotExist()
                                 .field(InfrastructureProvisionerKeys.appId)
                                 .exists()
                                 .fetch())) {
      for (InfrastructureProvisioner provisioner : provisioners) {
        String appId = provisioner.getAppId();
        if (isNotEmpty(appId)) {
          try {
            String accountId = appService.getAccountIdByAppId(appId);
            if (isNotEmpty(accountId)) {
              updateAccountId.put(InfrastructureProvisionerKeys.accountId, accountId);
              wingsPersistence.updateFields(InfrastructureProvisioner.class, provisioner.getUuid(), updateAccountId);
              logger.info(join(SPACE, DEBUG_LINE,
                  format("Set accountId [%s] for provisioner [%s]", accountId, provisioner.getUuid())));
              corrections++;
            } else {
              errors++;
              logger.error(join(SPACE, DEBUG_LINE, "empty accountId for appId-", appId));
            }
          } catch (Exception ex) {
            errors++;
            logger.error(join(SPACE, DEBUG_LINE, "could not fetch accountId for appId -", appId));
          }
        } else {
          errors++;
          logger.error(join(SPACE, DEBUG_LINE, "This should not be happening, We need appId here"));
        }
      }
    }
    logger.info(HarnessStringUtils.join(SPACE, DEBUG_LINE, "Set AccountId for provisioners done", SPACE,
        "Corrections -", String.valueOf(corrections), "Errors -", String.valueOf(errors)));
  }
}
