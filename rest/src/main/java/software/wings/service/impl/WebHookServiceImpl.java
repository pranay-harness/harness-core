package software.wings.service.impl;

import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.utils.Misc.isNullOrEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.WebHookService;

import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class WebHookServiceImpl implements WebHookService {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public WebHookResponse execute(String token, WebHookRequest webHookRequest) {
    try {
      String appId = webHookRequest.getApplication();
      String artifactStreamId = webHookRequest.getArtifactSource();

      ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
      if (artifactStream == null) {
        return WebHookResponse.builder().error("Invalid request payload").build();
      }

      ArtifactStreamAction streamAction = artifactStream.getStreamActions()
                                              .stream()
                                              .filter(sa -> token.equals(sa.getWebHookToken()))
                                              .findFirst()
                                              .orElse(null);
      if (streamAction == null) {
        return WebHookResponse.builder().error("Invalid WebHook token").build();
      }

      Artifact artifact = null;
      if (isNullOrEmpty(webHookRequest.getBuildNumber()) && isNullOrEmpty(webHookRequest.getImageTag())) {
        artifact = artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId);
      } else {
        String requestBuildNumber = isNullOrEmpty(webHookRequest.getBuildNumber()) ? webHookRequest.getImageTag()
                                                                                   : webHookRequest.getBuildNumber();
        artifact = artifactService.getArtifactByBuildNumber(appId, artifactStreamId, requestBuildNumber);
        if (artifact == null) {
          // do collection and then run
          logger.error("Artifact not found for webhook request " + webHookRequest);
          return WebHookResponse.builder().status(ERROR.name()).error("Artifact collection not supported").build();
        }
      }
      WorkflowExecution workflowExecution = artifactStreamService.triggerStreamAction(artifact, streamAction);
      return WebHookResponse.builder()
          .requestId(workflowExecution.getUuid())
          .status(workflowExecution.getStatus().name())
          .build();
      // Collect artifact and then execute
    } catch (Exception ex) {
      logger.error("Webhook call failed [%s]", token, ex);
      return WebHookResponse.builder().error(ex.getMessage().toLowerCase()).build();
    }
  }
}

// generate requestId and save workflow executionId map
// queue multiple request
// compare queued requests
// wait for artifact to appear
// return response;
// Already running workflow
