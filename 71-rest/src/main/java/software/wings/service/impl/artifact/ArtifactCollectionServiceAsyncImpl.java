package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.beans.artifact.ArtifactStreamType.SFTP;
import static software.wings.beans.artifact.ArtifactStreamType.SMB;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataBuilder;
import io.harness.exception.InvalidRequestException;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.delegatetasks.buildsource.BuildSourceCallback;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.delegatetasks.buildsource.BuildSourceParameters.BuildSourceRequestType;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.PermitService;
import software.wings.service.intfc.SettingsService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/***
 * Service responsible to glue all artifact
 */
@Singleton
@Slf4j
public class ArtifactCollectionServiceAsyncImpl implements ArtifactCollectionService {
  @Inject private BuildSourceService buildSourceService;
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private AwsCommandHelper awsCommandHelper;
  @Inject private PermitService permitService;
  @Inject private AlertService alertService;

  // Default timeout of 1 minutes.
  private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
  // With Labels timeout of 2 minutes.
  private static final long WITH_LABELS_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

  static final List<String> metadataOnlyStreams =
      Collections.unmodifiableList(asList(DOCKER.name(), ECR.name(), GCR.name(), NEXUS.name(), AMI.name(), ACR.name(),
          AMAZON_S3.name(), GCS.name(), SMB.name(), SFTP.name(), CUSTOM.name()));

