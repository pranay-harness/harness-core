package software.wings.service.impl.yaml;

import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.utils.ArtifactType;
import software.wings.yaml.AppYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersion.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class AppYamlResourceServiceImpl implements AppYamlResourceService {
  @Inject private AppService appService;
  @Inject private YamlHistoryService yamlHistoryService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;

  /**
   * Gets the yaml version of an app by appId
   *
   * @param appId  the app id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getApp(String appId) {
    List<Service> services = serviceResourceService.findServicesByApp(appId);
    List<Environment> environments = environmentService.getEnvByApp(appId);
    Application app = appService.get(appId);

    AppYaml appYaml = new AppYaml();
    appYaml.setName(app.getName());
    appYaml.setDescription(app.getDescription());
    appYaml.setServiceNamesFromServices(services);
    appYaml.setEnvironmentNamesFromEnvironments(environments);

    return YamlHelper.getYamlRestResponse(appYaml, app.getName() + ".yaml");
  }

  /**
   * Update an app that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId  the app id
   * @param yamlPayload the yaml version of app
   * @return the rest response
   */
  public RestResponse<Application> updateApp(String appId, YamlPayload yamlPayload, boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // get the before Yaml
    RestResponse beforeResponse = getApp(appId);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    if (yaml.equals(beforeYaml)) {
      // no change
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "No change to the Yaml.");
      return rr;
    }

    // what are the service changes? Which are additions and which are deletions?
    List<String> servicesToAdd = new ArrayList<String>();
    List<String> servicesToDelete = new ArrayList<String>();

    // what are the environment changes? Which are additions and which are deletions?
    List<String> environmentsToAdd = new ArrayList<String>();
    List<String> environmentsToDelete = new ArrayList<String>();

    AppYaml beforeAppYaml = null;

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      try {
        beforeAppYaml = mapper.readValue(beforeYaml, AppYaml.class);
      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        // bad before Yaml
        e.printStackTrace();
        YamlHelper.addCouldNotMapBeforeYamlMessage(rr);
        return rr;
      }
    } else {
      // missing before Yaml
      YamlHelper.addMissingBeforeYamlMessage(rr);
      return rr;
    }

    AppYaml appYaml = null;

    if (yaml != null && !yaml.isEmpty()) {
      try {
        appYaml = mapper.readValue(yaml, AppYaml.class);

        Application app = appService.get(appId);

        // ----------- START SERVICES SECTION ---------------
        List<String> serviceNames = appYaml.getServiceNames();

        if (serviceNames != null) {
          // initialize the services to add from the after
          for (String s : serviceNames) {
            servicesToAdd.add(s);
          }
        }

        if (beforeAppYaml != null) {
          List<String> beforeServices = beforeAppYaml.getServiceNames();

          if (beforeServices != null) {
            // initialize the services to delete from the before, and remove the befores from the services to add list
            for (String s : beforeServices) {
              servicesToDelete.add(s);
              servicesToAdd.remove(s);
            }
          }
        }

        if (serviceNames != null) {
          // remove the afters from the services to delete list
          for (String s : serviceNames) {
            servicesToDelete.remove(s);
          }
        }

        List<Service> services = serviceResourceService.findServicesByApp(appId);
        Map<String, Service> serviceMap = new HashMap<String, Service>();

        if (services != null) {
          // populate the map
          for (Service service : services) {
            serviceMap.put(service.getName(), service);
          }
        }

        // If we have deletions do a check - we CANNOT delete services with workflows!
        if (servicesToDelete.size() > 0 && !deleteEnabled) {
          YamlHelper.addNonEmptyDeletionsWarningMessage(rr);
          return rr;
        }

        if (servicesToDelete != null) {
          // do deletions
          for (String servName : servicesToDelete) {
            if (serviceMap.containsKey(servName)) {
              serviceResourceService.delete(appId, serviceMap.get(servName).getUuid());
            } else {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
                  "serviceMap does not contain the key: " + servName + "!");
              return rr;
            }
          }
        }

        if (servicesToAdd != null) {
          // do additions
          for (String s : servicesToAdd) {
            // create the new Service
            // TODO - ideally it should use the default ArtifactType for the account and if that is empty/null, use the
            // Harness (level) default
            Service newService = aService()
                                     .withAppId(appId)
                                     .withName(s)
                                     .withDescription("")
                                     .withArtifactType(ArtifactType.DOCKER)
                                     .build();

            serviceResourceService.save(newService);
          }
        }
        // ----------- END SERVICES SECTION ---------------

        // ----------- START ENVIRONMENTS SECTION ---------------
        List<String> environmentNames = appYaml.getEnvironmentNames();

        if (environmentNames != null) {
          // initialize the environments to add from the after
          for (String s : environmentNames) {
            environmentsToAdd.add(s);
          }
        }

        if (beforeAppYaml != null) {
          List<String> beforeEnvironments = beforeAppYaml.getEnvironmentNames();

          if (beforeEnvironments != null) {
            // initialize the environments to delete from the before, and remove the befores from the environments to
            // add list
            for (String s : beforeEnvironments) {
              environmentsToDelete.add(s);
              environmentsToAdd.remove(s);
            }
          }
        }

        if (environmentNames != null) {
          // remove the afters from the environments to delete list
          for (String s : environmentNames) {
            environmentsToDelete.remove(s);
          }
        }

        List<Environment> environments = environmentService.getEnvByApp(appId);
        Map<String, Environment> environmentMap = new HashMap<String, Environment>();

        if (environments != null) {
          // populate the map
          for (Environment environment : environments) {
            environmentMap.put(environment.getName(), environment);
          }
        }

        // If we have deletions do a check - we CANNOT delete environments with workflows!
        if (environmentsToDelete.size() > 0 && !deleteEnabled) {
          YamlHelper.addNonEmptyDeletionsWarningMessage(rr);
          return rr;
        }

        if (environmentsToDelete != null) {
          // do deletions
          for (String envName : environmentsToDelete) {
            if (environmentMap.containsKey(envName)) {
              environmentService.delete(appId, environmentMap.get(envName).getUuid());
            } else {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
                  "environmentMap does not contain the key: " + envName + "!");
              return rr;
            }
          }
        }

        if (environmentsToAdd != null) {
          // do additions
          for (String s : environmentsToAdd) {
            // create the new Environment
            // TODO - ideally it should use the default ArtifactType for the account and if that is empty/null, use the
            // Harness (level) default
            Environment newEnvironment = anEnvironment()
                                             .withAppId(appId)
                                             .withName(s)
                                             .withDescription("")
                                             .withEnvironmentType(EnvironmentType.NON_PROD)
                                             .build();

            environmentService.save(newEnvironment);
          }
        }
        // ----------- END ENVIRONMENTS SECTION ---------------

        // save the changes
        app.setName(appYaml.getName());
        app.setDescription(appYaml.getDescription());

        app = appService.update(app);

        // return the new resource
        if (app != null) {
          // save the before yaml version
          String accountId = app.getAccountId();
          YamlVersion beforeYamLVersion = aYamlVersion()
                                              .withAccountId(accountId)
                                              .withEntityId(accountId)
                                              .withType(Type.APP)
                                              .withYaml(beforeYaml)
                                              .build();
          yamlHistoryService.save(beforeYamLVersion);

          rr.setResource(app);
        }

      } catch (WingsException e) {
        throw e;
      } catch (Exception e) {
        e.printStackTrace();
        YamlHelper.addUnrecognizedFieldsMessage(rr);
      }
    } else {
      // missing Yaml
      YamlHelper.addMissingYamlMessage(rr);
    }

    return rr;
  }
}
