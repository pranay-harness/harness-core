package software.wings.resources;

import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.ServiceVariable.Builder.aServiceVariable;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;
import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Node;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.exception.WingsException;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.YamlHistoryService;
import software.wings.utils.ArtifactType;
import software.wings.yaml.ConfigVarYaml;
import software.wings.yaml.ServiceYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.YamlVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Service Resource class.
 *
 * @author bsollish
 */
@Api("/serviceYaml")
@Path("/serviceYaml")
@Produces("application/json")
@AuthRule(APPLICATION)
public class ServiceYamlResource {
  private AppService appService;
  private ServiceResourceService serviceResourceService;
  private ServiceVariableService serviceVariableService;
  private YamlHistoryService yamlHistoryService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates new resources
   *
   * @param appService the app service
   * @param serviceResourceService the service (resource) service
   * @param serviceVariableService the service (variable) service
   * @param yamlHistoryService the yaml history service
   */
  @Inject
  public ServiceYamlResource(AppService appService, ServiceResourceService serviceResourceService,
      ServiceVariableService serviceVariableService, YamlHistoryService yamlHistoryService) {
    this.appService = appService;
    this.serviceResourceService = serviceResourceService;
    this.serviceVariableService = serviceVariableService;
    this.yamlHistoryService = yamlHistoryService;
  }

  /**
   * Gets the yaml version of a service by serviceId
   *
   * @param appId  the app id
   * @param serviceId  the service id
   * @return the rest response
   */
  @GET
  @Path("/{accountId}/{appId}/{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId, true);
    List<ServiceCommand> serviceCommands = service.getServiceCommands();

    ServiceYaml serviceYaml = new ServiceYaml(service);
    serviceYaml.setServiceCommandNamesFromServiceCommands(serviceCommands);

    List<ServiceVariable> serviceVariables = service.getServiceVariables();
    serviceYaml.setConfigVariablesFromServiceVariables(serviceVariables);

    return YamlHelper.getYamlRestResponse(serviceYaml, service.getName() + ".yaml");
  }

  // TODO - NOTE: we probably don't need PUT and POST endpoints - there is really only one method - update (PUT)

  /**
   * Save the changes reflected in serviceYaml (in a JSON "wrapper")
   *
   * @param serviceId  the service id
   * @param yamlPayload the yaml version of service
   * @return the rest response
   */
  @POST
  @Path("/{accountId}/{appId}/{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Application> save(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // DOES NOTHING

    return rr;
  }

  /**
   * Update a service that is sent as Yaml (in a JSON "wrapper")
   *
   * @param serviceId  the service id
   * @param yamlPayload the yaml version of service
   * @return the rest response
   */
  @PUT
  @Path("/{accountId}/{appId}/{serviceId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Service> update(@PathParam("appId") String appId, @PathParam("serviceId") String serviceId,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // get the before Yaml
    RestResponse beforeResponse = get(appId, serviceId);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    if (yaml.equals(beforeYaml)) {
      // no change
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "No change to the Yaml.");
      return rr;
    }

    // what are the serviceCommand changes? Which are additions and which are deletions?
    List<String> serviceCommandsToAdd = new ArrayList<String>();
    List<String> serviceCommandsToDelete = new ArrayList<String>();

    // what are the config variable changes? Which are additions and which are deletions?
    List<ConfigVarYaml> configVarsToAdd = new ArrayList<ConfigVarYaml>();
    List<ConfigVarYaml> configVarsToDelete = new ArrayList<ConfigVarYaml>();

