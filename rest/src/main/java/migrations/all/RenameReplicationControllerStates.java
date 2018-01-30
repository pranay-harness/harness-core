package migrations.all;

import static migrations.MigrationUtil.renameStateTypeAndStateClass;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SETUP;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;

public class RenameReplicationControllerStates implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(RenameReplicationControllerStates.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public void migrate() {
    logger.info("Renaming Kubernetes state types and classes");

    renameStateTypeAndStateClass(KUBERNETES_SETUP, KUBERNETES_SETUP, wingsPersistence, workflowService);
    renameStateTypeAndStateClass(KUBERNETES_DEPLOY, KUBERNETES_DEPLOY, wingsPersistence, workflowService);
    renameStateTypeAndStateClass(
        KUBERNETES_DEPLOY_ROLLBACK, KUBERNETES_DEPLOY_ROLLBACK, wingsPersistence, workflowService);

    logger.info("Finished renaming Kubernetes state types and classes");
  }
}
