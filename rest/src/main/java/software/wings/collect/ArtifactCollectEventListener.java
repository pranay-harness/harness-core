package software.wings.collect;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Event.Builder.anEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.ErrorCodes;
import software.wings.beans.Event.Type;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactPathServiceEntry;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.common.UUIDGenerator;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.exception.WingsException;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

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

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(CollectEvent message) throws Exception {
    Artifact artifact = message.getArtifact();
    try {
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.RUNNING);
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());

      ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
      String accountId = appService.get(artifactStream.getAppId()).getAccountId();
      String waitId = UUIDGenerator.getUuid();

      DelegateTask delegateTask = createDelegateTask(accountId, artifactStream, artifact, waitId);
      waitNotifyEngine.waitForAll(new ArtifactCollectionCallback(artifact.getAppId(), artifact.getUuid()), waitId);
      delegateService.sendTaskWaitNotify(delegateTask);

    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      artifactService.updateStatus(artifact.getUuid(), artifact.getAppId(), Status.FAILED);
      eventEmitter.send(Channel.ARTIFACTS,
          anEvent().withType(Type.UPDATE).withUuid(artifact.getUuid()).withAppId(artifact.getAppId()).build());
    }
  }

  private DelegateTask createDelegateTask(
      String accountId, ArtifactStream artifactStream, Artifact artifact, String waitId) {
    switch (artifactStream.getSourceType()) {
      case JENKINS: {
        SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
        JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();
        List<String> artifactPaths = artifactStream.getArtifactPathServices()
                                         .stream()
                                         .map(ArtifactPathServiceEntry::getArtifactPathRegex)
                                         .collect(toList());

        return aDelegateTask()
            .withTaskType(TaskType.JENKINS_COLLECTION)
            .withAccountId(accountId)
            .withAppId(artifactStream.getAppId())
            .withWaitId(waitId)
            .withParameters(new Object[] {jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(),
                jenkinsConfig.getPassword(), artifactStream.getJobname(), artifactPaths, artifact.getMetadata()})
            .build();
      }
      case BAMBOO: {
        SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
        BambooConfig bambooConfig = (BambooConfig) settingAttribute.getValue();
        List<String> artifactPaths = artifactStream.getArtifactPathServices()
                                         .stream()
                                         .map(ArtifactPathServiceEntry::getArtifactPathRegex)
                                         .collect(toList());

        return aDelegateTask()
            .withTaskType(TaskType.BAMBOO_COLLECTION)
            .withAccountId(accountId)
            .withAppId(artifactStream.getAppId())
            .withWaitId(waitId)
            .withParameters(new Object[] {bambooConfig.getBambooUrl(), bambooConfig.getUsername(),
                bambooConfig.getPassword(), artifactStream.getJobname(), artifactPaths, artifact.getMetadata()})
            .build();
      }

      default: { throw new WingsException(ErrorCodes.UNKNOWN_ARTIFACT_TYPE); }
    }
  }
}
