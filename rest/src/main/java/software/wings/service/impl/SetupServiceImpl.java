package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Setup.Builder.aSetup;
import static software.wings.beans.Setup.SetupStatus.COMPLETE;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.SetupAction.Builder.aSetupAction;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.Setup;
import software.wings.beans.SetupAction;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;
/**
 * Created by anubhaw on 6/30/16.
 */
@ValidateOnExecution
@Singleton
public class SetupServiceImpl implements SetupService {
  @Inject private HostService hostService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  public Setup getApplicationSetupStatus(Application application) {
    SetupAction mandatoryAction = fetchIncompleteMandatoryApplicationAction(application);
    Setup setup = aSetup().build();
    if (mandatoryAction != null) {
      setup.setSetupStatus(INCOMPLETE);
      setup.getActions().add(mandatoryAction);
    } else {
      setup.setSetupStatus(COMPLETE);
      SetupAction nextSetupAction = nextAction(application);
      if (nextSetupAction != null) {
        setup.getActions().add(nextSetupAction);
      }
    }
    return setup;
  }

  /**
   * @param application
   * @return
   */
  private SetupAction nextAction(Application application) {
    SetupAction artifactStreamSetupAction = getArtifactStreamSetupAction(application);
    if (artifactStreamSetupAction != null) {
      return artifactStreamSetupAction;
    } else {
      return getDeploymentSetupAction(application);
    }
  }

  private SetupAction getArtifactStreamSetupAction(Application application) {
    PageRequest<ArtifactStream> req = aPageRequest().addFilter("appId", Operator.EQ, application.getUuid()).build();
    PageResponse<ArtifactStream> res = artifactStreamService.list(req);
    if (isEmpty(res)) {
      return SetupAction.Builder.aSetupAction()
          .withCode("NO_ARTIFACT_STREAM_FOUND")
          .withDisplayText("Setup complete: now you can add artifact stream and deployment.")
          .withUrl(String.format("/#/app/%s/artifact-streams", application.getUuid()))
          .build();
    }

    ArtifactStream artifactStream = res.getResponse().get(0);

    PageRequest<Artifact> pageReques =
        aPageRequest().addFilter("appId", Operator.EQ, application.getUuid()).withLimit("1").build();
    PageResponse<Artifact> artRes = artifactService.list(pageReques, false);
    if (isEmpty(artRes)) {
      return SetupAction.Builder.aSetupAction()
          .withCode("NO_ARTIFACT_FOUND")
          .withDisplayText("Setup complete: Please add an artifact")
          .withUrl(
              String.format("/#/app/%s/artifact-streams/%s/detail", application.getUuid(), artifactStream.getUuid()))
          .build();
    }
    if (artRes.getTotal() == 1 && artRes.get(0).getStatus() == Status.QUEUED) {
      return SetupAction.Builder.aSetupAction()
          .withCode("ARTIFACT_NOT_READY")
          .withDisplayText("Setup complete: Please wait for the artifact to finish downloading.")
          .withUrl(
              String.format("/#/app/%s/artifact-streams/%s/detail", application.getUuid(), artifactStream.getUuid()))
          .build();
    }
    return null;
  }

  private SetupAction getDeploymentSetupAction(Application application) {
    PageRequest<WorkflowExecution> req =
        aPageRequest().addFilter("appId", Operator.EQ, application.getUuid()).withLimit("1").build();
    PageResponse<WorkflowExecution> res = workflowExecutionService.listExecutions(req, false);
    if (isNotEmpty(res)) {
      return null;
    }
    for (Environment env : application.getEnvironments()) {
      List<Host> hosts = hostService.getHostsByEnv(env.getAppId(), env.getUuid());
      if (isNotEmpty(hosts)) {
        return SetupAction.Builder.aSetupAction()
            .withCode("NO_DEPLOYMENT_FOUND")
            .withDisplayText("Setup complete: you can create a deployment.")
            .withUrl(String.format("/#/app/%s/env/%s/executions", application.getUuid(), env.getUuid()))
            .build();
      }
    }
    return null;
  }

  @Override
  public Setup getServiceSetupStatus(Service service) {
    return aSetup().withSetupStatus(COMPLETE).build();
  }

  @Override
  public Setup getEnvironmentSetupStatus(Environment environment) {
    SetupAction mandatoryAction = fetchIncompleteMandatoryEnvironmentAction(environment);
    Setup setup = aSetup().build();
    if (mandatoryAction != null) {
      setup.setSetupStatus(INCOMPLETE);
      setup.getActions().add(mandatoryAction);
    } else {
      setup.setSetupStatus(COMPLETE);
      // TODO: Get suggestions here
    }
    return setup;
  }

  private SetupAction fetchIncompleteMandatoryEnvironmentAction(Environment environment) {
    List<Host> hosts = hostService.getHostsByEnv(environment.getAppId(), environment.getUuid());

    if (hosts.isEmpty()) {
      return aSetupAction()
          .withCode("NO_HOST_CONFIGURED")
          .withDisplayText("Setup required: Please add at least one host to the environment.")
          .withUrl(String.format("/#/app/%s/env/%s/detail", environment.getAppId(), environment.getUuid()))
          .build();
    }
    return null;
  }

  private SetupAction fetchIncompleteMandatoryApplicationAction(Application app) {
    if (isEmpty(app.getServices())) {
      return aSetupAction()
          .withCode("SERVICE_NOT_CONFIGURED")
          .withDisplayText("Setup required: Please configure at least one service.")
          .withUrl(String.format("/#/app/%s/services", app.getUuid()))
          .build();
    }

    if (isEmpty(app.getEnvironments())) {
      return aSetupAction()
          .withCode("ENVIRONMENT_NOT_CONFIGURED")
          .withDisplayText("Setup required: Please configure at least one environment.")
          .withUrl(String.format("/#/app/%s/environments", app.getUuid()))
          .build();
    } else {
      Map<String, SetupAction> setupActions = new HashMap<>();
      Map<String, String> envNameMap = new HashMap<>();
      app.getEnvironments().forEach(env -> {
        setupActions.put(env.getUuid(), fetchIncompleteMandatoryEnvironmentAction(env));
        envNameMap.put(env.getName(), env.getUuid());
      });

      if (setupActions.size() == app.getEnvironments().size()) {
        if (envNameMap.containsKey(Constants.DEV_ENV)) {
          return setupActions.get(envNameMap.get(Constants.DEV_ENV));
        } else {
          return setupActions.values().iterator().next();
        }
      }
    }
    return null;
  }
}
