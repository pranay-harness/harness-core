package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.migrations.Migration;

import software.wings.beans.security.HarnessUserGroup;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Re-saving Harness user groups to remove deprecated fields 'applyToAllFields' and 'actions'
 */
@Slf4j
public class RemoveDeprecatedFieldsFromHarnessUserGroup implements Migration {
  @Inject private WingsPersistence persistence;

  @Override
  public void migrate() {
    try {
      log.info("Removing deprecated fields from Harness user groups.");

      List<HarnessUserGroup> harnessUserGroups =
          persistence.createQuery(HarnessUserGroup.class, excludeAuthority).asList();
      harnessUserGroups.forEach(group -> persistence.save(group));

    } catch (Exception ex) {
      log.error("Error while removing deprecated fields from Harness user groups.", ex);
    }
  }
}
