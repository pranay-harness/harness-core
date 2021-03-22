package io.harness.cdng.environment;

import static io.harness.ng.core.mapper.TagMapper.convertToList;

import io.harness.cdng.environment.yaml.EnvironmentYaml;

import javax.annotation.Nonnull;

import io.harness.steps.shellScript.environment.EnvironmentOutcome;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EnvironmentMapper {
  public io.harness.steps.shellScript.environment.EnvironmentOutcome toOutcome(@Nonnull EnvironmentYaml environmentYaml) {
    return EnvironmentOutcome.builder()
        .identifier(environmentYaml.getIdentifier())
        .name(environmentYaml.getName() != null ? environmentYaml.getName() : "")
        .description(environmentYaml.getDescription() != null ? environmentYaml.getDescription().getValue() : "")
        .tags(convertToList(environmentYaml.getTags()))
        .environmentType(environmentYaml.getType())
        .build();
  }
}
