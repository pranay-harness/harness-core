package io.harness.migrations.all;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Add Tags for DeploymentType and ArtifactType for Service Entity.
 * @author rktummala on 03/28/20
 */
@Slf4j
@TargetModule(Module._390_DB_MIGRATION)
public class AddTagsForServicesAndEnvsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;

  @Override
  public void migrate() {
    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class).project(Account.ID_KEY2, true).fetch())) {
      while (accounts.hasNext()) {
        final Account account = accounts.next();

        try (HIterator<Service> services = new HIterator<>(
                 wingsPersistence.createQuery(Service.class).filter("accountId", account.getUuid()).fetch())) {
          while (services.hasNext()) {
            Service service = services.next();
            serviceResourceService.setArtifactTypeTag(service);
            serviceResourceService.setDeploymentTypeTag(service);
          }
        }

        try (HIterator<Environment> environments = new HIterator<>(
                 wingsPersistence.createQuery(Environment.class).filter("accountId", account.getUuid()).fetch())) {
          while (environments.hasNext()) {
            Environment environment = environments.next();
            environmentService.setEnvironmentTypeTag(environment);
          }
        }
      }
    }
  }
}
