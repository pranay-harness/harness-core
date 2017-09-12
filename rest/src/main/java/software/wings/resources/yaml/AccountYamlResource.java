package software.wings.resources.yaml;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlHistoryService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.SetupYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.settingAttribute.AppDynamicsYaml;
import software.wings.yaml.settingAttribute.ArtifactoryYaml;
import software.wings.yaml.settingAttribute.AwsYaml;
import software.wings.yaml.settingAttribute.BambooYaml;
import software.wings.yaml.settingAttribute.DockerYaml;
import software.wings.yaml.settingAttribute.ElbYaml;
import software.wings.yaml.settingAttribute.ElkYaml;
import software.wings.yaml.settingAttribute.GcpYaml;
import software.wings.yaml.settingAttribute.JenkinsYaml;
import software.wings.yaml.settingAttribute.LogzYaml;
import software.wings.yaml.settingAttribute.NexusYaml;
import software.wings.yaml.settingAttribute.PhysicalDataCenterYaml;
import software.wings.yaml.settingAttribute.SettingAttributeYaml;
import software.wings.yaml.settingAttribute.SlackYaml;
import software.wings.yaml.settingAttribute.SmtpYaml;
import software.wings.yaml.settingAttribute.SplunkYaml;

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
 * Account Yaml Resource class.
 *
 * @author bsollish
 */
@Api("/accountYaml")
@Path("/accountYaml")
@Produces("application/json")
@AuthRule(SETTING)
public class AccountYamlResource {
  private SettingsService settingsService;
  private YamlHistoryService yamlHistoryService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  /**
   * Instantiates a new app resource.
   *
   * @param settingsService        the settings service
   */
  @Inject
  public AccountYamlResource(SettingsService settingsService, YamlHistoryService yamlHistoryService) {
    this.settingsService = settingsService;
    this.yamlHistoryService = yamlHistoryService;
  }

  // TODO - not 100% sure we need this endpoint - but it fills a logical role

  /**
   * Gets all the setting attributes of a given type by accountId
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @return the rest response
   */
  @GET
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> getList(@PathParam("accountId") String accountId, @QueryParam("type") String type) {
    /*
    List<String> appNames = appService.getAppNamesByAccountId(accountId);

    SetupYaml setup = new SetupYaml();
    setup.setAppNames(appNames);

    return YamlHelper.getYamlRestResponse(setup, "setup.yaml");
    */

    // SettingVariableTypes settingsVariableType = SettingVariableTypes.valueOf(type);

    return null;
  }

  /**
   * Gets the yaml for a setting attribute by accountId and uuid
   *
   * @param accountId   the account id
   * @param uuid        the uid of the setting attribute
   * @return the rest response
   */
  @GET
  @Path("/{accountId}/{uuid}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@PathParam("accountId") String accountId, @PathParam("uuid") String uuid) {
    SettingAttribute settingAttribute = settingsService.get(uuid);

    if (settingAttribute == null) {
      RestResponse rr = new RestResponse<>();
      YamlHelper.addSettingAttributeNotFoundMessage(rr, uuid);
      return rr;
    }

    logger.info("******* settingAttribute.getName() = " + settingAttribute.getName());

    SettingVariableTypes settingVariableType = SettingVariableTypes.valueOf(settingAttribute.getValue().getType());

    logger.info("******* settingVariableType = " + settingVariableType);

    SettingAttributeYaml settingAttributeYaml = null;

    switch (settingVariableType) {
      // cloud providers
      case AWS:
        settingAttributeYaml = new AwsYaml(settingAttribute);
        break;
      case GCP:
        settingAttributeYaml = new GcpYaml(settingAttribute);
        break;
      case PHYSICAL_DATA_CENTER:
        settingAttributeYaml = new PhysicalDataCenterYaml(settingAttribute);
        break;

      // artifact servers
      case JENKINS:
        settingAttributeYaml = new JenkinsYaml(settingAttribute);
        break;
      case BAMBOO:
        settingAttributeYaml = new BambooYaml(settingAttribute);
        break;
      case DOCKER:
        settingAttributeYaml = new DockerYaml(settingAttribute);
        break;
      case NEXUS:
        settingAttributeYaml = new NexusYaml(settingAttribute);
        break;
      case ARTIFACTORY:
        settingAttributeYaml = new ArtifactoryYaml(settingAttribute);
        break;

      // collaboration providers
      case SMTP:
        settingAttributeYaml = new SmtpYaml(settingAttribute);
        break;
      case SLACK:
        settingAttributeYaml = new SlackYaml(settingAttribute);
        break;

      // load balancers
      case ELB:
        settingAttributeYaml = new ElbYaml(settingAttribute);
        break;

      // verification providers
      // JENKINS is also a (logical) part of this group
      case APP_DYNAMICS:
        settingAttributeYaml = new AppDynamicsYaml(settingAttribute);
        break;
      case SPLUNK:
        settingAttributeYaml = new SplunkYaml(settingAttribute);
        break;
      case ELK:
        settingAttributeYaml = new ElkYaml(settingAttribute);
        break;
      case LOGZ:
        settingAttributeYaml = new LogzYaml(settingAttribute);
        break;
      default:
        // handle not found
        RestResponse rr = new RestResponse<>();
        YamlHelper.addUnknownSettingVariableTypeMessage(rr, settingVariableType);
        return rr;
    }

    if (settingAttributeYaml != null) {
      return YamlHelper.getYamlRestResponse(settingAttributeYaml, settingAttribute.getName() + ".yaml");
    }

    return null;
  }

