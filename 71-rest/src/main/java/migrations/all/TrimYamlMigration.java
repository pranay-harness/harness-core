package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.dl.WingsPersistence;

import java.util.List;

public class TrimYamlMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(TrimYamlMigration.class);

  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<ContainerTask> containerTasks = wingsPersistence.createQuery(ContainerTask.class)
                                             .filter("deploymentType", "KUBERNETES")
                                             .field("advancedConfig")
                                             .exists()
                                             .asList();

    logger.info("Trimming {} advanced yaml configs", containerTasks.size());
    for (ContainerTask containerTask : containerTasks) {
      logger.info("Trimming {}", containerTask.getUuid());
      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
      kubernetesContainerTask.setAdvancedConfig(kubernetesContainerTask.getAdvancedConfig());
    }
    logger.info("Done trimming.");

    wingsPersistence.save(containerTasks);
  }
}
