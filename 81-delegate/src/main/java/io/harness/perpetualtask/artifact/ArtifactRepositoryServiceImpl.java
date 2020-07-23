package io.harness.perpetualtask.artifact;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static software.wings.delegatetasks.buildsource.BuildSourceParameters.BuildSourceRequestType.GET_BUILDS;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.delegatetasks.buildsource.BuildSourceParameters.BuildSourceRequestType;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.intfc.BuildService;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class ArtifactRepositoryServiceImpl {
  private ServiceClassLocator serviceLocator;
  private Injector injector;

  @Inject
  public ArtifactRepositoryServiceImpl(ServiceClassLocator serviceLocator, Injector injector) {
    this.serviceLocator = serviceLocator;
    this.injector = injector;
  }

  public BuildSourceExecutionResponse publishCollectedArtifacts(BuildSourceParameters buildSourceParameters) {
    BuildSourceExecutionResponse buildSourceExecutionResponse;
    try {
      buildSourceExecutionResponse =
          BuildSourceExecutionResponse.builder()
              .commandExecutionStatus(SUCCESS)
              .artifactStreamId(buildSourceParameters.getArtifactStreamId())
              .buildSourceResponse(
                  BuildSourceResponse.builder().buildDetails(collectBuilds(buildSourceParameters)).stable(true).build())
              .build();

    } catch (Exception ex) {
      logger.error("Exception in processing BuildSource task [{}]", ex);
      buildSourceExecutionResponse = BuildSourceExecutionResponse.builder()
                                         .artifactStreamId(buildSourceParameters.getArtifactStreamId())
                                         .commandExecutionStatus(FAILURE)
                                         .errorMessage(ExceptionUtils.getMessage(ex))
                                         .build();
    }
    return buildSourceExecutionResponse;
  }

  public List<BuildDetails> collectBuilds(BuildSourceParameters buildSourceParameters) {
    ArtifactStreamAttributes artifactStreamAttributes = buildSourceParameters.getArtifactStreamAttributes();
    int limit = buildSourceParameters.getLimit();
    String artifactStreamType = buildSourceParameters.getArtifactStreamType();
    SettingValue settingValue = buildSourceParameters.getSettingValue();
    String appId = buildSourceParameters.getAppId();

    // NOTE: Here we are only setting the collection field and not the savedBuildDetailsKeys. The management of whether
    // some keys need to be added/deleted is now done by ArtifactsPublishedCache so we don't need to pass
    // savedBuildDetailsKeys. Doing the same thing at 2 different places can result in bugs.
    artifactStreamAttributes.setCollection(buildSourceParameters.isCollection());

    List<EncryptedDataDetail> encryptedDataDetails = buildSourceParameters.getEncryptedDataDetails();
    BuildSourceRequestType buildSourceRequestType = buildSourceParameters.getBuildSourceRequestType();

    BuildService service = getBuildService(artifactStreamType);

    List<BuildDetails> buildDetails = new ArrayList<>();
    if (buildSourceRequestType == GET_BUILDS) {
      if (ArtifactStreamType.CUSTOM.name().equals(artifactStreamType)) {
        buildDetails = service.getBuilds(artifactStreamAttributes);
      } else {
        boolean enforceLimitOnResults = limit != -1 && "ARTIFACTORY".equals(artifactStreamType);
        buildDetails = enforceLimitOnResults
            ? service.getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails, limit)
            : service.getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
      }
    } else {
      BuildDetails lastSuccessfulBuild =
          service.getLastSuccessfulBuild(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
      if (lastSuccessfulBuild != null) {
        buildDetails.add(lastSuccessfulBuild);
      }
    }
    return buildDetails;
  }

  private BuildService getBuildService(String artifactStreamType) {
    Class<? extends BuildService> buildServiceClass = serviceLocator.getBuildServiceClass(artifactStreamType);
    return injector.getInstance(Key.get(buildServiceClass));
  }
}
