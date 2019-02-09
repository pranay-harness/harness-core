package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfrastructureProvisionerService;

public class TerraformIsTemplatizedMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(TerraformIsTemplatizedMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;

  @Override
  public void migrate() {
    Query<InfrastructureProvisioner> query =
        wingsPersistence.createQuery(InfrastructureProvisioner.class, excludeAuthority)
            .field("infrastructureProvisionerType")
            .equal("TERRAFORM");
    try (HIterator<InfrastructureProvisioner> provisioners = new HIterator<>(query.fetch())) {
      while (provisioners.hasNext()) {
        InfrastructureProvisioner provisioner = null;
        try {
          provisioner = provisioners.next();
          if (infrastructureProvisionerService.isTemplatizedProvisioner(
                  (TerraformInfrastructureProvisioner) provisioner)) {
            ((TerraformInfrastructureProvisioner) provisioner).setTemplatized(true);
          } else {
            ((TerraformInfrastructureProvisioner) provisioner).setTemplatized(false);
          }

          infrastructureProvisionerService.update(provisioner);
        } catch (Exception ex) {
          logger.error("Error while updating isTemplatized field for provisioner: {}",
              provisioner != null ? provisioner.getName() : "", ex);
        }
      }
    } catch (Exception ex) {
      logger.error("Failed - Populating templatized field in TerraformInfrastructureProvisioner", ex);
    }
  }
}
