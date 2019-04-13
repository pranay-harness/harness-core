package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.InfrastructureMapping.INFRA_MAPPING_TYPE_KEY;
import static software.wings.beans.InfrastructureMapping.SERVICE_ID_KEY;
import static software.wings.beans.InfrastructureMappingType.AZURE_KUBERNETES;
import static software.wings.beans.InfrastructureMappingType.DIRECT_KUBERNETES;
import static software.wings.beans.InfrastructureMappingType.GCP_KUBERNETES;
import static software.wings.beans.Service.IS_K8S_V2_KEY;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.List;

public class K8sV2ServiceInfraReleaseNameMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(K8sV2ServiceInfraReleaseNameMigration.class);
  private static final String RELEASE_NAME_KEY = "releaseName";
  private static final String INFRA_KUBERNETES_INFRAID_EXPRESSION = "${infra.kubernetes.infraId}";

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Running K8sV2ServiceInfraReleaseNameMigration");
    migrateServiceInfra();
    logger.info("Completed K8sV2ServiceInfraReleaseNameMigration");
  }

  private void migrateServiceInfra() {
    List<Service> servicesList = new ArrayList<>();

    logger.info("Fetching k8v2 services");
    try (HIterator<Service> services = new HIterator<>(
             wingsPersistence.createQuery(Service.class, excludeAuthority).filter(IS_K8S_V2_KEY, true).fetch())) {
      while (services.hasNext()) {
        servicesList.add(services.next());
      }
    }

    List<String> allowedInfraMappingTypes = new ArrayList<>();
    allowedInfraMappingTypes.add(AZURE_KUBERNETES.name());
    allowedInfraMappingTypes.add(DIRECT_KUBERNETES.name());
    allowedInfraMappingTypes.add(GCP_KUBERNETES.name());

    logger.info("Migrating service Infra for k8v2");
    for (Service service : servicesList) {
      if (!service.isK8sV2()) {
        continue;
      }

      try (HIterator<InfrastructureMapping> infrastructureMappingHIterator =
               new HIterator<>(wingsPersistence.createQuery(InfrastructureMapping.class, excludeAuthority)
                                   .filter(SERVICE_ID_KEY, service.getUuid())
                                   .field(INFRA_MAPPING_TYPE_KEY)
                                   .in(allowedInfraMappingTypes)
                                   .fetch())) {
        while (infrastructureMappingHIterator.hasNext()) {
          InfrastructureMapping infraMapping = infrastructureMappingHIterator.next();

          if (infraMapping instanceof AzureKubernetesInfrastructureMapping
              || infraMapping instanceof DirectKubernetesInfrastructureMapping
              || infraMapping instanceof GcpKubernetesInfrastructureMapping) {
            updateServiceInfraWithReleaseName((ContainerInfrastructureMapping) infraMapping);
          }
        }
      }
    }
  }

  private void updateServiceInfraWithReleaseName(ContainerInfrastructureMapping infraMapping) {
    if (isNotBlank(infraMapping.getReleaseName())) {
      return;
    }

    UpdateOperations<InfrastructureMapping> updateOperations =
        wingsPersistence.createUpdateOperations(InfrastructureMapping.class)
            .set(RELEASE_NAME_KEY, INFRA_KUBERNETES_INFRAID_EXPRESSION);

    Query<InfrastructureMapping> query = wingsPersistence.createQuery(InfrastructureMapping.class)
                                             .filter(InfrastructureMapping.ID_KEY, infraMapping.getUuid());

    wingsPersistence.update(query, updateOperations);
  }
}
