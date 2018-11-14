package migrations;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;

public class TerraformProvisionerBranchMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(TerraformProvisionerBranchMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingService;

  @Override
  public void migrate() {
    try (HIterator<TerraformInfrastructureProvisioner> iterator = new HIterator<TerraformInfrastructureProvisioner>(
             wingsPersistence.createQuery(TerraformInfrastructureProvisioner.class)
                 .filter("sourceRepoBranch", null)
                 .fetch())) {
      while (iterator.hasNext()) {
        TerraformInfrastructureProvisioner provisioner = iterator.next();

        SettingAttribute settingAttribute = settingService.get(provisioner.getSourceRepoSettingId());
        if (settingAttribute != null) {
          GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

          provisioner.setSourceRepoBranch(gitConfig.getBranch());
          wingsPersistence.save(provisioner);
        }
      }
    }
  }
}
