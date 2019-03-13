package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
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

import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.TaskData.TaskDataBuilder;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.lock.PersistentLocker;
import io.harness.waiter.WaitNotifyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.DelegateTaskBuilder;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.delegatetasks.buildsource.BuildSourceCallback;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.delegatetasks.buildsource.BuildSourceParameters.BuildSourceRequestType;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.utils.ArtifactType;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/***
 * Service responsible to glue all artifact
 */
@Singleton
public class ArtifactCollectionServiceAsyncImpl implements ArtifactCollectionService {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionServiceAsyncImpl.class);

  @Inject private ServiceResourceService serviceResourceService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private BuildSourceService buildSourceService;
  @Inject private ArtifactService artifactService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private ArtifactCollectionUtil artifactCollectionUtil;
  @Inject private AwsCommandHelper awsCommandHelper;

  public static final Duration timeout = Duration.ofMinutes(10);

  public static final List<String> metadataOnlyStreams =
      Collections.unmodifiableList(asList(DOCKER.name(), ECR.name(), GCR.name(), NEXUS.name(), AMI.name(), ACR.name(),
          AMAZON_S3.name(), GCS.name(), SMB.name(), SFTP.name(), CUSTOM.name()));

  private BuildSourceRequestType getRequestType(ArtifactStream artifactStream, ArtifactType artifactType) {
    String artifactStreamType = artifactStream.getArtifactStreamType();

    if (metadataOnlyStreams.contains(artifactStreamType) || isArtifactoryDockerOrGenric(artifactStream, artifactType)) {
      return BuildSourceRequestType.GET_BUILDS;
    } else {
      return BuildSourceRequestType.GET_LAST_SUCCESSFUL_BUILD;
    }
  }

  private boolean isArtifactoryDockerOrGenric(ArtifactStream artifactStream, ArtifactType artifactType) {
    return ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())
        && (ArtifactType.DOCKER.equals(artifactType)
               || !"maven".equals(artifactStream.fetchArtifactStreamAttributes().getRepositoryType()));
  }

  @Override
  public Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream == null) {
      throw new WingsException("Artifact Stream was deleted", USER);
    }
    return artifactService.create(artifactCollectionUtil.getArtifact(artifactStream, buildDetails));
  }

  @Override
  public void collectNewArtifactsAsync(String appId, ArtifactStream artifactStream, String permitId) {
    logger.info("Collecting build details for artifact stream id {} type {} and source name {} ",
        artifactStream.getUuid(), artifactStream.getArtifactStreamType(), artifactStream.getSourceName());

    String artifactStreamType = artifactStream.getArtifactStreamType();

    String accountId;
    BuildSourceParameters buildSourceRequest;

    String waitId = generateUuid();
    final TaskDataBuilder dataBuilder = TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT);
    DelegateTaskBuilder delegateTaskBuilder = DelegateTask.builder()
                                                  .async(true)
                                                  .taskType(TaskType.BUILD_SOURCE_TASK.name())
                                                  .appId(GLOBAL_APP_ID)
                                                  .waitId(waitId);

    if (CUSTOM.name().equals(artifactStreamType)) {
      // Defaulting to the 60 secs
      ArtifactStreamAttributes artifactStreamAttributes =
          artifactCollectionUtil.renderCustomArtifactScriptString((CustomArtifactStream) artifactStream);

      long timeout = isEmpty(artifactStreamAttributes.getCustomScriptTimeout())
          ? Long.parseLong(CustomArtifactStream.DEFAULT_SCRIPT_TIME_OUT)
          : Long.parseLong(artifactStreamAttributes.getCustomScriptTimeout());

      accountId = artifactStreamAttributes.getAccountId();
      BuildSourceRequestType requestType = BuildSourceRequestType.GET_BUILDS;

      buildSourceRequest = BuildSourceParameters.builder()
                               .accountId(artifactStreamAttributes.getAccountId())
                               .appId(appId)
                               .artifactStreamAttributes(artifactStreamAttributes)
                               .artifactStreamType(artifactStreamType)
                               .buildSourceRequestType(requestType)
                               .limit(getLimit(artifactStream.getArtifactStreamType(), requestType))
                               .build();

      List<String> tags = ((CustomArtifactStream) artifactStream).getTags();
      if (isNotEmpty(tags)) {
        // To remove if any empty tags in case saved for custom artifact stream
        tags = tags.stream().filter(s -> isNotEmpty(s)).distinct().collect(Collectors.toList());
        delegateTaskBuilder.tags(tags);
      }

      delegateTaskBuilder.accountId(accountId);
      delegateTaskBuilder.tags(tags);
      dataBuilder.parameters(new Object[] {buildSourceRequest}).timeout(timeout);

    } else {
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        logger.warn("Artifact Server {} was deleted of artifactStreamId {}", artifactStream.getSettingId(),
            artifactStream.getUuid());
        // TODO:: mark inactive maybe
        artifactStreamService.updateFailedCronAttempts(
            artifactStream.getAppId(), artifactStream.getUuid(), artifactStream.getFailedCronAttempts() + 1);
        return;
      }
      accountId = settingAttribute.getAccountId();
      SettingValue settingValue = settingAttribute.getValue();

      List<EncryptedDataDetail> encryptedDataDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingValue, null, null);

      Service service = getService(appId, artifactStream);

      ArtifactStreamAttributes artifactStreamAttributes = getArtifactStreamAttributes(artifactStream, service);

      BuildSourceRequestType requestType = getRequestType(artifactStream, service.getArtifactType());

      buildSourceRequest = BuildSourceParameters.builder()
                               .accountId(settingAttribute.getAccountId())
                               .appId(service.getAppId())
                               .artifactStreamAttributes(artifactStreamAttributes)
                               .artifactStreamType(artifactStreamType)
                               .settingValue(settingValue)
                               .encryptedDataDetails(encryptedDataDetails)
                               .buildSourceRequestType(requestType)
                               .limit(getLimit(artifactStream.getArtifactStreamType(), requestType))
                               .build();

      delegateTaskBuilder.accountId(accountId);
      dataBuilder.parameters(new Object[] {buildSourceRequest}).timeout(TimeUnit.MINUTES.toMillis(1));
      delegateTaskBuilder.tags(awsCommandHelper.getAwsConfigTagsFromSettingAttribute(settingAttribute));
    }

    delegateTaskBuilder.data(dataBuilder.build());

    waitNotifyEngine.waitForAll(new BuildSourceCallback(accountId, appId, artifactStream.getUuid(), permitId), waitId);
    logger.info("Queuing delegate task for artifactStreamId {} with waitId {}", artifactStream.getUuid(), waitId);
    final String taskId = delegateService.queueTask(delegateTaskBuilder.build());
    logger.info("Queued delegate taskId {} for artifactStreamId {}", taskId, artifactStream.getUuid());
  }

  private int getLimit(String artifactStreamType, BuildSourceRequestType requestType) {
    return ARTIFACTORY.name().equals(artifactStreamType) && BuildSourceRequestType.GET_BUILDS.equals(requestType) ? 25
                                                                                                                  : -1;
  }

  @Override
  public Artifact collectNewArtifacts(String appId, ArtifactStream artifactStream, String buildNumber) {
    List<BuildDetails> builds =
        buildSourceService.getBuilds(appId, artifactStream.getUuid(), artifactStream.getSettingId());
    if (isNotEmpty(builds)) {
      Optional<BuildDetails> buildDetails =
          builds.stream().filter(build -> buildNumber.equals(build.getNumber())).findFirst();
      if (buildDetails.isPresent()) {
        return collectArtifact(appId, artifactStream.getUuid(), buildDetails.get());
      }
    }
    return null;
  }

  @Override
  public List<Artifact> collectNewArtifacts(String appId, String artifactStreamId) {
    throw new InvalidRequestException("Method not supported");
  }

  private ArtifactStreamAttributes getArtifactStreamAttributes(ArtifactStream artifactStream, Service service) {
    ArtifactStreamAttributes artifactStreamAttributes = artifactStream.fetchArtifactStreamAttributes();
    artifactStreamAttributes.setArtifactType(service.getArtifactType());
    return artifactStreamAttributes;
  }

  private Service getService(String appId, ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    if (service == null) {
      artifactStreamService.delete(appId, artifactStream.getUuid());
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", format("Artifact stream %s is a zombie.", artifactStream.getUuid()));
    }
    return service;
  }
}
