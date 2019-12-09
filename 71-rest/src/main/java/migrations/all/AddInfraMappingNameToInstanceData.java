package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.time.Duration;
import java.util.List;

/**
 * Migration script to add inframapping name to instance data
 * @author rktummala on 04/03/19
 */
@Slf4j
public class AddInfraMappingNameToInstanceData implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private PersistentLocker persistentLocker;

  @Override
  public void migrate() {
    PageRequest<Account> accountPageRequest = aPageRequest().addFieldsIncluded("_id").build();
    List<Account> accounts = accountService.list(accountPageRequest);
    accounts.forEach(account -> {
      List<String> appIds = appService.getAppIdsByAccountId(account.getUuid());
      appIds.forEach(appId -> {
        try {
          logger.info("Adding infraMapping name to instances for appId:" + appId);
          PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
          pageRequest.addFilter("appId", Operator.EQ, appId);
          PageResponse<InfrastructureMapping> response = infraMappingService.list(pageRequest);
          // Response only contains id
          List<InfrastructureMapping> infraMappingList = response.getResponse();

          infraMappingList.forEach(infraMapping -> {
            String infraMappingId = infraMapping.getUuid();
            try (AcquiredLock lock = persistentLocker.tryToAcquireLock(
                     InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(120))) {
              if (lock == null) {
                return;
              }

              try {
                List<Instance> instances = wingsPersistence.createQuery(Instance.class)
                                               .field("isDeleted")
                                               .equal(false)
                                               .field("infraMappingId")
                                               .equal(infraMappingId)
                                               .field("appId")
                                               .equal(appId)
                                               .asList();

                instances.forEach(instance
                    -> wingsPersistence.updateField(
                        Instance.class, instance.getUuid(), "infraMappingName", infraMapping.getName()));
              } catch (Exception ex) {
                logger.warn("Adding infraMappingName failed for infraMappingId [{}]", infraMappingId, ex);
              }
            } catch (Exception e) {
              logger.warn("Failed to acquire lock for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
            }
          });

          logger.info("Adding infraMappingName done for appId:" + appId);
        } catch (WingsException exception) {
          ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
        } catch (Exception ex) {
          logger.warn("Error while setting infraMappingName to instances for app: {}", appId, ex);
        }
      });
    });
  }
}
