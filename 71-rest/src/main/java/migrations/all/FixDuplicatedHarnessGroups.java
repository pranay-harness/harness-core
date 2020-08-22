package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.dl.WingsPersistence;

import java.util.HashSet;
import java.util.Set;

/**
 * Create single Harness user group with all members, delete existing groups
 */

@Slf4j
public class FixDuplicatedHarnessGroups implements Migration {
  @Inject private WingsPersistence persistence;

  @Override
  public void migrate() {
    try {
      logger.error("Fixing duplicate Harness user groups issue.");

      Set<String> memberIds = new HashSet<>();

      try (HIterator<HarnessUserGroup> harnessUserGroupIterator =
               new HIterator<>(persistence.createQuery(HarnessUserGroup.class, excludeAuthority).fetch())) {
        while (harnessUserGroupIterator.hasNext()) {
          final HarnessUserGroup group = harnessUserGroupIterator.next();
          memberIds.addAll(group.getMemberIds());
        }
      }

      logger.info("Harness support users: " + memberIds.toString());

      Query<HarnessUserGroup> query = persistence.createQuery(HarnessUserGroup.class, excludeAuthority);
      persistence.delete(query);

      persistence.save(HarnessUserGroup.builder().memberIds(memberIds).name("readOnly").build());

    } catch (Exception e) {
      logger.error("Error while fixing duplicated Harness user groups", e);
    }
  }
}