    ServiceYaml beforeServiceYaml = null;

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      try {
        beforeServiceYaml = mapper.readValue(beforeYaml, ServiceYaml.class);
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

    ServiceYaml serviceYaml = null;

    if (yaml != null && !yaml.isEmpty()) {
      try {
        Service service = serviceResourceService.get(appId, serviceId);

        serviceYaml = mapper.readValue(yaml, ServiceYaml.class);

        // ----------- START SERVICE COMMAND SECTION ---------------
        List<String> serviceCommandNames = serviceYaml.getServiceCommandNames();

        if (serviceCommandNames != null) {
          // initialize the service commands to add from the after
          for (String sc : serviceCommandNames) {
            serviceCommandsToAdd.add(sc);
          }
        }

        if (beforeServiceYaml != null) {
          List<String> beforeServiceCommands = beforeServiceYaml.getServiceCommandNames();

          if (beforeServiceCommands != null) {
            // initialize the service commands to delete from the before, and remove the befores from the service
            // commands to add list
            for (String sc : beforeServiceCommands) {
              serviceCommandsToDelete.add(sc);
              serviceCommandsToAdd.remove(sc);
            }
          }
        }

        if (serviceCommandNames != null) {
          // remove the afters from the service commands to delete list
          for (String sc : serviceCommandNames) {
            serviceCommandsToDelete.remove(sc);
          }
        }

        List<ServiceCommand> serviceCommands = service.getServiceCommands();
        Map<String, ServiceCommand> serviceCommandMap = new HashMap<String, ServiceCommand>();

        if (serviceCommands != null) {
          // populate the map
          for (ServiceCommand serviceCommand : serviceCommands) {
            serviceCommandMap.put(serviceCommand.getName(), serviceCommand);
          }
        }

        // If we have deletions do a check - we CANNOT delete service commands without deleteEnabled true
        if (serviceCommandsToDelete.size() > 0 && !deleteEnabled) {
          YamlHelper.addNonEmptyDeletionsWarningMessage(rr);
          return rr;
        }

        if (serviceCommandsToDelete != null) {
          // do deletions
          for (String servCommandName : serviceCommandsToDelete) {
            if (serviceCommandMap.containsKey(servCommandName)) {
              serviceResourceService.deleteCommand(appId, serviceId, servCommandName);
            } else {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
                  "serviceCommandMap does not contain the key: " + servCommandName + "!");
              return rr;
            }
          }
        }

        if (serviceCommandsToAdd != null) {
          // do additions
          for (String scName : serviceCommandsToAdd) {
            ServiceCommand newServiceCommand = createNewServiceCommand(appId, scName);
            serviceResourceService.addCommand(appId, serviceId, newServiceCommand);
          }
        }
        // ----------- END SERVICE COMMAND SECTION ---------------

        // ----------- START CONFIG VARIABLE SECTION ---------------
        List<ConfigVarYaml> configVars = serviceYaml.getConfigVariables();

        if (configVars != null) {
          // initialize the config vars to add from the after
          for (ConfigVarYaml cv : configVars) {
            configVarsToAdd.add(cv);
          }
        }

        if (beforeServiceYaml != null) {
          List<ConfigVarYaml> beforeConfigVars = beforeServiceYaml.getConfigVariables();

          if (beforeConfigVars != null) {
            // initialize the config vars to delete from the before, and remove the befores from the config vars to add
            // list
            for (ConfigVarYaml cv : beforeConfigVars) {
              configVarsToDelete.add(cv);
              configVarsToAdd.remove(cv);
            }
          }
        }

        if (configVars != null) {
          // remove the afters from the config vars to delete list
          for (ConfigVarYaml cv : configVars) {
            configVarsToDelete.remove(cv);
          }
        }

        List<ServiceVariable> serviceVariables = service.getServiceVariables();
        Map<String, ServiceVariable> serviceVariableMap = new HashMap<String, ServiceVariable>();

        if (serviceVariables != null) {
          // populate the map
          for (ServiceVariable serviceVariable : serviceVariables) {
            serviceVariableMap.put(serviceVariable.getName(), serviceVariable);
          }
        }

        // If we have deletions do a check - we CANNOT delete config vars without deleteEnabled true
        if (configVarsToDelete.size() > 0 && !deleteEnabled) {
          YamlHelper.addNonEmptyDeletionsWarningMessage(rr);
          return rr;
        }

        // ServiceVariableResource svr = new ServiceVariableResource(serviceVariableService);

        if (configVarsToDelete != null) {
          // do deletions
          for (ConfigVarYaml cv : configVarsToDelete) {
            if (serviceVariableMap.containsKey(cv.getName())) {
              serviceVariableService.delete(appId, serviceVariableMap.get(cv.getName()).getUuid());
            } else {
              YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
                  "serviceVariableMap does not contain the key: " + cv.getName() + "!");
              return rr;
            }
          }
        }

        if (configVarsToAdd != null) {
          // do additions
          for (ConfigVarYaml cv : configVarsToAdd) {
            ServiceVariable savedServiceVariable =
                serviceVariableService.save(createNewServiceVariable(appId, service.getUuid(), cv));
          }
        }
        // ----------- END CONFIG VARIABLE SECTION ---------------

        // save the changes
        service.setName(serviceYaml.getName());
        service.setDescription(serviceYaml.getDescription());
        String artifactTypeStr = serviceYaml.getArtifactType().toUpperCase();

        try {
          ArtifactType at = ArtifactType.valueOf(artifactTypeStr);
          service.setArtifactType(at);
        } catch (Exception e) {
          e.printStackTrace();
          YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
              "The ArtifactType: '" + artifactTypeStr + "' is not found in the ArtifactType Enum!");
          return rr;
        }

        service = serviceResourceService.update(service);

        // return the new resource
        if (service != null) {
          // save the before yaml version
          String accountId = appService.get(appId).getAccountId();
          YamlVersion beforeYamLVersion = aYamlVersion()
                                              .withAccountId(accountId)
                                              .withEntityId(accountId)
                                              .withType(YamlVersion.Type.SERVICE)
                                              .withYaml(beforeYaml)
                                              .build();
          yamlHistoryService.save(beforeYamLVersion);

          rr.setResource(service);
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

  private ServiceCommand createNewServiceCommand(String appId, String scName) {
    Node node = aNode()
                    .withType(CommandUnitType.RESIZE.getType())
                    .withName(CommandUnitType.RESIZE.getName() + "-0")
                    .withOrigin(true)
                    .build();

    Graph graph = aGraph().withGraphName(scName).addNodes(node).build();

    Command command = aCommand()
                          .withName(scName)
                          .withCommandType(CommandType.OTHER)
                          .withExecutionResult(CommandExecutionStatus.QUEUED)
                          .withGraph(graph)
                          .build();

    ServiceCommand newServiceCommand =
        aServiceCommand().withAppId(appId).withName(scName).withTargetToAllEnv(true).withCommand(command).build();

    return newServiceCommand;
  }

  private ServiceVariable createNewServiceVariable(String appId, String serviceId, ConfigVarYaml cv) {
    ServiceVariable newServiceVariable = aServiceVariable()
                                             .withName(cv.getName())
                                             .withValue(cv.getValue().toCharArray())
                                             .withEntityType(EntityType.SERVICE)
                                             .withEntityId(serviceId)
                                             .withTemplateId(ServiceVariable.DEFAULT_TEMPLATE_ID)
                                             .withType(Type.TEXT)
                                             .withAppId(appId)
                                             .build();

    return newServiceVariable;
  }
}
