package migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_INSTANCE_FILTER;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.dl.WingsPersistence;

import java.util.List;
@Slf4j
public class InfraProvisionerFilteringTypeMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    // We could implement a more intelligent logic like batching etc.
    // But it is probably not needed right now, as we have very few provisioners created
    logger.info("Getting all provisioners to migrate blue prints");
    PageRequest<InfrastructureProvisioner> request = aPageRequest().withLimit(UNLIMITED).build();
    List<InfrastructureProvisioner> provisioners =
        wingsPersistence.query(InfrastructureProvisioner.class, request).getResponse();
    if (isNotEmpty(provisioners)) {
      provisioners.forEach(provisioner -> {
        try {
          List<InfrastructureMappingBlueprint> blueprints = provisioner.getMappingBlueprints();
          if (isNotEmpty(blueprints)) {
            blueprints.forEach(blueprint -> { blueprint.setNodeFilteringType(AWS_INSTANCE_FILTER); });
            wingsPersistence.saveAndGet(InfrastructureProvisioner.class, provisioner);
          }
        } catch (Exception ex) {
          logger.error(ExceptionUtils.getMessage(ex), ex);
        }
      });
    }
    logger.info("Completed update of blue prints");
  }
}