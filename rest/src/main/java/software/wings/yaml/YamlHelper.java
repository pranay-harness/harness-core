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
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.directory.YamlNode;
import software.wings.yaml.gitSync.YamlGitSync;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YamlHelper {
  //@Inject private static YamlGitSyncService yamlGitSyncService;

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

  public static void addSettingAttributeNotFoundMessage(RestResponse rr, String uuid) {
    addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
        "ERROR: No Setting Attribute found for uuid: '" + uuid + "'!");
  }

  public static void addUnknownSettingVariableTypeMessage(RestResponse rr, SettingVariableTypes settingVariableType) {
    addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, ResponseTypeEnum.ERROR,
        "ERROR: Unrecognized SettingVariableType: '" + settingVariableType + "'!");
  }

  public static YamlRepresenter getRepresenter() {
    return getRepresenter(false, false);
  }

  public static YamlRepresenter getRepresenter(
      boolean removeEmptyValues, boolean everythingExceptDoNotSerializeAndTransients) {
    YamlRepresenter representer = new YamlRepresenter(removeEmptyValues, everythingExceptDoNotSerializeAndTransients);

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

  public static RestResponse<YamlPayload> getYamlRestResponse(
      YamlGitSyncService yamlGitSyncService, String entityId, GenericYaml theYaml, String payloadName) {
    RestResponse rr = new RestResponse<>();

    Yaml yaml = new Yaml(YamlHelper.getRepresenter(), YamlHelper.getDumperOptions());
    String dumpedYaml = yaml.dump(theYaml);
    YamlPayload yp = new YamlPayload(cleanupYaml(dumpedYaml));
    yp.setName(payloadName);

    // add the YamlGitSync instance (if found) to the payload
    if (yamlGitSyncService != null) {
      YamlGitSync ygs = yamlGitSyncService.get(entityId);
      if (ygs != null) {
        yp.setGitSync(ygs);
      }
    }

    rr.setResponseMessages(yp.getResponseMessages());

    if (yp.getYaml() != null && !yp.getYaml().isEmpty()) {
      rr.setResource(yp);
    }

    return rr;
  }

  public static String cleanupYaml(String yaml) {
    // instead of removing the first line - we should remove any line that starts with two exclamation points
    yaml = cleanUpDoubleExclamationLines(yaml);

    // remove empty arrays/lists:
    yaml = yaml.replace("[]", "");

    yaml = fixIndentSpaces(yaml);

    return yaml;
  }

  // added this while working on workflows
  public static <T> RestResponse<YamlPayload> getYamlRestResponseGeneric(
      T obj, String payloadName, boolean removeEmptyValues, boolean everythingExceptDoNotSerializeAndTransients) {
    RestResponse rr = new RestResponse<>();

    Yaml yaml = new Yaml(YamlHelper.getRepresenter(removeEmptyValues, everythingExceptDoNotSerializeAndTransients),
        YamlHelper.getDumperOptions());
    String dumpedYaml = yaml.dump(obj);

    // instead of removing the first line - we should remove any line that starts with two exclamation points
    dumpedYaml = cleanUpDoubleExclamationLines(dumpedYaml);

    // remove empty arrays/lists:
    dumpedYaml = dumpedYaml.replace("[]", "");

    dumpedYaml = fixIndentSpaces(dumpedYaml);
    // dumpedYaml = fixIndentSpaces2(dumpedYaml);

    YamlPayload yp = new YamlPayload(dumpedYaml);
    yp.setName(payloadName);

    rr.setResponseMessages(yp.getResponseMessages());

    if (yp.getYaml() != null && !yp.getYaml().isEmpty()) {
      rr.setResource(yp);
    }

    return rr;
  }

  private static String cleanUpDoubleExclamationLines(String content) {
    StringBuilder sb = new StringBuilder();

    BufferedReader bufReader = new BufferedReader(new StringReader(content));

    String line = null;

    try {
      while ((line = bufReader.readLine()) != null) {
        String trimmedLine = line.trim();

        // check for line starting with two exclamation points
        if (trimmedLine.length() >= 2 && trimmedLine.charAt(0) == '!' && trimmedLine.charAt(1) == '!') {
          continue;
        } else {
          // we need to remove lines BUT we have to add the dash to the NEXT line!
          if (trimmedLine.length() >= 4 && trimmedLine.charAt(0) == '-' && trimmedLine.charAt(1) == ' '
              && trimmedLine.charAt(2) == '!' && trimmedLine.charAt(3) == '!') {
            line = bufReader.readLine();
            if (line != null) {
              char[] chars = line.toCharArray();

              for (int i = 0; i < chars.length; i++) {
                if (chars[i] != ' ') {
                  if (i >= 2) {
                    chars[i - 2] = '-';
                    sb.append(new String(chars) + "\n");
                    break;
                  }
                }
              }
            }
            continue;
          }
        }

        sb.append(line + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return sb.toString();
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

  // TODO - newer version attempts to thr over-indenting of "sub folders" like AWS, GCP, etc. in Cloud Providers section
  // of setup.yaml
  private static String fixIndentSpaces2(String content) {
    System.out.println("********* BEFORE: \n" + content);

    StringBuilder sb = new StringBuilder();

    BufferedReader bufReader = new BufferedReader(new StringReader(content));

    String line = null;

    try {
      while ((line = bufReader.readLine()) != null) {
        StringBuilder newLine = new StringBuilder();

        String lineTrimmed = line.trim();

        // if the line starts with a dash - prepend two spaces
        if (lineTrimmed.charAt(0) == '-') {
          newLine.append("  ");
        } else {
          System.out.println("    ********* lineTrimmed: |" + lineTrimmed + "|");

          // check that the line doesn't end in a colon
          if (lineTrimmed.length() > 0 && !lineTrimmed.substring(lineTrimmed.length() - 1).equals(":")) {
            newLine.append("  ");
          }
        }

        sb.append(newLine.append(line) + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.println("********* AFTER: \n" + sb.toString());

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
