package software.wings.service.impl;

import static software.wings.beans.Setup.Builder.aSetup;
import static software.wings.beans.Setup.SetupStatus.COMPLETE;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.SetupAction.Builder.aSetupAction;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Release;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.Setup;
import software.wings.beans.SetupAction;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.SetupService;

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

  @Override
  public Setup getApplicationSetupStatus(Application application) {
    SetupAction mandatoryAction = fetchIncompleteMandatoryApplicationAction(application);
    Setup setup = aSetup().build();
    if (mandatoryAction != null) {
      setup.setSetupStatus(INCOMPLETE);
      setup.getActions().add(mandatoryAction);
    } else {
      setup.setSetupStatus(COMPLETE);
      setup.getActions().add(nextAction(application));
    }
    return setup;
  }

  /**
   * @param application
   * @return
   */
  private SetupAction nextAction(Application application) {
    SetupAction releaseSetupAction = getReleaseSetupAction(application);
    return releaseSetupAction;
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
          .withDisplayText("Please add a release and build source and deploy.")
          .withUrl(String.format("/#/app/%s/releases", application.getUuid()))
          .build();
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
          .withDisplayText("Setup Incomplete: Please add at least one host to the environment.")
          .withUrl(String.format("/#/app/%s/env/%s/detail", environment.getAppId(), environment.getUuid()))
          .build();
    }
    return null;
  }

  private SetupAction fetchIncompleteMandatoryApplicationAction(Application app) {
    if (app.getServices() == null || app.getServices().size() == 0) {
      return aSetupAction()
          .withCode("SERVICE_NOT_CONFIGURED")
          .withDisplayText("Setup Incomplete: Please configure at least one service.")
          .withUrl(String.format("/#/app/%s/services", app.getUuid()))
          .build();
    }

    if ((app.getEnvironments() == null || app.getEnvironments().size() == 0)) {
      return aSetupAction()
          .withCode("ENVIRONMENT_NOT_CONFIGURED")
          .withDisplayText("Setup Incomplete: Please configure at least one environment.")
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
