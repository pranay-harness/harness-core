package io.harness.cdng.environment;

import static io.harness.ng.core.mapper.TagMapper.convertToList;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;

@UtilityClass
public class EnvironmentMapper {
  public EnvironmentOutcome toOutcome(@Nonnull EnvironmentYaml environmentYaml) {
    return EnvironmentOutcome.builder()
        .identifier(environmentYaml.getIdentifier().getValue())
        .name(environmentYaml.getName() != null ? environmentYaml.getName().getValue() : "")
        .description(environmentYaml.getDescription() != null ? environmentYaml.getDescription().getValue() : "")
        .tags(convertToList(environmentYaml.getTags()))
        .type(environmentYaml.getType())
        .build();
  }
}
