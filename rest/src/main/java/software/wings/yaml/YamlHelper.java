package software.wings.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.FlowStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.ScalarStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.PropertyUtils;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.Setup;
import software.wings.beans.command.ServiceCommand;
import software.wings.exception.WingsException;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.YamlNode;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YamlHelper {
  public static void addResponseMessage(
      RestResponse rr, ErrorCode errorCode, ResponseTypeEnum responseType, String message) {
    ResponseMessage rm = new ResponseMessage();
    rm.setCode(errorCode);
    rm.setErrorType(responseType);
    rm.setMessage(message);

    List<ResponseMessage> responseMessages = rr.getResponseMessages();
    responseMessages.add(rm);
    rr.setResponseMessages(responseMessages);
  }

  public static void addUnrecognizedFieldsMessage(RestResponse rr) {
    addResponseMessage(rr, ErrorCode.UNRECOGNIZED_YAML_FIELDS, ResponseTypeEnum.ERROR,
        "ERROR: The Yaml provided contains unrecognized fields!");
  }

  public static void addCouldNotMapBeforeYamlMessage(RestResponse rr) {
    addResponseMessage(
        rr, ErrorCode.COULD_NOT_MAP_BEFORE_YAML, ResponseTypeEnum.ERROR, "ERROR: The BEFORE Yaml could not be mapped!");
  }

  public static void addMissingBeforeYamlMessage(RestResponse rr) {
    addResponseMessage(
        rr, ErrorCode.MISSING_BEFORE_YAML, ResponseTypeEnum.ERROR, "ERROR: The BEFORE Yaml is empty or missing!");
  }

  public static void addMissingYamlMessage(RestResponse rr) {
    addResponseMessage(rr, ErrorCode.MISSING_YAML, ResponseTypeEnum.ERROR, "ERROR: The Yaml is empty or missing!");
  }

  public static void addNonEmptyDeletionsWarningMessage(RestResponse rr) {
    addResponseMessage(rr, ErrorCode.NON_EMPTY_DELETIONS, ResponseTypeEnum.WARN,
        "WARNING: This operation will delete objects! Pass 'deleteEnabled=true' if you want to proceed.");
  }

  public static YamlRepresenter getRepresenter() {
    YamlRepresenter representer = new YamlRepresenter();

    // use custom that PropertyUtils that doesn't sort alphabetically
    PropertyUtils pu = new UnsortedPropertyUtils();
    pu.setSkipMissingProperties(false);

    representer.setPropertyUtils(pu);

    return representer;
  }

  public static DumperOptions getDumperOptions() {
    DumperOptions dumpOpts = new DumperOptions();
    // dumpOpts.setPrettyFlow(true);
    dumpOpts.setPrettyFlow(false); // keeps the empty square brackets together
    dumpOpts.setDefaultFlowStyle(FlowStyle.BLOCK);
    dumpOpts.setDefaultScalarStyle(ScalarStyle.PLAIN);
    dumpOpts.setIndent(2);

    return dumpOpts;
  }

  public static RestResponse<YamlPayload> getYamlRestResponse(GenericYaml theYaml, String payloadName) {
    RestResponse rr = new RestResponse<>();

    Yaml yaml = new Yaml(YamlHelper.getRepresenter(), YamlHelper.getDumperOptions());
    String dumpedYaml = yaml.dump(theYaml);

    // remove first line of Yaml:
    dumpedYaml = dumpedYaml.substring(dumpedYaml.indexOf('\n') + 1);

    // remove empty arrays/lists:
    dumpedYaml = dumpedYaml.replace("[]", "");

    dumpedYaml = fixIndentSpaces(dumpedYaml);

    YamlPayload yp = new YamlPayload(dumpedYaml);
    yp.setName(payloadName);

    rr.setResponseMessages(yp.getResponseMessages());

    if (yp.getYaml() != null && !yp.getYaml().isEmpty()) {
      rr.setResource(yp);
    }

    return rr;
  }

  private static String fixIndentSpaces(String content) {
    StringBuilder sb = new StringBuilder();

    BufferedReader bufReader = new BufferedReader(new StringReader(content));

    String line = null;

    try {
      while ((line = bufReader.readLine()) != null) {
        StringBuilder newLine = new StringBuilder();

        // count number of spaces or dashes at start of line
        int count = 0;

        for (int i = 0; i < line.length(); i++) {
          char c = line.charAt(i);
          // Process char
          if (c != ' ' && c != '-') {
            count = i;
            break;
          }
        }

        // prepend that many spaces to the start of the line
        for (int i = 0; i < count; i++) {
          newLine.append(" ");
        }

        sb.append(newLine.append(line) + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return sb.toString();
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

  public static <E> List<E> findDifferenceBetweenLists(List<E> itemsA, List<E> itemsB) {
    // we need to make a copy of itemsA, because we don't want to modify itemsA!
    List<E> diffList = new ArrayList<>();
    diffList.addAll(itemsA);

    if (diffList != null && itemsB != null) {
      diffList.removeAll(itemsB);
    }

    return diffList;
  }

  public static <T> Optional<T> doMapperReadValue(
      RestResponse rr, ObjectMapper mapper, String yamlStr, Class<T> theClass) {
    try {
      T thing = mapper.readValue(yamlStr, theClass);
      return Optional.of(thing);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      // bad before Yaml
      e.printStackTrace();
      YamlHelper.addCouldNotMapBeforeYamlMessage(rr);
      return Optional.empty();
    }
  }
}
