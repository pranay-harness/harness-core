package software.wings.service.impl;

import static com.amazonaws.regions.Regions.GovCloud;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.ResourceType;
import io.harness.data.structure.EmptyPredicate;
import software.wings.app.MainConfiguration;
import software.wings.beans.AwsConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.NameValuePairBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AwsHelperResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by sgurubelli on 7/16/17.
 */
@Singleton
@ValidateOnExecution
public class AwsHelperResourceServiceImpl implements AwsHelperResourceService {
  @Inject private MainConfiguration mainConfiguration;

  @Inject private AwsHelperService awsHelperService;
  @Inject private SettingsService settingService;
  @Inject private SecretManager secretManager;

  @Deprecated
  public Map<String, String> getRegions() {
    return Arrays.stream(Regions.values())
        .filter(regions -> regions != Regions.GovCloud)
        .collect(toMap(Regions::getName,
            regions
            -> Optional.ofNullable(mainConfiguration.getAwsRegionIdToName())
                   .orElse(ImmutableMap.of(regions.getName(), regions.getName()))
                   .get(regions.getName())));
  }

  public List<NameValuePair> getAwsRegions() {
    Map<String, String> awsRegionIdToName = mainConfiguration.getAwsRegionIdToName();
    if (EmptyPredicate.isEmpty(awsRegionIdToName)) {
      awsRegionIdToName = new LinkedHashMap<>();
    }
    List<NameValuePair> awsRegions = new ArrayList<>();
    for (Regions region : Regions.values()) {
      String regionName = region.getName();
      if (!GovCloud.getName().equals(regionName)) {
        NameValuePairBuilder regionNameValuePair = NameValuePair.builder().value(regionName);
        if (awsRegionIdToName.containsKey(regionName)) {
          regionNameValuePair.name(awsRegionIdToName.get(regionName));
        } else {
          regionNameValuePair.name(regionName);
        }
        awsRegions.add(regionNameValuePair.build());
      }
    }
    return awsRegions;
  }

  @Override
  public Set<String> listTags(String appId, String computeProviderId, String region, String resourceTypeStr) {
    ResourceType resourceType = resourceTypeStr == null ? ResourceType.Image : ResourceType.valueOf(resourceTypeStr);
    SettingAttribute computeProviderSetting = settingService.get(computeProviderId);
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsHelperService.listTags(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, resourceType);
  }

  private AwsConfig validateAndGetAwsConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AwsConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "No cloud provider exist or not of type Aws");
    }

    return (AwsConfig) computeProviderSetting.getValue();
  }
}
