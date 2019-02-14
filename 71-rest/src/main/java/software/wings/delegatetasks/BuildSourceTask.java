package software.wings.delegatetasks;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.delegate.task.protocol.TaskParameters;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceRequest;
import software.wings.delegatetasks.buildsource.BuildSourceRequest.BuildSourceRequestType;
import software.wings.delegatetasks.buildsource.BuildSourceResponse;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.intfc.BuildService;
import software.wings.settings.SettingValue;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by anubhaw on 7/19/18.
 */
public class BuildSourceTask extends AbstractDelegateRunnableTask {
  @Inject private Map<Class<? extends SettingValue>, Class<? extends BuildService>> buildServiceMap;
  @Inject private ServiceClassLocator serviceLocator;
  @Inject private Injector injector;

  private static final Logger logger = LoggerFactory.getLogger(BuildSourceTask.class);

  public BuildSourceTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    try {
      BuildSourceRequest buildSourceRequest = (BuildSourceRequest) parameters[0];
      int limit = buildSourceRequest.getLimit();
      String artifactStreamType = buildSourceRequest.getArtifactStreamType();
      SettingValue settingValue = buildSourceRequest.getSettingValue();
      BuildService service = getBuildService(artifactStreamType);
      ArtifactStreamAttributes artifactStreamAttributes = buildSourceRequest.getArtifactStreamAttributes();
      String appId = buildSourceRequest.getAppId();
      List<EncryptedDataDetail> encryptedDataDetails = buildSourceRequest.getEncryptedDataDetails();
      BuildSourceRequestType buildSourceRequestType = buildSourceRequest.getBuildSourceRequestType();

      List<BuildDetails> buildDetails = new ArrayList<>();
      if (buildSourceRequestType.equals(BuildSourceRequestType.GET_BUILDS)) {
        if (ArtifactStreamType.CUSTOM.name().equals(artifactStreamType)) {
          buildDetails = service.getBuilds(artifactStreamAttributes);
        } else {
          boolean enforceLimitOnResults =
              limit != -1 && "ARTIFACTORY".equals(artifactStreamType); // TODO: supported for Artifactory only
          buildDetails = enforceLimitOnResults
              ? service.getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails, limit)
              : service.getBuilds(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
        }

      } else {
        BuildDetails lastSuccessfulBuild =
            service.getLastSuccessfulBuild(appId, artifactStreamAttributes, settingValue, encryptedDataDetails);
        buildDetails.add(lastSuccessfulBuild);
      }

      return BuildSourceExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .buildSourceResponse(BuildSourceResponse.builder().buildDetails(buildDetails).build())
          .build();
    } catch (Exception ex) {
      logger.error("Exception in processing BuildSource task [{}]", ex);
      return BuildSourceExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(Misc.getMessage(ex))
          .build();
    }
  }

  public HelmCommandExecutionResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  private BuildService getBuildService(String artifactStreamType) {
    Class<? extends BuildService> buildServiceClass = serviceLocator.getBuildServiceClass(artifactStreamType);
    return injector.getInstance(Key.get(buildServiceClass));
  }
}
