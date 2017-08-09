package software.wings.yaml;

/**
 * Created by bsollish on 8/9/17
 * This is for a Yaml payload wrapped in JSON
 */
public class YamlPayload {
  private String yaml;

  public YamlPayload() {}

  public YamlPayload(String yaml) {
    this.yaml = yaml;
  }

  public String getYaml() {
    return yaml;
  }

  public void setYamlPayload(String yaml) {
    this.yaml = yaml;
  }
}

/*
    Yaml y = new Yaml();
    y.load(yamlString);
 */
