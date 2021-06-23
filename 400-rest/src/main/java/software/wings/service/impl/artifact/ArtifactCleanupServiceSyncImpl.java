package software.wings.service.impl.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;

import io.harness.ff.FeatureFlagService;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.delegatetasks.buildsource.BuildSourceCleanupHelper;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.ArtifactCleanupService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ArtifactCleanupServiceSyncImpl implements ArtifactCleanupService {
  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private AwsCommandHelper awsCommandHelper;
  @Inject private BuildSourceService buildSourceService;
  @Inject private AuthService authService;
  @Inject private transient ArtifactService artifactService;
  @Inject private FeatureFlagService featureFlagService;

  public static final Duration timeout = Duration.ofMinutes(10);

  @Override
  public void cleanupArtifacts(ArtifactStream artifactStream) {
    String artifactStreamType = artifactStream.getArtifactStreamType();
    String accountId;

    if (CUSTOM.name().equals(artifactStreamType)) {
      ArtifactStreamAttributes artifactStreamAttributes =
          artifactCollectionUtils.renderCustomArtifactScriptString((CustomArtifactStream) artifactStream);
      accountId = artifactStreamAttributes.getAccountId();
    } else {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        log.warn("Artifact Server {} was deleted", artifactStream.getSettingId());
        // TODO:: mark inactive maybe
        return;
      }
      accountId = settingAttribute.getAccountId();
    }

    final User user = UserThreadLocal.get();
    if (user != null) {
      authService.authorize(accountId, artifactStream.getAppId(), artifactStream.getServiceId(), user,
          Arrays.asList(new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
                            PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.READ),
              new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
                  PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.CREATE),
              new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
                  PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.UPDATE)));
    }

    log.info("Cleaning build details for artifact stream type {} and source name {} ",
        artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
    List<BuildDetails> builds = buildSourceService.getBuilds(
        artifactStream.getAppId(), artifactStream.getUuid(), artifactStream.getSettingId());

    BuildSourceCleanupHelper buildSourceCleanupHelper = new BuildSourceCleanupHelper(
        accountId, artifactStream, builds, artifactService, featureFlagService, artifactCollectionUtils);
    buildSourceCleanupHelper.cleanupArtifacts();
  }
}
