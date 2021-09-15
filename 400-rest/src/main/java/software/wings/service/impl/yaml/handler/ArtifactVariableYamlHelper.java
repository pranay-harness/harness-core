/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;

import io.harness.exception.WingsException;

import software.wings.beans.AllowedValueYaml;
import software.wings.beans.ArtifactStreamAllowedValueYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
public class ArtifactVariableYamlHelper {
  @Inject SettingsService settingsService;
  @Inject ArtifactStreamService artifactStreamService;

  public SettingVariableTypes getSettingVariableTypeFromArtifactStreamType(String artifactStreamType) {
    ArtifactStreamType streamType = ArtifactStreamType.valueOf(artifactStreamType.toUpperCase());
    SettingVariableTypes settingVariableTypes;
    switch (streamType) {
      case JENKINS:
        settingVariableTypes = SettingVariableTypes.JENKINS;
        break;
      case BAMBOO:
        settingVariableTypes = SettingVariableTypes.BAMBOO;
        break;
      case DOCKER:
        settingVariableTypes = SettingVariableTypes.DOCKER;
        break;
      case GCR:
      case GCS:
        settingVariableTypes = SettingVariableTypes.GCP;
        break;
      case ACR:
        settingVariableTypes = SettingVariableTypes.ACR;
        break;
      case NEXUS:
        settingVariableTypes = SettingVariableTypes.NEXUS;
        break;
      case ARTIFACTORY:
        settingVariableTypes = SettingVariableTypes.ARTIFACTORY;
        break;
      case AMI:
      case ECR:
      case AMAZON_S3:
        settingVariableTypes = SettingVariableTypes.AWS;
        break;
      case SMB:
        settingVariableTypes = SettingVariableTypes.SMB;
        break;
      case SFTP:
        settingVariableTypes = SettingVariableTypes.SFTP;
        break;
      case CUSTOM:
        settingVariableTypes = SettingVariableTypes.CUSTOM;
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + artifactStreamType);
    }
    return settingVariableTypes;
  }

  @NotNull
  public List<String> computeAllowedList(
      String accountId, List<AllowedValueYaml> allowedValueYamlList, String artifactVariableName) {
    List<String> allowedList = new ArrayList<>();
    if (isNotEmpty(allowedValueYamlList)) {
      for (AllowedValueYaml allowedValueYaml : allowedValueYamlList) {
        if (allowedValueYaml instanceof ArtifactStreamAllowedValueYaml) {
          ArtifactStreamAllowedValueYaml artifactStreamAllowedValueYaml =
              (ArtifactStreamAllowedValueYaml) allowedValueYaml;
          SettingVariableTypes settingVariableTypes =
              getSettingVariableTypeFromArtifactStreamType(artifactStreamAllowedValueYaml.getArtifactStreamType());
          SettingAttribute settingAttribute = settingsService.fetchSettingAttributeByName(
              accountId, artifactStreamAllowedValueYaml.getArtifactServerName(), settingVariableTypes);
          if (settingAttribute == null) {
            throw new WingsException(format("Artifact Server with name: [%s] not found",
                                         artifactStreamAllowedValueYaml.getArtifactServerName()),
                USER);
          }
          ArtifactStream artifactStream = artifactStreamService.getArtifactStreamByName(
              settingAttribute.getUuid(), artifactStreamAllowedValueYaml.getArtifactStreamName());
          if (artifactStream == null) {
            throw new WingsException(format("Artifact Stream with name: [%s] not found under artifact xxxxxxxx [%s]",
                                         artifactStreamAllowedValueYaml.getArtifactStreamName(),
                                         artifactStreamAllowedValueYaml.getArtifactServerName()),
                USER);
          }

          if (!allowedList.contains(artifactStream.getUuid())) {
            allowedList.add(artifactStream.getUuid());
          }
        } // TODO: else throw Exception?
      }
    } else {
      throw new WingsException(
          format("Allowed artifact stream(s) not provided for variable: [%s]", artifactVariableName));
    }
    return allowedList;
  }
}
