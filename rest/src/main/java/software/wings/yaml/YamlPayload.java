package software.wings.yaml;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;

/**
 * Created by bsollish on 8/9/17
 * This is for a Yaml payload wrapped in JSON
 */
public class YamlPayload {
  private String yaml;

  public YamlPayload() {}

  public YamlPayload(String yamlString) {
    this.setYamlPayload(yamlString);
  }

  public String getYaml() {
    return yaml;
  }

  public void setYamlPayload(String yamlString) {
    validateYamlString(yamlString);

    this.yaml = yamlString;
  }

  public static boolean validateYamlString(String yamlString) {
    // For validation, confirm that a Yaml Object can be constructed from the Yaml string
    Yaml yamlObj = new Yaml();
    yamlObj.load(yamlString); // NOTE: we don't do anything with the Yaml Object

    return true;
  }
}
