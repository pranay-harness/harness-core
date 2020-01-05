package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by anubhaw on 7/19/18.
 */
@Slf4j
public class BuildSourceTask extends AbstractDelegateRunnableTask {
  @Inject private Map<Class<? extends SettingValue>, Class<? extends BuildService>> buildServiceMap;
  @Inject private ServiceClassLocator serviceLocator;
  @Inject private Injector injector;

  public BuildSourceTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public BuildSourceExecutionResponse run(TaskParameters parameters) {
    try {
      BuildSourceParameters buildSourceRequest = (BuildSourceParameters) parameters;
      int limit = buildSourceRequest.getLimit();
      String artifactStreamType = buildSourceRequest.getArtifactStreamType();
      SettingValue settingValue = buildSourceRequest.getSettingValue();
      BuildService service = getBuildService(artifactStreamType);
      ArtifactStreamAttributes artifactStreamAttributes = buildSourceRequest.getArtifactStreamAttributes();
      artifactStreamAttributes.setCollection(buildSourceRequest.isCollection());
      if (isNotEmpty(buildSourceRequest.getSavedBuildDetailsKeys())) {
        artifactStreamAttributes.setSavedBuildDetailsKeys(buildSourceRequest.getSavedBuildDetailsKeys());
      }
      String appId = buildSourceRequest.getAppId();
      List<EncryptedDataDetail> encryptedDataDetails = buildSourceRequest.getEncryptedDataDetails();
      BuildSourceRequestType buildSourceRequestType = buildSourceRequest.getBuildSourceRequestType();

      List<BuildDetails> buildDetails = new ArrayList<>();
      if (buildSourceRequestType.equals(BuildSourceRequestType.GET_BUILDS)) {
        if (ArtifactStreamType.CUSTOM.name().equals(artifactStreamType)) {
          buildDetails = service.getBuilds(artifactStreamAttributes);
        } else {
          boolean enforceLimitOnResults =
              limit != -1 && ARTIFACTORY.name().equals(artifactStreamType); // TODO: supported for Artifactory only
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

      // NOTE: Here BuildSourceExecutionResponse::buildSourceResponse::stable is marked always true. When artifact
      // streams are capable of pagination we'll need to get that from the getBuilds function above.
      return BuildSourceExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .buildSourceResponse(BuildSourceResponse.builder().buildDetails(buildDetails).stable(true).build())
          .build();
    } catch (Exception ex) {
      logger.error("Exception in processing BuildSource task [{}]", ex);
      return BuildSourceExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }

  private BuildService getBuildService(String artifactStreamType) {
    Class<? extends BuildService> buildServiceClass = serviceLocator.getBuildServiceClass(artifactStreamType);
    return injector.getInstance(Key.get(buildServiceClass));
  }
}
