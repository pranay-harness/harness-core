package io.harness.migrations;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.persistence.HIterator;

import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisioner.InfrastructureProvisionerKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
@OwnedBy(CDP)
public class TerraformProvisionerBranchMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingService;

  @Override
  public void migrate() {
    try (HIterator<InfrastructureProvisioner> iterator =
             new HIterator<>(wingsPersistence.createQuery(InfrastructureProvisioner.class)
                                 .filter(InfrastructureProvisionerKeys.infrastructureProvisionerType, "TERRAFORM")
                                 .fetch())) {
      while (iterator.hasNext()) {
        TerraformInfrastructureProvisioner provisioner = (TerraformInfrastructureProvisioner) iterator.next();
        if (StringUtils.isEmpty(provisioner.getSourceRepoBranch())) {
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
}
