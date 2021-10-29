package software.wings.service.impl.yaml.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Singleton
class YamlFieldValidator {
  private static final String AMI_FILTERS = "amiFilters";
  private static final String PHASES = "phases";
  private static final String AMI_TAGS = "amiTags";
  private static final String NAME = "name";
  private static final String VARIABLE_OVERRIDES = "variableOverrides";

  void validateYaml(String yamlString) {
    Yaml yamlObj = new Yaml(new SafeConstructor());

    // We just load the yaml to see if its well formed.
    LinkedHashMap<String, Object> load = (LinkedHashMap<String, Object>) yamlObj.load(yamlString);
    checkOnPhasesNamesWithDots(load);
    checkOnEmptyAmiFiltersNames(load);
    checkOnEmptyAmiTagNames(load);
    checkEmptyVariableOverridesValues(load);
    checkEmptyVariableOverridesNames(load);
  }

  private void checkOnPhasesNamesWithDots(LinkedHashMap<String, Object> load) {
    if (load.containsKey(PHASES)) {
      List<String> phaseNames =
          ((List<LinkedHashMap<String, String>>) load.get(PHASES)).stream().map(map -> map.get(NAME)).collect(toList());

      phaseNames.forEach(name -> {
        if (name.contains(".")) {
          throw new InvalidYamlNameException("Invalid phase name [" + name + "]. Dots are not permitted");
        }
      });
    }
  }

  private void checkOnEmptyAmiFiltersNames(LinkedHashMap<String, Object> load) {
    if (load.containsKey(AMI_FILTERS)) {
      List<String> phaseNames = ((List<LinkedHashMap<String, String>>) load.get(AMI_FILTERS))
                                    .stream()
                                    .map(map -> map.get(NAME))
                                    .collect(toList());

      phaseNames.forEach(name -> {
        if (name.trim().isEmpty()) {
          throw new InvalidYamlNameException("Invalid amiFilter name. Empty names are not permitted");
        }
      });
    }
  }

  private void checkOnEmptyAmiTagNames(LinkedHashMap<String, Object> load) {
    if (load.containsKey(AMI_TAGS)) {
      List<String> phaseNames = ((List<LinkedHashMap<String, String>>) load.get(AMI_TAGS))
                                    .stream()
                                    .map(map -> map.get(NAME))
                                    .collect(toList());

      phaseNames.forEach(name -> {
        if (name.trim().isEmpty()) {
          throw new InvalidYamlNameException("Invalid amiTag name. Empty names are not permitted");
        }
      });
    }
  }

  private void checkEmptyVariableOverridesValues(LinkedHashMap<String, Object> load) {
    ((List<Map<String, String>>) load.getOrDefault(VARIABLE_OVERRIDES, ImmutableList.of()))
        .stream()
        .map(map -> map.get("value"))
        .forEach(value -> {
          if (value == null || value.trim().isEmpty()) {
            throw new InvalidRequestException("Variable override value cannot be null.");
          }
        });
  }

  private void checkEmptyVariableOverridesNames(Map<String, Object> load) {
    ((List<Map<String, String>>) load.getOrDefault(VARIABLE_OVERRIDES, ImmutableList.of()))
        .stream()
        .map(map -> map.get("name"))
        .forEach(name -> {
          if (name == null || name.trim().isEmpty()) {
            throw new InvalidRequestException("Variable override name cannot be null.");
          }
        });
  }
}
