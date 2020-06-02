package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;
import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.security.PermissionAttribute.Action.EXECUTE_PIPELINE;
import static software.wings.security.PermissionAttribute.Action.EXECUTE_WORKFLOW;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class PipelineWorkflowExecutionActionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<UserGroup> userGroupHIterator = new HIterator<>(
             wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.appPermissions).exists().fetch())) {
      while (userGroupHIterator.hasNext()) {
        UserGroup userGroup = userGroupHIterator.next();
        if (isNotEmpty(userGroup.getAppPermissions())) {
          Set<AppPermission> appPermissions = new HashSet<>();
          for (AppPermission appPermission : userGroup.getAppPermissions()) {
            if (isNotEmpty(appPermission.getActions())
                && (appPermission.getPermissionType() == ALL_APP_ENTITIES
                       || appPermission.getPermissionType() == DEPLOYMENT)) {
              Set<Action> actionSet = new HashSet<>();
              appPermission.getActions().forEach(action -> {
                if (action != null && action.equals(EXECUTE)) {
                  actionSet.add(EXECUTE_PIPELINE);
                  actionSet.add(EXECUTE_WORKFLOW);
                }
                actionSet.add(action);
              });
              appPermission.setActions(actionSet);
            }
            appPermissions.add(appPermission);
          }
          UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
          setUnset(operations, UserGroupKeys.appPermissions, appPermissions);
          wingsPersistence.update(userGroup, operations);
        }
      }
    } catch (Exception e) {
      logger.error("Could not run workflow/pipeline execution right migration", e);
    }
  }
}