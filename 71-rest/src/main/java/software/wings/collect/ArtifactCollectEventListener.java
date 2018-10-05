package software.wings.collect;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Event.Builder.anEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.Event.Type;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Misc;
import software.wings.waitnotify.WaitNotifyEngine;

/**
 * Created by peeyushaggarwal on 5/11/16.
 *
 * @see CollectEvent
 */
@Singleton
public class ArtifactCollectEventListener extends AbstractQueueListener<CollectEvent> {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectEventListener.class);

  @Inject private AppService appService;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private SettingsService settingsService;
  @Inject private DelegateService delegateService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private EventEmitter eventEmitter;
  @Inject private SecretManager secretManager;

  public ArtifactCollectEventListener() {
    super(true);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(CollectEvent message) {
    Artifact artifact = message.getArtifact();
    try {
      logger.info("Received artifact collection event for artifactId {} and of appId {}", artifact.getUuid(),
          artifact.getAppId());
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.RUNNING, ContentStatus.DOWNLOADING);
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());

      ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
      String accountId = appService.get(artifactStream.getAppId()).getAccountId();
      String waitId = generateUuid();

      DelegateTask delegateTask = createDelegateTask(accountId, artifactStream, artifact, waitId);
      logger.info("Registering callback for the artifact artifactId {} with waitId {}", artifact.getUuid(), waitId);
      waitNotifyEngine.waitForAll(new ArtifactCollectionCallback(artifact.getAppId(), artifact.getUuid()), waitId);
      logger.info("Queuing delegate task {} for artifactId {} of arifactSourceName {} ", delegateTask.getUuid(),
          artifact.getUuid(), artifact.getArtifactSourceName());
      delegateService.queueTask(delegateTask);

    } catch (Exception ex) {
      logger.error(format("Failed to collect artifact. Reason %s", Misc.getMessage(ex)), ex);
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.FAILED, ContentStatus.FAILED);
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());
    }
  }

  private DelegateTask createDelegateTask(
      String accountId, ArtifactStream artifactStream, Artifact artifact, String waitId) {
    switch (ArtifactStreamType.valueOf(artifactStream.getArtifactStreamType())) {
      case JENKINS: {
        JenkinsArtifactStream jenkinsArtifactStream = (JenkinsArtifactStream) artifactStream;
        SettingAttribute settingAttribute = settingsService.get(jenkinsArtifactStream.getSettingId());
        JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();

        return aDelegateTask()
            .withTaskType(TaskType.JENKINS_COLLECTION)
            .withAccountId(accountId)
            .withAppId(jenkinsArtifactStream.getAppId())
            .withWaitId(waitId)
            .withParameters(new Object[] {jenkinsConfig, secretManager.getEncryptionDetails(jenkinsConfig, null, null),
                jenkinsArtifactStream.getJobname(), jenkinsArtifactStream.getArtifactPaths(), artifact.getMetadata()})
            .build();
      }
      case BAMBOO: {
        BambooArtifactStream bambooArtifactStream = (BambooArtifactStream) artifactStream;
        SettingAttribute settingAttribute = settingsService.get(bambooArtifactStream.getSettingId());
        BambooConfig bambooConfig = (BambooConfig) settingAttribute.getValue();

        return aDelegateTask()
            .withTaskType(TaskType.BAMBOO_COLLECTION)
            .withAccountId(accountId)
            .withAppId(bambooArtifactStream.getAppId())
            .withWaitId(waitId)
            .withParameters(new Object[] {bambooConfig, secretManager.getEncryptionDetails(bambooConfig, null, null),
                bambooArtifactStream.getJobname(), bambooArtifactStream.getArtifactPaths(), artifact.getMetadata()})
            .build();
      }
      case NEXUS: {
        NexusArtifactStream nexusArtifactStream = (NexusArtifactStream) artifactStream;
        SettingAttribute settingAttribute = settingsService.get(nexusArtifactStream.getSettingId());
        NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();

        return aDelegateTask()
            .withTaskType(TaskType.NEXUS_COLLECTION)
            .withAccountId(accountId)
            .withAppId(nexusArtifactStream.getAppId())
            .withWaitId(waitId)
            .withParameters(new Object[] {nexusConfig, secretManager.getEncryptionDetails(nexusConfig, null, null),
                nexusArtifactStream.getJobname(), nexusArtifactStream.getGroupId(),
                nexusArtifactStream.getArtifactPaths(), artifact.getBuildNo()})
            .build();
      }
      case ARTIFACTORY: {
        ArtifactoryArtifactStream artifactoryArtifactStream = (ArtifactoryArtifactStream) artifactStream;
        SettingAttribute settingAttribute = settingsService.get(artifactoryArtifactStream.getSettingId());
        ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingAttribute.getValue();

        return aDelegateTask()
            .withTaskType(TaskType.ARTIFACTORY_COLLECTION)
            .withAccountId(accountId)
            .withAppId(artifactoryArtifactStream.getAppId())
            .withWaitId(waitId)
            .withParameters(
                new Object[] {artifactoryConfig, secretManager.getEncryptionDetails(artifactoryConfig, null, null),
                    artifactoryArtifactStream.getJobname(), artifact.getMetadata()})
            .build();
      }
      case AMAZON_S3: {
        AmazonS3ArtifactStream amazonS3ArtifactStream = (AmazonS3ArtifactStream) artifactStream;
        SettingAttribute settingAttribute = settingsService.get(amazonS3ArtifactStream.getSettingId());
        AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();

        return aDelegateTask()
            .withTaskType(TaskType.AMAZON_S3_COLLECTION)
            .withAccountId(accountId)
            .withAppId(amazonS3ArtifactStream.getAppId())
            .withWaitId(waitId)
            .withParameters(new Object[] {awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null),
                amazonS3ArtifactStream.getJobname(), amazonS3ArtifactStream.getArtifactPaths()})
            .build();
      }

      default: { throw new WingsException(ErrorCode.UNKNOWN_ARTIFACT_TYPE); }
    }
  }
}
