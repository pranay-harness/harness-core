package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.infrastructure.instance.key.PodInstanceKey;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfrastructureMappingService;

import java.time.Duration;
import java.util.List;

/**
 * Migration script to set namespace for kubernetes instances.
 * @author bzane on 5/10/19
 */
@Slf4j
public class SetNamespaceInContainerInstanceInfo implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private PersistentLocker persistentLocker;

  @Override
  public void migrate() {
    for (Account account :
        wingsPersistence.createQuery(Account.class, excludeAuthority).project(Account.ID_KEY, true)) {
      for (Application app : wingsPersistence.createQuery(Application.class)
                                 .filter(Application.ACCOUNT_ID_KEY, account.getUuid())
                                 .project(Application.ID_KEY, true)) {
        String appId = app.getUuid();
        try {
          logger.info("Fixing instances for appId:" + appId);
          PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
          pageRequest.addFilter("appId", Operator.EQ, appId);
          PageResponse<InfrastructureMapping> response = infraMappingService.list(pageRequest);
          // Response only contains id
          List<InfrastructureMapping> infraMappingList = response.getResponse();

          for (InfrastructureMapping infraMapping : infraMappingList) {
            String infraMappingId = infraMapping.getUuid();
            logger.info("Fixing kubernetes instances for infra mappingId:" + infraMappingId);
            try (AcquiredLock ignore = persistentLocker.waitToAcquireLock(
                     InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(120), Duration.ofSeconds(120))) {
              try {
                List<Instance> instances =
                    wingsPersistence.createQuery(Instance.class)
                        .filter("infraMappingId", infraMappingId)
                        .filter("appId", appId)
                        .filter("instanceType", InstanceType.KUBERNETES_CONTAINER_INSTANCE.name())
                        .filter("isDeleted", false)
                        .asList();

                for (Instance instance : instances) {
                  InstanceInfo instanceInfo = instance.getInstanceInfo();
                  if (instanceInfo == null) {
                    logger.error("instanceInfo is null for instance {}", instance.getUuid());
                    continue;
                  }

                  if (!(instanceInfo instanceof KubernetesContainerInfo)) {
                    logger.error("instanceInfo is not of type kubernetes for instance {}", instance.getUuid());
                    continue;
                  }

                  KubernetesContainerInfo kubernetesContainerInfo = (KubernetesContainerInfo) instanceInfo;
                  String namespace = kubernetesContainerInfo.getNamespace();

                  if (isBlank(namespace)) {
                    logger.error("namespace is blank in container info for {}", instance.getUuid());
                    continue;
                  }

                  if (instance.getContainerInstanceKey() == null && instance.getPodInstanceKey() == null) {
                    logger.error("container or pod key not found for {}", instance.getUuid());
                    continue;
                  }

                  ContainerInstanceKey containerInstanceKey = instance.getContainerInstanceKey();
                  if (containerInstanceKey != null) {
                    containerInstanceKey.setNamespace(kubernetesContainerInfo.getNamespace());
                    wingsPersistence.updateField(
                        Instance.class, instance.getUuid(), InstanceKeys.containerInstanceKey, containerInstanceKey);
                  } else {
                    PodInstanceKey podInstanceKey = instance.getPodInstanceKey();
                    podInstanceKey.setNamespace(kubernetesContainerInfo.getNamespace());
                    wingsPersistence.updateField(
                        Instance.class, instance.getUuid(), InstanceKeys.podInstanceKey, podInstanceKey);
                  }
                }
                logger.info("Instance fix completed for Kubernetes instances for infra mapping [{}]", infraMappingId);
              } catch (Exception ex) {
                logger.warn("Kubernetes Instance fix failed for infraMappingId [{}]", infraMappingId, ex);
              }
            }
          }
          logger.info("Kubernetes Instance fix done for appId:" + appId);
        } catch (Exception ex) {
          logger.warn("Error while fixing Kubernetes instances for app: {}", appId, ex);
        }
      }
    }
  }
}
