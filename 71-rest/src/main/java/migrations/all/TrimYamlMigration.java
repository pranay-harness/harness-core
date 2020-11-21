package migrations.all;

import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTask.ContainerTaskKeys;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;

@Slf4j
public class TrimYamlMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    List<ContainerTask> containerTasks = wingsPersistence.createQuery(ContainerTask.class)
                                             .filter(ContainerTaskKeys.deploymentType, "KUBERNETES")
                                             .field("advancedConfig")
                                             .exists()
                                             .asList();

    log.info("Trimming {} advanced yaml configs", containerTasks.size());
    for (ContainerTask containerTask : containerTasks) {
      log.info("Trimming {}", containerTask.getUuid());
      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
      kubernetesContainerTask.setAdvancedConfig(kubernetesContainerTask.getAdvancedConfig());
    }
    log.info("Done trimming.");

    wingsPersistence.save(containerTasks);
  }
}
