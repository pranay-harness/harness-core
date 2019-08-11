package software.wings.service.impl.yaml.handler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AllowedValueYaml;
import software.wings.beans.ArtifactStreamAllowedValueYaml;
import software.wings.beans.FeatureName;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Singleton
@Slf4j
public class ServiceVariableYamlHelper {
  @Inject FeatureFlagService featureFlagService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject SettingsService settingsService;

  public void convertArtifactVariableToYaml(
      String accountId, ServiceVariable serviceVariable, List<AllowedValueYaml> allowedValueYamlList) {
    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      if (isNotEmpty(serviceVariable.getAllowedList())) {
        for (String id : serviceVariable.getAllowedList()) {
          ArtifactStream as = artifactStreamService.get(id);
          if (as != null) {
            SettingAttribute settingAttribute = settingsService.get(as.getSettingId());
            allowedValueYamlList.add(ArtifactStreamAllowedValueYaml.builder()
                                         .artifactServerName(settingAttribute.getName())
                                         .artifactStreamName(as.getName())
                                         .artifactStreamType(as.getArtifactStreamType())
                                         .type("ARTIFACT")
                                         .build());
          } else {
            logger.warn("Artifact Stream with id {} not found, not converting it to yaml", id);
          }
        }
      }
    } else {
      logger.warn(
          "Variable type ARTIFACT not supported, skipping processing of variable {}", serviceVariable.getName());
    }
  }
}
