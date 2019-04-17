package software.wings.service.impl.artifact;

import static io.harness.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataBuilder;
import io.harness.waiter.WaitNotifyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.delegatetasks.buildsource.BuildSourceCleanupCallback;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.service.intfc.ArtifactCleanupService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Singleton
public class ArtifactCleanupServiceAsyncImpl implements ArtifactCleanupService {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCleanupServiceAsyncImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private ArtifactCollectionUtil artifactCollectionUtil;
  @Inject private AwsCommandHelper awsCommandHelper;

  public static final Duration timeout = Duration.ofMinutes(10);

  @Override
  public void cleanupArtifactsAsync(String appId, ArtifactStream artifactStream) {
    logger.info("Collecting build details for artifact stream id {} type {} and source name {} ",
        artifactStream.getUuid(), artifactStream.getArtifactStreamType(), artifactStream.getSourceName());

    String artifactStreamType = artifactStream.getArtifactStreamType();

    String accountId;
    BuildSourceParameters buildSourceRequest;

    String waitId = generateUuid();
    final TaskDataBuilder dataBuilder =
        TaskData.builder().taskType(TaskType.BUILD_SOURCE_TASK.name()).timeout(DEFAULT_ASYNC_CALL_TIMEOUT);
    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder().async(true).appId(GLOBAL_APP_ID).waitId(waitId);

    if (DOCKER.name().equals(artifactStreamType)) {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        logger.warn("Artifact Server {} was deleted of artifactStreamId {}", artifactStream.getSettingId(),
            artifactStream.getUuid());
        // TODO:: mark inactive maybe
        return;
      }
      accountId = settingAttribute.getAccountId();
      buildSourceRequest = artifactCollectionUtil.getBuildSourceParameters(appId, artifactStream, settingAttribute);
      delegateTaskBuilder.accountId(accountId);
      dataBuilder.parameters(new Object[] {buildSourceRequest}).timeout(TimeUnit.MINUTES.toMillis(1));
      delegateTaskBuilder.tags(awsCommandHelper.getAwsConfigTagsFromSettingAttribute(settingAttribute));
    } else {
      // TODO: add handling for other artifact stream types
      return;
    }

    delegateTaskBuilder.data(dataBuilder.build());

    waitNotifyEngine.waitForAll(new BuildSourceCleanupCallback(accountId, appId, artifactStream.getUuid()), waitId);
    logger.info("Queuing delegate task for artifactStreamId {} with waitId {}", artifactStream.getUuid(), waitId);
    final String taskId = delegateService.queueTask(delegateTaskBuilder.build());
    logger.info("Queued delegate taskId {} for artifactStreamId {}", taskId, artifactStream.getUuid());
  }
}
