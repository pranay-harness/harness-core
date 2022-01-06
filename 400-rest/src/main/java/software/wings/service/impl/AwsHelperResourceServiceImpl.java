/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.app.MainConfiguration;
import software.wings.beans.AwsConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.NameValuePairBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.AwsHelperResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsEc2HelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsS3HelperServiceManager;
import software.wings.service.intfc.security.SecretManager;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.model.ResourceType;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by sgurubelli on 7/16/17.
 */
@Singleton
@ValidateOnExecution
@OwnedBy(CDP)
public class AwsHelperResourceServiceImpl implements AwsHelperResourceService {
  @Inject private MainConfiguration mainConfiguration;

  @Inject private AwsEc2HelperServiceManager awsEc2HelperServiceManager;
  @Inject private SettingsService settingService;
  @Inject private SecretManager secretManager;
  @Inject private AwsS3HelperServiceManager s3HelperServiceManager;

  @Override
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

  @Override
  public List<NameValuePair> getAwsRegions() {
    Map<String, String> awsRegionIdToName = mainConfiguration.getAwsRegionIdToName();
    if (EmptyPredicate.isEmpty(awsRegionIdToName)) {
      awsRegionIdToName = new LinkedHashMap<>();
    }
    List<NameValuePair> awsRegions = new ArrayList<>();
    for (Regions region : Regions.values()) {
      String regionName = region.getName();
      NameValuePairBuilder regionNameValuePair = NameValuePair.builder().value(regionName);
      if (awsRegionIdToName.containsKey(regionName)) {
        regionNameValuePair.name(awsRegionIdToName.get(regionName));
      } else {
        regionNameValuePair.name(regionName);
      }
      awsRegions.add(regionNameValuePair.build());
    }
    return awsRegions;
  }

  @Override
  public Set<String> listTags(String appId, String computeProviderId, String region, String resourceTypeStr) {
    ResourceType resourceType = resourceTypeStr == null ? ResourceType.Image : ResourceType.valueOf(resourceTypeStr);
    SettingAttribute computeProviderSetting = settingService.get(computeProviderId);
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsEc2HelperServiceManager.listTags(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, appId, resourceType);
  }

  @Override
  public Set<String> listTags(String computeProviderId, String region, String resourceTypeStr) {
    ResourceType resourceType = resourceTypeStr == null ? ResourceType.Image : ResourceType.valueOf(resourceTypeStr);
    SettingAttribute computeProviderSetting = settingService.get(computeProviderId);
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsEc2HelperServiceManager.listTags(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, resourceType);
  }

  private AwsConfig validateAndGetAwsConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AwsConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "No cloud provider exist or not of type Aws");
    }

    return (AwsConfig) computeProviderSetting.getValue();
  }

  @Override
  public List<String> listBuckets(String awsSettingId) {
    SettingAttribute settingAttribute = settingService.get(awsSettingId);
    notNullCheck("Cloud provider doesn't exist", settingAttribute, USER);

    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, null, null);

    return s3HelperServiceManager.listBucketNames(awsConfig, encryptionDetails);
  }

  @Override
  public List<String> listCloudformationCapabilities() {
    return Stream.of(Capability.values()).map(Capability::toString).collect(Collectors.toList());
  }

  @Override
  public Set<String> listCloudFormationStatues() {
    return EnumSet.allOf(StackStatus.class).stream().map(status -> status.name()).collect(Collectors.toSet());
  }
}
