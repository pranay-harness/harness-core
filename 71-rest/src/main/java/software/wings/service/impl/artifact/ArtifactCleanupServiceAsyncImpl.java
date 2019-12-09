package software.wings.service.impl.artifact;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataBuilder;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.delegatetasks.buildsource.BuildSourceCleanupCallback;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.service.intfc.ArtifactCleanupService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class ArtifactCleanupServiceAsyncImpl implements ArtifactCleanupService {
  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private AwsCommandHelper awsCommandHelper;

  public static final Duration timeout = Duration.ofMinutes(10);

  @Override
  public void cleanupArtifactsAsync(ArtifactStream artifactStream) {
    logger.info("Cleaning build details for artifact stream type {} and source name {} ",
        artifactStream.getArtifactStreamType(), artifactStream.getSourceName());

    String artifactStreamType = artifactStream.getArtifactStreamType();

    String accountId;
    BuildSourceParameters buildSourceRequest;

    String waitId = generateUuid();
    final TaskDataBuilder dataBuilder =
        TaskData.builder().taskType(TaskType.BUILD_SOURCE_TASK.name()).timeout(DEFAULT_ASYNC_CALL_TIMEOUT);
    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder().async(true).appId(GLOBAL_APP_ID).waitId(waitId);

    if (CUSTOM.name().equals(artifactStreamType)) {
      ArtifactStreamAttributes artifactStreamAttributes =
          artifactCollectionUtils.renderCustomArtifactScriptString((CustomArtifactStream) artifactStream);
      accountId = artifactStreamAttributes.getAccountId();
      delegateTaskBuilder =
          artifactCollectionUtils.fetchCustomDelegateTask(waitId, artifactStream, artifactStreamAttributes, false);
    } else {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        logger.warn("Artifact Server {} was deleted", artifactStream.getSettingId());
        // TODO:: mark inactive maybe
        return;
      }

      accountId = settingAttribute.getAccountId();
      buildSourceRequest =
          artifactCollectionUtils.getBuildSourceParameters(artifactStream, settingAttribute, false, false);
      delegateTaskBuilder.accountId(accountId);
      dataBuilder.parameters(new Object[] {buildSourceRequest}).timeout(TimeUnit.MINUTES.toMillis(1));
      delegateTaskBuilder.tags(awsCommandHelper.getAwsConfigTagsFromSettingAttribute(settingAttribute));
      delegateTaskBuilder.data(dataBuilder.build());
    }

    waitNotifyEngine.waitForAllOn(GENERAL, new BuildSourceCleanupCallback(accountId, artifactStream.getUuid()), waitId);
    logger.info("Queuing delegate task with waitId {}", waitId);
    final String taskId = delegateService.queueTask(delegateTaskBuilder.build());
    logger.info("Queued delegate taskId {}", taskId);
  }
}
