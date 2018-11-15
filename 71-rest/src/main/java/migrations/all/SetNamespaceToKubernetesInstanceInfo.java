package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.time.Duration;
import java.util.List;

/**
 * Migration script to set namespace for kubernetes instances.
 * @author rktummala on 11/13/18
 */
public class SetNamespaceToKubernetesInstanceInfo implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(SetNamespaceToKubernetesInstanceInfo.class);

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
          PageResponse<InfrastructureMapping> response = infraMappingService.list(pageRequest);
          // Response only contains id
          List<InfrastructureMapping> infraMappingList = response.getResponse();

          infraMappingList.forEach(infraMapping -> {
            String infraMappingId = infraMapping.getUuid();
            logger.info("Fixing kubernetes instances for infra mappingId:" + infraMappingId);
            try (AcquiredLock lock = persistentLocker.waitToAcquireLock(
                     InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(120), Duration.ofSeconds(120))) {
              if (lock == null) {
                return;
              }

              try {
                List<Instance> instances =
                    wingsPersistence.createQuery(Instance.class)
                        .filter("infraMappingId", infraMappingId)
                        .filter("appId", appId)
                        .filter("instanceType", InstanceType.KUBERNETES_CONTAINER_INSTANCE.name())
                        .filter("isDeleted", false)
                        .asList();

                instances.forEach(instance -> {
                  InstanceInfo instanceInfo = instance.getInstanceInfo();
                  if (instanceInfo == null) {
                    logger.error("instanceInfo is null for instance {}", instance.getUuid());
                    return;
                  }

                  if (!(instanceInfo instanceof KubernetesContainerInfo)) {
                    logger.error("instanceInfo is not of type kubernetes for instance {}", instance.getUuid());
                    return;
                  }

                  KubernetesContainerInfo kubernetesContainerInfo = (KubernetesContainerInfo) instanceInfo;
                  if (isNotEmpty(kubernetesContainerInfo.getNamespace())) {
                    logger.error("namespace is not null in instance info for instance {}", instance.getUuid());
                    return;
                  }

                  String namespace = getNamespace(infraMapping);

                  if (namespace == null) {
                    logger.error("namespace is null in infra mapping for instance {}", instance.getUuid());
                    return;
                  }

                  kubernetesContainerInfo.setNamespace(namespace);
                  wingsPersistence.updateField(
                      Instance.class, instance.getUuid(), "instanceInfo", kubernetesContainerInfo);
                });
                logger.info("Instance fix completed for Kubernetes instances for infra mapping [{}]", infraMappingId);
              } catch (Exception ex) {
                logger.warn("Kubernetes Instance fix failed for infraMappingId [{}]", infraMappingId, ex);
              }
            } catch (Exception e) {
              logger.warn(
                  "Kubernetes - Failed to acquire lock for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
            }
          });

          logger.info("Kubernetes Instance fix done for appId:" + appId);
        } catch (WingsException exception) {
          WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
        } catch (Exception ex) {
          logger.warn("Error while fixing Kubernetes instances for app: {}", appId, ex);
        }
      });
    });
  }

  private String getNamespace(InfrastructureMapping infrastructureMapping) {
    if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
      return ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
    } else if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
      return ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
    } else if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
      return ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
    } else {
      return null;
    }
  }
}
