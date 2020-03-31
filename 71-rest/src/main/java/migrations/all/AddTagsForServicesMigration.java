package migrations.all;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;

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
            serviceResourceService.setArtifactTypeTag(service);
            serviceResourceService.setDeploymentTypeTag(service);
          }
        }
      }
    }
  }
}
