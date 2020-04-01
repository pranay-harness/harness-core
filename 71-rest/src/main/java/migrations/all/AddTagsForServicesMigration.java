package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.api.DeploymentType;
import software.wings.beans.Account;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Add Tags for DeploymentType and ArtifactType for Service Entity.
 * @author rktummala on 03/28/20
 */
@Slf4j
public class AddTagsForServicesMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public void migrate() {
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class).project(Account.ID_KEY, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();

        try (HIterator<Service> services = new HIterator<>(
                 wingsPersistence.createQuery(Service.class).filter("accountId", account.getUuid()).fetch())) {
          while (services.hasNext()) {
            Service service = services.next();
            if (service.getDeploymentType() == null) {
              List<InfrastructureMapping> infrastructureMappings =
                  wingsPersistence.createQuery(InfrastructureMapping.class)
                      .filter("accountId", account.getUuid())
                      .filter("serviceId", service.getUuid())
                      .project("deploymentType", true)
                      .asList();
              if (isNotEmpty(infrastructureMappings)) {
                Set<String> deploymentTypes =
                    infrastructureMappings.stream().map(infra -> infra.getDeploymentType()).collect(Collectors.toSet());
                String[] deploymentTypesArr = deploymentTypes.toArray(new String[0]);
                logger.info(
                    "Deployment type for service {} is resolved to {}", service.getUuid(), deploymentTypesArr[0]);
                service.setDeploymentType(DeploymentType.valueOf(deploymentTypesArr[0]));
              } else {
                logger.info("Could not resolve Deployment type for service {} ", service.getUuid());
              }
            }

            serviceResourceService.setArtifactTypeTag(service);
            serviceResourceService.setDeploymentTypeTag(service);
          }
        }
      }
    }
  }
}
