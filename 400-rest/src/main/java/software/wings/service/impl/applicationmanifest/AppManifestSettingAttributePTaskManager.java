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
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.logcontext.SettingAttributeLogContext;
import software.wings.service.impl.SettingAttributeObserver;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.applicationmanifest.HelmChartService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.EnumSet;
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

  private final EnumSet<SettingVariableTypes> helmSettingAttributeTypes =
      EnumSet.of(SettingVariableTypes.HTTP_HELM_REPO, SettingVariableTypes.AMAZON_S3_HELM_REPO,
          SettingVariableTypes.GCS_HELM_REPO);

  @Override
  public void onSaved(SettingAttribute settingAttribute) {
    // Nothing to do on save as no application manifests are linked yet
  }

  @Override
  public void onUpdated(SettingAttribute prevSettingAttribute, SettingAttribute currSettingAttribute) {
    if (currSettingAttribute.getValue() == null
        || !helmSettingAttributeTypes.contains(currSettingAttribute.getValue().getSettingType())) {
      return;
    }

    if (!featureFlagService.isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, currSettingAttribute.getAccountId())) {
      return;
    }

    if (!connectorValueChanged(prevSettingAttribute, currSettingAttribute)) {
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

  private boolean connectorValueChanged(SettingAttribute prevSettingAttribute, SettingAttribute currSettingAttribute) {
    if (currSettingAttribute.getValue() instanceof AmazonS3HelmRepoConfig) {
      AmazonS3HelmRepoConfig currAmazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) currSettingAttribute.getValue();
      AmazonS3HelmRepoConfig prevAmazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) prevSettingAttribute.getValue();
      return !prevAmazonS3HelmRepoConfig.getConnectorId().equals(currAmazonS3HelmRepoConfig.getConnectorId())
          || prevAmazonS3HelmRepoConfig.getBucketName().equals(currAmazonS3HelmRepoConfig.getBucketName())
          || !prevAmazonS3HelmRepoConfig.getRegion().equals(currAmazonS3HelmRepoConfig.getRegion());
    } else if (currSettingAttribute.getValue() instanceof GCSHelmRepoConfig) {
      GCSHelmRepoConfig currAmazonS3HelmRepoConfig = (GCSHelmRepoConfig) currSettingAttribute.getValue();
      GCSHelmRepoConfig prevAmazonS3HelmRepoConfig = (GCSHelmRepoConfig) prevSettingAttribute.getValue();
      return !prevAmazonS3HelmRepoConfig.getConnectorId().equals(currAmazonS3HelmRepoConfig.getConnectorId())
          || prevAmazonS3HelmRepoConfig.getBucketName().equals(currAmazonS3HelmRepoConfig.getBucketName());
    } else if (currSettingAttribute.getValue() instanceof HttpHelmRepoConfig) {
      HttpHelmRepoConfig currAmazonS3HelmRepoConfig = (HttpHelmRepoConfig) currSettingAttribute.getValue();
      HttpHelmRepoConfig prevAmazonS3HelmRepoConfig = (HttpHelmRepoConfig) prevSettingAttribute.getValue();
      return !prevAmazonS3HelmRepoConfig.getChartRepoUrl().equals(currAmazonS3HelmRepoConfig.getChartRepoUrl())
          || prevAmazonS3HelmRepoConfig.getUsername().equals(currAmazonS3HelmRepoConfig.getUsername())
          || !Arrays.equals(prevAmazonS3HelmRepoConfig.getPassword(), currAmazonS3HelmRepoConfig.getPassword());
    }
    return false;
  }

  @Override
  public void onDeleted(SettingAttribute settingAttribute) {
    // Nothing to do on delete as error message is thrown if app manifests are being referenced
  }
}
