package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;

/**
 * Created by Pranjal on 08/28/2019
 */
@Slf4j
public class DeleteStaleSlackConfigs implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Deleting stale Slack configs");
    wingsPersistence.delete(
        wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority).filter("value.type", "SLACK"));
  }
}
