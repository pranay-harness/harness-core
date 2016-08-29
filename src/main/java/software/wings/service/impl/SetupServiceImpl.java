package software.wings.service.impl;

import static software.wings.beans.Setup.Builder.aSetup;
import static software.wings.beans.Setup.SetupStatus.COMPLETE;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.SetupAction.Builder.aSetupAction;

import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Artifact.Status;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Release;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.Setup;
import software.wings.beans.SetupAction;
import software.wings.beans.WorkflowExecution;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.WorkflowService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 6/30/16.
 */
@ValidateOnExecution
@Singleton
public class SetupServiceImpl implements SetupService {
  @Inject private HostService hostService;
  @Inject private ReleaseService releaseService;
  @Inject private ArtifactService artifactService;
  @Inject private WorkflowService workflowService;

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
    SetupAction releaseSetupAction = getReleaseSetupAction(application);
    if (releaseSetupAction != null) {
      return releaseSetupAction;
    } else {
      return getDeploymentSetupAction(application);
    }
  }

  private SetupAction getDeploymentSetupAction(Application application) {
    PageRequest<WorkflowExecution> req = PageRequest.Builder.aPageRequest()
                                             .addFilter("appId", Operator.EQ, application.getUuid())
                                             .withLimit("1")
                                             .build();
    PageResponse<WorkflowExecution> res = workflowService.listExecutions(req, false);
    if (res != null && !res.isEmpty()) {
      return null;
    }
    for (Environment env : application.getEnvironments()) {
      List<Host> hosts = hostService.getHostsByEnv(env.getAppId(), env.getUuid());
      if (hosts != null && !hosts.isEmpty()) {
        return SetupAction.Builder.aSetupAction()
            .withCode("NO_DEPLOYMENT_FOUND")
            .withDisplayText("Setup complete: you can create a deployment.")
            .withUrl(String.format("/#/app/%s/env/%s/executions", application.getUuid(), env.getUuid()))
            .build();
      }
    }
    return null;
  }

  /**
   * @param application
   * @return
   */
  private SetupAction getReleaseSetupAction(Application application) {
    PageRequest<Release> req =
        PageRequest.Builder.aPageRequest().addFilter("appId", Operator.EQ, application.getUuid()).build();
    PageResponse<Release> res = releaseService.list(req);
    if (res == null || res.isEmpty()) {
      return SetupAction.Builder.aSetupAction()
          .withCode("NO_RELEASE_FOUND")
          .withDisplayText("Setup complete: now you can create release and deployment.")
          .withUrl(String.format("/#/app/%s/releases", application.getUuid()))
          .build();
    }

    Release rel = findReleaseWithSource(res.getResponse());
    if (rel == null) {
      return SetupAction.Builder.aSetupAction()
          .withCode("NO_ARTIFACT_SOURCE_FOUND")
          .withDisplayText("Setup complete: Please add a build source.")
          .withUrl(String.format("/#/app/%s/release/%s/detail", application.getUuid(), res.get(0).getUuid()))
          .build();
    }

    PageRequest<Artifact> pageReques = PageRequest.Builder.aPageRequest()
                                           .addFilter("appId", Operator.EQ, application.getUuid())
                                           .withLimit("1")
                                           .build();
    PageResponse<Artifact> artRes = artifactService.list(pageReques);
    if (artRes == null || artRes.isEmpty()) {
      return SetupAction.Builder.aSetupAction()
          .withCode("NO_ARTIFACT_FOUND")
          .withDisplayText("Setup complete: Please add an artifact for the release.")
          .withUrl(String.format("/#/app/%s/release/%s/detail", application.getUuid(), rel.getUuid()))
          .build();
    }
    if (artRes.getTotal() == 1 && artRes.get(0).getStatus() == Status.QUEUED) {
      return SetupAction.Builder.aSetupAction()
          .withCode("ARTIFACT_NOT_READY")
          .withDisplayText("Setup complete: Please wait for the artifact to finish downloading.")
          .withUrl(String.format("/#/app/%s/release/%s/detail", application.getUuid(), rel.getUuid()))
          .build();
    }
    return null;
  }

  private Release findReleaseWithSource(List<Release> list) {
    for (Release rel : list) {
      if (rel != null && rel.getArtifactSources() != null && !rel.getArtifactSources().isEmpty()) {
        return rel;
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

    if (hosts.size() == 0) {
      return aSetupAction()
          .withCode("NO_HOST_CONFIGURED")
          .withDisplayText("Setup required: Please add at least one host to the environment.")
          .withUrl(String.format("/#/app/%s/env/%s/detail", environment.getAppId(), environment.getUuid()))
          .build();
    }
    return null;
  }

  private SetupAction fetchIncompleteMandatoryApplicationAction(Application app) {
    if (app.getServices() == null || app.getServices().size() == 0) {
      return aSetupAction()
          .withCode("SERVICE_NOT_CONFIGURED")
          .withDisplayText("Setup required: Please configure at least one service.")
          .withUrl(String.format("/#/app/%s/services", app.getUuid()))
          .build();
    }

    if ((app.getEnvironments() == null || app.getEnvironments().size() == 0)) {
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
