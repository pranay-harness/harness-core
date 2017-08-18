package software.wings.yaml;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.FlowStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.ScalarStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup;
import software.wings.beans.command.ServiceCommand;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.YamlNode;

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

  public static FolderNode sampleConfigAsCodeDirectory() {
    FolderNode config = new FolderNode("config", Setup.class);
    config.addChild(new YamlNode("setup.yaml", SetupYaml.class));
    FolderNode applications = new FolderNode("applications", Application.class);
    config.addChild(applications);

    FolderNode myapp1 = new FolderNode("Myapp1", Application.class);
    applications.addChild(myapp1);
    myapp1.addChild(new YamlNode("Myapp1.yaml", AppYaml.class));
    FolderNode myapp1_services = new FolderNode("services", Service.class);
    applications.addChild(myapp1_services);

    FolderNode myapp1_Login = new FolderNode("Login", Service.class);
    myapp1_services.addChild(myapp1_Login);
    myapp1_Login.addChild(new YamlNode("Login.yaml", ServiceYaml.class));
    FolderNode myapp1_Login_serviceCommands = new FolderNode("service-commands", ServiceCommand.class);
    myapp1_Login.addChild(myapp1_Login_serviceCommands);
    myapp1_Login_serviceCommands.addChild(new YamlNode("start.yaml", ServiceCommand.class));
    myapp1_Login_serviceCommands.addChild(new YamlNode("install.yaml", ServiceCommand.class));
    myapp1_Login_serviceCommands.addChild(new YamlNode("stop.yaml", ServiceCommand.class));

    FolderNode myapp1_Order = new FolderNode("Order", Service.class);
    myapp1_services.addChild(myapp1_Order);
    myapp1_Order.addChild(new YamlNode("Order.yaml", ServiceYaml.class));
    FolderNode myapp1_Order_serviceCommands = new FolderNode("service-commands", ServiceCommand.class);
    myapp1_Order.addChild(myapp1_Order_serviceCommands);

    FolderNode myapp2 = new FolderNode("Myapp2", Application.class);
    applications.addChild(myapp2);
    myapp2.addChild(new YamlNode("Myapp2.yaml", AppYaml.class));
    FolderNode myapp2_services = new FolderNode("services", Service.class);
    applications.addChild(myapp2_services);

    return config;
  }
}
