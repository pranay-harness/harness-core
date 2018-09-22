package software.wings.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.wings.beans.ResponseMessage;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsollish on 8/9/17
 * This is for a Yaml payload wrapped in JSON
 */
public class YamlPayload {
  private String name = "";
  private String yaml = "";
  private String path;

  @JsonIgnore private List<ResponseMessage> responseMessages = new ArrayList<>();

  private YamlGitConfig gitSync;

  // required no arg constructor
  public YamlPayload() {}

  public YamlPayload(String yamlString) {
    this.setYamlPayload(yamlString);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getYaml() {
    return yaml;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setYamlPayload(String yamlString) {
    this.yaml = yamlString;
  }

  public List<ResponseMessage> getResponseMessages() {
    return responseMessages;
  }

  @JsonIgnore
  public void setResponseMessages(List<ResponseMessage> responseMessages) {
    this.responseMessages = responseMessages;
  }

  public YamlGitConfig getGitSync() {
    return gitSync;
  }

  public void setGitSync(YamlGitConfig gitSync) {
    this.gitSync = gitSync;
  }
}