  // TODO - NOTE: we probably don't need PUT and POST endpoints - there is really only one method - update (PUT)

  /**
   * Save the changes reflected in setupYaml (in a JSON "wrapper")
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  @POST
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<SetupYaml> save(@PathParam("accountId") String accountId, @QueryParam("type") String type,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // DOES NOTHING

    return rr;
  }

  /**
   * Update setup that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId   the account id
   * @param type        the SettingVariableTypes
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  @PUT
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<SetupYaml> update(@PathParam("accountId") String accountId, @QueryParam("type") String type,
      YamlPayload yamlPayload, @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    // TODO - needs implementation

    /*
    String afterYaml = yamlPayload.getYaml();

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // get the before Yaml
    RestResponse beforeResponse = get(accountId);
    YamlPayload beforeYP = (YamlPayload) beforeResponse.getResource();
    String beforeYaml = beforeYP.getYaml();

    if (afterYaml.trim().equals(beforeYaml.trim())) {
      // no change
      YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.INFO, "No change to the Yaml.");
      return rr;
    }

    SetupYaml beforeSetupYaml = null;

    if (beforeYaml != null && !beforeYaml.isEmpty()) {
      Optional<SetupYaml> beforeSetupYamlOpt = doMapperReadValue(rr, mapper, beforeYaml, SetupYaml.class);
      if (beforeSetupYamlOpt.isPresent()) {
        beforeSetupYaml = beforeSetupYamlOpt.get();
      } else {
        return rr;
      }
    } else {
      // missing before Yaml
      YamlHelper.addMissingBeforeYamlMessage(rr);
      return rr;
    }

    SetupYaml afterSetupYaml = null;

    if (afterYaml != null && !afterYaml.isEmpty()) {
      List<String> afterList = new ArrayList<>();
      List<String> beforeList = new ArrayList<>();

      Optional<SetupYaml> setupYamlOpt = doMapperReadValue(rr, mapper, afterYaml, SetupYaml.class);
      if (setupYamlOpt.isPresent()) {
        afterSetupYaml = setupYamlOpt.get();
      } else {
        return rr;
      }

      if (afterSetupYaml != null) {
        afterList = afterSetupYaml.getAppNames();
      }

      if (beforeSetupYaml != null) {
        beforeList = beforeSetupYaml.getAppNames();
      }

      // what are the changes? Determine additions and deletions
      List<String> addList = YamlHelper.findDifferenceBetweenLists(afterList, beforeList);
      List<String> deleteList = YamlHelper.findDifferenceBetweenLists(beforeList, afterList);

      // If we have deletions do a check - we CANNOT delete applications without deleteEnabled true
      if (deleteList.size() > 0 && !deleteEnabled) {
        YamlHelper.addNonEmptyDeletionsWarningMessage(rr);
        return rr;
      }

      List<Application> applications = appService.getAppsByAccountId(accountId);
      Map<String, Application> applicationMap = new HashMap<String, Application>();

      if (applications != null) {
        // populate the map
        for (Application application : applications) {
          applicationMap.put(application.getName(), application);
        }
      }

      if (deleteList != null) {
        // do deletions
        for (String appName : deleteList) {
          if (applicationMap.containsKey(appName)) {
            appService.delete(applicationMap.get(appName).getAppId());
          } else {
            YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR, "applicationMap does
    not contain the key: " + appName + "!"); return rr;
          }
        }
      }

      if (addList != null) {
        // do additions
        for (String s : addList) {
          // create the new Application
          Application newApplication = anApplication().withAccountId(accountId).withName(s).withDescription("").build();

          appService.save(newApplication);
        }
      }

      // get the after Yaml to confirm addition/deletion changes
      RestResponse afterResponse = get(accountId);
      YamlPayload afterYP = (YamlPayload) afterResponse.getResource();
      String roundtripYaml = afterYP.getYaml();

      SetupYaml roundtripSetupYaml = null;

      setupYamlOpt = doMapperReadValue(rr, mapper, roundtripYaml, SetupYaml.class);
      if (setupYamlOpt.isPresent()) {
        roundtripSetupYaml = setupYamlOpt.get();
      } else {
        return rr;
      }

      // save the before yaml version
      YamlVersion beforeYamLVersion =
    aYamlVersion().withAccountId(accountId).withEntityId(accountId).withType(Type.SETUP).withYaml(beforeYaml).build();
      yamlHistoryService.save(beforeYamLVersion);

      rr.setResource(roundtripSetupYaml);

    } else {
      // missing Yaml
      YamlHelper.addMissingYamlMessage(rr);
    }

    return rr;
    */

    // TODO - TEMP
    return null;
  }
}