package migrations.all;

import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SETUP;

import static migrations.MigrationUtils.renameStateTypeAndStateClass;

import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;

@Slf4j
public class RenameReplicationControllerStates implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    log.info("Renaming Kubernetes state types and classes");

    renameStateTypeAndStateClass(KUBERNETES_SETUP, KUBERNETES_SETUP, wingsPersistence, workflowService);
    renameStateTypeAndStateClass(KUBERNETES_DEPLOY, KUBERNETES_DEPLOY, wingsPersistence, workflowService);
    renameStateTypeAndStateClass(
        KUBERNETES_DEPLOY_ROLLBACK, KUBERNETES_DEPLOY_ROLLBACK, wingsPersistence, workflowService);

    log.info("Finished renaming Kubernetes state types and classes");
  }
}
