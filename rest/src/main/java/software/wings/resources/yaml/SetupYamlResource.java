package software.wings.resources.yaml;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;
import static software.wings.yaml.YamlHelper.doMapperReadValue;
import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.YamlHistoryService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.CloudProvidersYaml;
import software.wings.yaml.SetupYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersion.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Setup Yaml Resource class.
 *
 * @author bsollish
 */
@Api("/setupYaml")
@Path("/setupYaml")
@Produces("application/json")
@AuthRule(SETTING)
public class SetupYamlResource {
  private AppService appService;
  private SettingsService settingsService;
  private YamlHistoryService yamlHistoryService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  /**
   * Instantiates a new app resource.
   *
   * @param appService the app service
   * @param yamlHistoryService the yaml history service
   */
  @Inject
  public SetupYamlResource(
      AppService appService, SettingsService settingsService, YamlHistoryService yamlHistoryService) {
    this.appService = appService;
    this.settingsService = settingsService;
    this.yamlHistoryService = yamlHistoryService;
  }

  /**
   * Gets the setup yaml by accountId
   *
   * @param accountId  the account id
   * @return the rest response
   */
  @GET
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<YamlPayload> get(@PathParam("accountId") String accountId) {
    SetupYaml setup = new SetupYaml();

    List<String> appNames = appService.getAppNamesByAccountId(accountId);
    setup.setAppNames(appNames);

    //------------- CLOUD PROVIDERS SECTION ------------------
    CloudProvidersYaml cloudProvidersYaml = new CloudProvidersYaml();

    // types of cloud providers
    doCloudProviders(cloudProvidersYaml, setup, accountId, "AWS", SettingVariableTypes.AWS);
    doCloudProviders(cloudProvidersYaml, setup, accountId, "google_cloud_platform", SettingVariableTypes.GCP);
    doCloudProviders(
        cloudProvidersYaml, setup, accountId, "physical_data_centers", SettingVariableTypes.PHYSICAL_DATA_CENTER);

    setup.setCloudProviders(cloudProvidersYaml);
    //------------- END CLOUD PROVIDERS SECTION ------------------

    //------------- ARTIFACT SERVERS SECTION ------------------
    // types of artifact servers
    doArtifactServers(setup, accountId, SettingVariableTypes.JENKINS);
    doArtifactServers(setup, accountId, SettingVariableTypes.BAMBOO);
    doArtifactServers(setup, accountId, SettingVariableTypes.DOCKER);
    doArtifactServers(setup, accountId, SettingVariableTypes.NEXUS);
    doArtifactServers(setup, accountId, SettingVariableTypes.ARTIFACTORY);
    //------------- END ARTIFACT SERVERS SECTION ------------------

    // TODO - LEFT OFF HERE

    /*
    List<String> collaborationProviderNames = settingsService.getAppNamesByAccountId(accountId);
    setup.setCollaborationProviderNames(collaborationProviderNames);

    List<String> loadBalancerNames = settingsService.getAppNamesByAccountId(accountId);
    setup.setLoadBalancerNames(loadBalancerNames);

    List<String> verificationProviderNames = settingsService.getAppNamesByAccountId(accountId);
    setup.setVerificationProviderNames(verificationProviderNames);
    */

    return YamlHelper.getYamlRestResponse(setup, "setup.yaml");
  }

  private void doCloudProviders(CloudProvidersYaml cloudProvidersYaml, SetupYaml setup, String accountId,
      String subListName, SettingVariableTypes type) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        switch (type) {
          case AWS:
            cloudProvidersYaml.getAWS().add(settingAttribute.getName());
            break;
          case GCP:
            cloudProvidersYaml.getGoogleCloudPlatform().add(settingAttribute.getName());
            break;
          case PHYSICAL_DATA_CENTER:
            cloudProvidersYaml.getPhysicalDataCenters().add(settingAttribute.getName());
            break;
        }
      }
    }
  }

  private void doArtifactServers(SetupYaml setup, String accountId, SettingVariableTypes type) {
    List<SettingAttribute> settingAttributes = settingsService.getGlobalSettingAttributesByType(accountId, type.name());

    if (settingAttributes != null) {
      // iterate over providers
      for (SettingAttribute settingAttribute : settingAttributes) {
        setup.addArtifactServerName(settingAttribute.getName());
      }
    }
  }

  // TODO - NOTE: we probably don't need PUT and POST endpoints - there is really only one method - update (PUT)

  /**
   * Save the changes reflected in setupYaml (in a JSON "wrapper")
   *
   * @param accountId  the account id
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  @POST
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<SetupYaml> save(@PathParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
    String yaml = yamlPayload.getYaml();

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // DOES NOTHING

    return rr;
  }

  /**
   * Update setup that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId  the account id
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  @PUT
  @Path("/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<SetupYaml> update(@PathParam("accountId") String accountId, YamlPayload yamlPayload,
      @QueryParam("deleteEnabled") @DefaultValue("false") boolean deleteEnabled) {
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
            YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
                "applicationMap does not contain the key: " + appName + "!");
            return rr;
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
      YamlVersion beforeYamLVersion = aYamlVersion()
                                          .withAccountId(accountId)
                                          .withEntityId(accountId)
                                          .withType(Type.SETUP)
                                          .withYaml(beforeYaml)
                                          .build();
      yamlHistoryService.save(beforeYamLVersion);

      rr.setResource(roundtripSetupYaml);

    } else {
      // missing Yaml
      YamlHelper.addMissingYamlMessage(rr);
    }

    return rr;
  }
}
