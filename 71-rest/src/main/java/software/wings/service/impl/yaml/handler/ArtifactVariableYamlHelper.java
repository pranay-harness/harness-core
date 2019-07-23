package software.wings.service.impl.yaml.handler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.AllowedValueYaml;
import software.wings.beans.ArtifactStreamAllowedValueYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.List;

@Singleton
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
      default:
        throw new IllegalStateException("Unexpected value: " + artifactStreamType);
    }
    return settingVariableTypes;
  }

  @NotNull
  public List<String> computeAllowedList(String accountId, List<AllowedValueYaml> allowedValueYamlList) {
    List<String> allowedList = new ArrayList<>();
    if (isNotEmpty(allowedValueYamlList)) {
      for (AllowedValueYaml allowedValueYaml : allowedValueYamlList) {
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
      }
    }
    return allowedList;
  }
}
