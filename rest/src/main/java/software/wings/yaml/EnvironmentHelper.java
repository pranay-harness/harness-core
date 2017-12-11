package software.wings.yaml;

import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Helper class that handles to and from yaml for environment entity.
 * @author rktummala on 10/08/17
 */
public class EnvironmentHelper {
  @Inject private ConfigFileHelper configFileHelper;

  public EnvironmentYaml toYaml(Environment environment) {
    List<ConfigFile> configFiles = environment.getConfigFiles();
    // TODO handle config var
    List<ConfigFileYaml> configFileYamlList =
        configFiles.stream().map(configFile -> configFileHelper.toYaml(configFile)).collect(Collectors.toList());

    return EnvironmentYaml.Builder.anEnvironmentYaml()
        .withName(environment.getName())
        .withDescription(environment.getDescription())
        .withEnvironmentType(environment.getEnvironmentType().name())
        .withConfigFileOverrides(configFileYamlList)
        .build();
  }
}
