package migrations.all;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.dl.WingsPersistence;

import java.util.List;

@Slf4j
public class TrimYamlMigration implements Migration {
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
