package software.wings.service.impl.applicationmanifest;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;

import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.logcontext.SettingAttributeLogContext;
import software.wings.service.impl.SettingAttributeObserver;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.applicationmanifest.HelmChartService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class AppManifestSettingAttributePTaskManager implements SettingAttributeObserver {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private AppManifestPTaskHelper appManifestPTaskHelper;
  @Inject private HelmChartService helmChartService;

  @Override
  public void onSaved(SettingAttribute settingAttribute) {
    // Nothing to do on save as no application manifests are linked yet
  }

  @Override
  public void onUpdated(SettingAttribute prevSettingAttribute, SettingAttribute currSettingAttribute) {
    if (!featureFlagService.isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, currSettingAttribute.getAccountId())) {
      return;
    }
    try (AutoLogContext ignore1 = new AccountLogContext(currSettingAttribute.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new SettingAttributeLogContext(currSettingAttribute.getUuid(), OVERRIDE_ERROR)) {
      List<ApplicationManifest> applicationManifests = applicationManifestService.listHelmChartSourceBySettingId(
          currSettingAttribute.getAccountId(), currSettingAttribute.getUuid());

      applicationManifests.forEach(applicationManifest -> {
        helmChartService.deleteByAppManifest(applicationManifest.getAppId(), applicationManifest.getUuid());
        if (applicationManifest.getPerpetualTaskId() != null) {
          appManifestPTaskHelper.resetPerpetualTask(applicationManifest);
        }
      });
    }
  }

  @Override
  public void onDeleted(SettingAttribute settingAttribute) {
    // Nothing to do on delete as error message is thrown if app manifests are being referenced
  }
}
