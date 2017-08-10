package software.wings.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bsollish on 8/9/17
 * This is for a Yaml payload wrapped in JSON
 */
public class YamlPayload {
  private String yaml = "";
  @JsonIgnore private List<ResponseMessage> responseMessages = new ArrayList<>();

  // required no arg constructor
  public YamlPayload() {}

  public YamlPayload(String yamlString) {
    this.setYamlPayload(yamlString);
  }

  public String getYaml() {
    return yaml;
  }

  public void setYamlPayload(String yamlString) {
    if (validateYamlString(yamlString)) {
      this.yaml = yamlString;
    } else {
      // create a ResponseMessage
      ResponseMessage rm = new ResponseMessage();
      rm.setCode(ErrorCode.INVALID_YAML_PAYLOAD);
      rm.setErrorType(ResponseTypeEnum.ERROR);
      rm.setMessage("ERROR: Yaml provided to YamlPayload is not valid!");
      this.responseMessages.add(rm);
    }
  }

  public List<ResponseMessage> getResponseMessages() {
    return responseMessages;
  }

  @JsonIgnore
  public void setResponseMessages(List<ResponseMessage> responseMessages) {
    this.responseMessages = responseMessages;
  }

  public static boolean validateYamlString(String yamlString) {
    // For validation, confirm that a Yaml Object can be constructed from the Yaml string
    Yaml yamlObj = new Yaml();

    try {
      // NOTE: we don't do anything with the Yaml Object
      yamlObj.load(yamlString);
      return true;
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return false;
  }
}
