package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Application.ApplicationKeys;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import io.harness.persistence.HQuery;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.security.GenericEntityFilter;
import software.wings.service.intfc.UserGroupService;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Migration script to cleanup orphan app ids from user groups
 */
@Slf4j
public class RemoveDeletedAppIdsFromUserGroups implements Migration {
  @Inject private WingsPersistence persistence;

  @Inject private UserGroupService userGroupService;

  @Override
  public void migrate() {
    try {
      logger.info("Start - Deleting orphan app ids from user groups");

      Set<String> existingAppIds = persistence.createQuery(Application.class)
                                       .project(ApplicationKeys.appId, true)
                                       .asList()
                                       .stream()
                                       .map(Application::getAppId)
                                       .collect(Collectors.toSet());

      Query<UserGroup> query = persistence.createQuery(UserGroup.class, HQuery.excludeAuthority);

      try (HIterator<UserGroup> iterator = new HIterator<>(query.fetch())) {
        for (UserGroup userGroup : iterator) {
          Set<String> deletedAppIds = getDeletedAppIds(userGroup, existingAppIds);
          userGroupService.removeAppIdsFromAppPermissions(userGroup, deletedAppIds);
        }
      }
      logger.info("Deleting orphan app ids from user groups finished successfully");
    } catch (Exception ex) {
      logger.error("Error while deleting orphan app ids from user groups", ex);
    }
  }

  private Set<String> getDeletedAppIds(UserGroup userGroup, Set<String> existingAppIds) {
    Set<String> deletedIds = new HashSet<>();

    if (isEmpty(existingAppIds)) {
      return deletedIds;
    }

    Set<AppPermission> groupAppPermissions = userGroup.getAppPermissions();

    if (isEmpty(groupAppPermissions)) {
      return deletedIds;
    }

    for (AppPermission permission : groupAppPermissions) {
      GenericEntityFilter filter = permission.getAppFilter();
      Set<String> ids = filter.getIds();
      if (isEmpty(ids)) {
        continue;
      }

      for (String id : ids) {
        if (!existingAppIds.contains(id)) {
          deletedIds.add(id);
        }
      }
    }

    return deletedIds;
  }
}
