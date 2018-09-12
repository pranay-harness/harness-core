package migrations.all;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.lang.String.format;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.HostInstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.time.Duration;
import java.util.List;

/**
 * Migration script to fix the host name issue due to invalid pattern.
 * @author rktummala on 05/03/18
 */
public class FixInstanceDataForAwsSSH implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(FixInstanceDataForAwsSSH.class);

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
          logger.info("Fixing instances for appId:" + appId);
          PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
          pageRequest.addFilter("appId", Operator.EQ, appId);
          pageRequest.addFilter("infraMappingType", Operator.EQ, InfrastructureMappingType.AWS_SSH.getName());
          PageResponse<InfrastructureMapping> response = infraMappingService.list(pageRequest);
          // Response only contains id
          List<InfrastructureMapping> infraMappingList = response.getResponse();

          infraMappingList.forEach(infraMapping -> {
            String infraMappingId = infraMapping.getUuid();
            logger.info("Fixing instances for infraMappingId:" + infraMappingId);
            try (AcquiredLock lock = persistentLocker.tryToAcquireLock(
                     InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(120))) {
              if (lock == null) {
                return;
              }

              try {
                List<Instance> instances = wingsPersistence.createQuery(Instance.class)
                                               .field("infraMappingId")
                                               .equal(infraMappingId)
                                               .field("appId")
                                               .equal(appId)
                                               .field("hostInstanceKey.hostName")
                                               .contains("internal.split")
                                               .asList();

                instances.forEach(instance -> {
                  HostInstanceKey hostInstanceKey = instance.getHostInstanceKey();
                  String hostName = hostInstanceKey.getHostName();
                  String changedHostName = hostName.replace("internal.split(\'\\.\')[0]", "internal");
                  hostInstanceKey.setHostName(changedHostName);
                  HostInstanceInfo instanceInfo = (HostInstanceInfo) instance.getInstanceInfo();
                  instanceInfo.setHostName(changedHostName);

                  UpdateOperations<Instance> updateOperations = wingsPersistence.createUpdateOperations(Instance.class);
                  updateOperations.set("hostInstanceKey.hostName", changedHostName);
                  updateOperations.set("instanceInfo.hostName", changedHostName);
                  wingsPersistence.update(instance, updateOperations);
                });

                logger.info("Instance fix completed for infraMapping [{}]", infraMappingId);
              } catch (Exception ex) {
                logger.warn(format("Instance fix failed for infraMappingId [%s]", infraMappingId), ex);
              }
            } catch (Exception e) {
              logger.warn("Failed to acquire lock for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
            }
          });

          logger.info("Instance sync done for appId:" + appId);
        } catch (WingsException exception) {
          WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
        } catch (Exception ex) {
          logger.warn(format("Error while syncing instances for app: %s", appId), ex);
        }
      });
    });
  }
}