  @Override
  public Artifact collectArtifact(String artifactStreamId, BuildDetails buildDetails) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact stream was deleted", USER);
    }
    if (DOCKER.name().equals(artifactStream.getArtifactStreamType())) {
      List<Map<String, String>> labelsMap =
          buildSourceService.getLabels(artifactStream, Collections.singletonList(buildDetails.getNumber()));
      if (isNotEmpty(labelsMap) && isNotEmpty(labelsMap.get(0))) {
        buildDetails.setLabels(labelsMap.get(0));
      }
    }
    return collectArtifactWithoutLabels(artifactStream, buildDetails);
  }

  private Artifact collectArtifactWithoutLabels(String artifactStreamId, BuildDetails buildDetails) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream == null) {
      throw new InvalidRequestException("Artifact stream was deleted", USER);
    }
    return collectArtifactWithoutLabels(artifactStream, buildDetails);
  }

  private Artifact collectArtifactWithoutLabels(ArtifactStream artifactStream, BuildDetails buildDetails) {
    final Artifact savedArtifact =
        artifactService.create(artifactCollectionUtils.getArtifact(artifactStream, buildDetails));
    if (artifactStream.getFailedCronAttempts() != 0) {
      artifactStreamService.updateFailedCronAttempts(
          artifactStream.getAccountId(), savedArtifact.getArtifactStreamId(), 0);
      permitService.releasePermitByKey(artifactStream.getUuid());
      alertService.closeAlert(artifactStream.getAccountId(), null, AlertType.ARTIFACT_COLLECTION_FAILED,
          ArtifactCollectionFailedAlert.builder().artifactStreamId(artifactStream.getUuid()).build());
    }
    return savedArtifact;
  }

  @Override
  public void collectNewArtifactsAsync(ArtifactStream artifactStream, String permitId) {
    logger.info("Collecting build details for artifact stream id {} type {} and source name {} ",
        artifactStream.getUuid(), artifactStream.getArtifactStreamType(), artifactStream.getSourceName());

    String artifactStreamType = artifactStream.getArtifactStreamType();

    String accountId;
    BuildSourceParameters buildSourceRequest;

    String waitId = generateUuid();
    final TaskDataBuilder dataBuilder = TaskData.builder().taskType(TaskType.BUILD_SOURCE_TASK.name());
    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder().async(true).appId(GLOBAL_APP_ID).waitId(waitId);

    if (CUSTOM.name().equals(artifactStreamType)) {
      ArtifactStreamAttributes artifactStreamAttributes =
          artifactCollectionUtils.renderCustomArtifactScriptString((CustomArtifactStream) artifactStream);
      accountId = artifactStreamAttributes.getAccountId();
      BuildSourceRequestType requestType = BuildSourceRequestType.GET_BUILDS;

      buildSourceRequest =
          BuildSourceParameters.builder()
              .accountId(artifactStreamAttributes.getAccountId())
              .appId(artifactStream.fetchAppId())
              .artifactStreamAttributes(artifactStreamAttributes)
              .artifactStreamType(artifactStreamType)
              .buildSourceRequestType(requestType)
              .limit(ArtifactCollectionUtils.getLimit(artifactStream.getArtifactStreamType(), requestType))
              .isCollection(true)
              .savedBuildDetailsKeys(artifactCollectionUtils.getArtifactsKeys(artifactStream, artifactStreamAttributes))
              .build();

      List<String> tags = ((CustomArtifactStream) artifactStream).getTags();
      if (isNotEmpty(tags)) {
        // To remove if any empty tags in case saved for custom artifact stream
        tags = tags.stream().filter(EmptyPredicate::isNotEmpty).distinct().collect(toList());
      }

      // Set timeout. Labels are not fetched for CUSTOM artifact streams.
      long timeout = isEmpty(artifactStreamAttributes.getCustomScriptTimeout())
          ? Long.parseLong(CustomArtifactStream.DEFAULT_SCRIPT_TIME_OUT)
          : Long.parseLong(artifactStreamAttributes.getCustomScriptTimeout());
      dataBuilder.parameters(new Object[] {buildSourceRequest}).timeout(timeout);
      delegateTaskBuilder.tags(tags);
    } else {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        logger.warn("Artifact Server {} was deleted of artifactStreamId {}", artifactStream.getSettingId(),
            artifactStream.getUuid());
        // TODO:: mark inactive maybe
        int failedCronAttempts = artifactStream.getFailedCronAttempts() + 1;
        artifactStreamService.updateFailedCronAttempts(
            artifactStream.getAccountId(), artifactStream.getUuid(), failedCronAttempts);
        if (PermitServiceImpl.shouldSendAlert(failedCronAttempts)) {
          String appId = artifactStream.fetchAppId();
          if (!GLOBAL_APP_ID.equals(appId)) {
            alertService.openAlert(artifactStream.getAccountId(), null, AlertType.ARTIFACT_COLLECTION_FAILED,
                ArtifactCollectionFailedAlert.builder()
                    .appId(appId)
                    .serviceId(artifactStream.getServiceId())
                    .artifactStreamId(artifactStream.getUuid())
                    .build());
          } else {
            alertService.openAlert(artifactStream.getAccountId(), null, AlertType.ARTIFACT_COLLECTION_FAILED,
                ArtifactCollectionFailedAlert.builder()
                    .settingId(artifactStream.getSettingId())
                    .artifactStreamId(artifactStream.getUuid())
                    .build());
          }
        }
        return;
      }

      accountId = settingAttribute.getAccountId();
      buildSourceRequest =
          artifactCollectionUtils.getBuildSourceParameters(artifactStream, settingAttribute, true, true);

      // Set timeout.
      long timeout = DEFAULT_TIMEOUT;
      if (DOCKER.name().equals(artifactStreamType)) {
        // Update timeout if collecting labels.
        timeout = WITH_LABELS_TIMEOUT;
      }
      dataBuilder.parameters(new Object[] {buildSourceRequest}).timeout(timeout);
      delegateTaskBuilder.tags(awsCommandHelper.getAwsConfigTagsFromSettingAttribute(settingAttribute));
    }

    delegateTaskBuilder.accountId(accountId);
    delegateTaskBuilder.data(dataBuilder.build());

    waitNotifyEngine.waitForAll(
        new BuildSourceCallback(accountId, artifactStream.getUuid(), permitId, artifactStream.getSettingId()), waitId);
    logger.info("Queuing delegate task for artifactStreamId {} with waitId {}", artifactStream.getUuid(), waitId);
    final String taskId = delegateService.queueTask(delegateTaskBuilder.build());
    logger.info("Queued delegate taskId {} for artifactStreamId {}", taskId, artifactStream.getUuid());
  }

  @Override
  public Artifact collectNewArtifacts(String appId, ArtifactStream artifactStream, String buildNumber) {
    List<BuildDetails> builds =
        buildSourceService.getBuilds(appId, artifactStream.getUuid(), artifactStream.getSettingId());
    if (isNotEmpty(builds)) {
      Optional<BuildDetails> buildDetails =
          builds.stream().filter(build -> buildNumber.equals(build.getNumber())).findFirst();
      if (buildDetails.isPresent()) {
        return collectArtifactWithoutLabels(artifactStream.getUuid(), buildDetails.get());
      }
    }
    return null;
  }

  @Override
  public List<Artifact> collectNewArtifacts(String appId, String artifactStreamId) {
    throw new InvalidRequestException("Method not supported");
  }
}
