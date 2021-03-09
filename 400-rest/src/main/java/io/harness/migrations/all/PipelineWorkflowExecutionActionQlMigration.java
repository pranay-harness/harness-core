package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.MongoUtils.setUnset;

import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.security.PermissionAttribute.Action.EXECUTE_PIPELINE;
import static software.wings.security.PermissionAttribute.Action.EXECUTE_WORKFLOW;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@TargetModule(Module._390_DB_MIGRATION)
public class PipelineWorkflowExecutionActionQlMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<UserGroup> groupHIterator = new HIterator<>(
             wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.appPermissions).exists().fetch())) {
      while (groupHIterator.hasNext()) {
        UserGroup userGroup = groupHIterator.next();
        if (isNotEmpty(userGroup.getAppPermissions())) {
          Set<AppPermission> newAppPermissions = new HashSet<>();
          for (AppPermission appPermission : userGroup.getAppPermissions()) {
            if (isNotEmpty(appPermission.getActions())
                && (appPermission.getPermissionType() == ALL_APP_ENTITIES
                    || appPermission.getPermissionType() == DEPLOYMENT)) {
              Set<Action> newActionSet = new HashSet<>();
              appPermission.getActions().forEach(action -> {
                if (action != null && action.equals(EXECUTE)) {
                  newActionSet.add(EXECUTE_PIPELINE);
                  newActionSet.add(EXECUTE_WORKFLOW);
                }
                newActionSet.add(action);
              });
              appPermission.setActions(newActionSet);
            }
            newAppPermissions.add(appPermission);
          }
          UpdateOperations<UserGroup> operations = wingsPersistence.createUpdateOperations(UserGroup.class);
          setUnset(operations, UserGroupKeys.appPermissions, newAppPermissions);
          wingsPersistence.update(userGroup, operations);
        }
      }
    } catch (Exception e) {
      log.error("Could not run workflow/pipeline execution right graph QL migration", e);
    }
  }
}
