package software.wings.service.impl.yaml;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.yaml.YamlHelper.doMapperReadValue;
import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.SetupYamlResourceService;
import software.wings.service.intfc.yaml.YamlHistoryService;
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

public class SetupYamlResourceServiceImpl implements SetupYamlResourceService {
  private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  @Inject private AppService appService;
  @Inject private YamlHistoryService yamlHistoryService;
  @Inject private SettingsService settingsService;

  /**
   * Gets the setup yaml by accountId
   *
   * @param accountId  the account id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getSetup(String accountId) {
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

  /**
   * Update setup that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId  the account id
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  public RestResponse<SetupYaml> updateSetup(String accountId, YamlPayload yamlPayload, boolean deleteEnabled) {
    String afterYaml = yamlPayload.getYaml();

    RestResponse rr = new RestResponse<>();
    rr.setResponseMessages(yamlPayload.getResponseMessages());

    // get the before Yaml
    RestResponse beforeResponse = getSetup(accountId);
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
      RestResponse afterResponse = getSetup(accountId);
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
