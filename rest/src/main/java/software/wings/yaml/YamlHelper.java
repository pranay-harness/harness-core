package software.wings.yaml;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.FlowStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.ScalarStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;

import java.util.List;

public class YamlHelper {
  public static void addUnrecognizedFieldsMessage(RestResponse rr) {
    ResponseMessage rm = new ResponseMessage();
    rm.setCode(ErrorCode.UNRECOGNIZED_YAML_FIELDS);
    rm.setErrorType(ResponseTypeEnum.ERROR);
    rm.setMessage("ERROR: The Yaml provided contains unrecognized fields!");

    List<ResponseMessage> responseMessages = rr.getResponseMessages();
    responseMessages.add(rm);
    rr.setResponseMessages(responseMessages);
  }

  public static YamlRepresenter getRepresenter() {
    YamlRepresenter representer = new YamlRepresenter();

    return representer;
  }

  public static DumperOptions getDumperOptions() {
    DumperOptions dumpOpts = new DumperOptions();
    dumpOpts.setPrettyFlow(true);
    dumpOpts.setDefaultFlowStyle(FlowStyle.BLOCK);
    dumpOpts.setDefaultScalarStyle(ScalarStyle.PLAIN);

    return dumpOpts;
  }

  public static RestResponse<YamlPayload> getYamlRestResponse(GenericYaml theYaml, String payloadName) {
    RestResponse rr = new RestResponse<>();

    Yaml yaml = new Yaml(YamlHelper.getRepresenter(), YamlHelper.getDumperOptions());
    String dumpedYaml = yaml.dump(theYaml);

    // remove first line of Yaml:
    dumpedYaml = dumpedYaml.substring(dumpedYaml.indexOf('\n') + 1);

    YamlPayload yp = new YamlPayload(dumpedYaml);
    yp.setName(payloadName);

    rr.setResponseMessages(yp.getResponseMessages());

    if (yp.getYaml() != null && !yp.getYaml().isEmpty()) {
      rr.setResource(yp);
    }

    return rr;
  }
}
