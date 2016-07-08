package software.wings.service.impl;

import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.Setup.Builder.aSetup;
import static software.wings.beans.Setup.SetupStatus.COMPLETE;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.SetupAction.Builder.aSetupAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Service;
import software.wings.beans.Setup;
import software.wings.beans.SetupAction;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SetupService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 6/30/16.
 */
@ValidateOnExecution
@Singleton
public class SetupServiceImpl implements SetupService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private HostService hostService;
  @Inject private MainConfiguration configuration;

  @Override
  public Setup getApplicationSetupStatus(String appId) {
    Application application = appService.get(appId);
    if (application == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Application doesn't exist");
    }
    SetupAction mandatoryAction = fetchIncompleteMandatoryApplicationAction(application);
    Setup setup = aSetup().withEntity(application).build();
    if (mandatoryAction != null) {
      setup.setSetupStatus(INCOMPLETE);
      setup.getActions().add(mandatoryAction);
    } else {
      setup.setSetupStatus(COMPLETE);
      // Get suggestions here
    }
    return setup;
  }

  @Override
  public Setup getServiceSetupStatus(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    if (service == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Service doesn't exist");
    }
    return aSetup().withEntity(service).withSetupStatus(COMPLETE).build();
  }

  @Override
  public Setup getEnvironmentSetupStatus(String appId, String envId) {
    Environment environment = environmentService.get(appId, envId, false);
    if (environment == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Environment doesn't exist");
    }

    SetupAction mandatoryAction = fetchIncompleteMandatoryEnvironmentAction(environment);
    Setup setup = aSetup().withEntity(environment).build();
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
          .withDisplayText("Please add atlast one host to environment")
          .withUrl(getBaseUrl() + String.format("#/app/%s/env/%s", environment.getAppId(), environment.getUuid()))
          .build();
    }
    return null;
  }

  private SetupAction fetchIncompleteMandatoryApplicationAction(Application app) {
    if (app.getServices() == null || app.getServices().size() == 0) {
      return aSetupAction()
          .withCode("SERVICE_NOT_CONFIGURED")
          .withDisplayText("Please configure atlast one service.")
          .withUrl(getBaseUrl() + String.format("#/app/%s/services", app.getUuid()))
          .build();
    }

    if ((app.getEnvironments() == null || app.getEnvironments().size() == 0)) {
      return aSetupAction()
          .withCode("ENVIRONMENT_NOT_CONFIGURED")
          .withDisplayText("Please configure atlast one environment")
          .withUrl(getBaseUrl() + String.format("#/app/%s/environments", app.getUuid()))
          .build();
    } else {
      Optional<SetupAction> setupAction = app.getEnvironments()
                                              .stream()
                                              .map(this ::fetchIncompleteMandatoryEnvironmentAction)
                                              .filter(Objects::nonNull)
                                              .findFirst();
      if (setupAction.isPresent()) {
        return setupAction.get();
      }
    }
    return null;
  }

  private String getBaseUrl() {
    String baseURl = configuration.getPortal().getUrl().trim();
    if (baseURl.charAt(baseURl.length() - 1) != '/') {
      baseURl += "/";
    }
    return baseURl;
  }
}
