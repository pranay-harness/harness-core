package software.wings.yaml;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ResponseMessage.aResponseMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.FlowStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions.ScalarStyle;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.PropertyUtils;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.Pipeline;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.Level;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.YamlVersion.Type;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YamlHelper {
  public static final String ENCRYPTED_VALUE_STR = "<KMS URL>";

  public static void addResponseMessage(RestResponse rr, ErrorCode errorCode, Level level, String message) {
    ResponseMessage rm = aResponseMessage().code(errorCode).level(level).message(message).build();

    List<ResponseMessage> responseMessages = rr.getResponseMessages();
    responseMessages.add(rm);
    rr.setResponseMessages(responseMessages);
  }

  public static void addUnrecognizedFieldsMessage(RestResponse rr) {
    addResponseMessage(
        rr, ErrorCode.UNRECOGNIZED_YAML_FIELDS, Level.ERROR, "ERROR: The Yaml provided contains unrecognized fields!");
  }

  public static void addCouldNotMapBeforeYamlMessage(RestResponse rr) {
    addResponseMessage(
        rr, ErrorCode.COULD_NOT_MAP_BEFORE_YAML, Level.ERROR, "ERROR: The BEFORE Yaml could not be mapped!");
  }

  public static void addMissingBeforeYamlMessage(RestResponse rr) {
    addResponseMessage(rr, ErrorCode.MISSING_BEFORE_YAML, Level.ERROR, "ERROR: The BEFORE Yaml is empty or missing!");
  }

  public static void addMissingYamlMessage(RestResponse rr) {
    addResponseMessage(rr, ErrorCode.MISSING_YAML, Level.ERROR, "ERROR: The Yaml is empty or missing!");
  }

  public static void addNonEmptyDeletionsWarningMessage(RestResponse rr) {
    addResponseMessage(rr, ErrorCode.NON_EMPTY_DELETIONS, Level.WARN,
        "WARNING: This operation will delete objects! Pass 'deleteEnabled=true' if you want to proceed.");
  }

  public static void addSettingAttributeNotFoundMessage(RestResponse rr, String uuid) {
    addResponseMessage(
        rr, ErrorCode.GENERAL_YAML_ERROR, Level.ERROR, "ERROR: No Setting Attribute found for uuid: '" + uuid + "'!");
  }

  public static void addUnknownSettingVariableTypeMessage(RestResponse rr, SettingVariableTypes settingVariableType) {
    addResponseMessage(rr, ErrorCode.GENERAL_YAML_ERROR, Level.ERROR,
        "ERROR: Unrecognized SettingVariableType: '" + settingVariableType + "'!");
  }

  public static YamlRepresenter getRepresenter() {
    return getRepresenter(true);
  }

  public static YamlRepresenter getRepresenter(boolean removeEmptyValues) {
    YamlRepresenter representer = new YamlRepresenter(removeEmptyValues);

    // use custom that PropertyUtils that doesn't sort alphabetically
    PropertyUtils pu = new CustomPropertyUtils();
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
      YamlGitService yamlGitSyncService, String entityId, String accountId, BaseYaml theYaml, String payloadName) {
    RestResponse rr = new RestResponse<>();

    String dumpedYaml = toYamlString(theYaml);
    YamlPayload yp = new YamlPayload(dumpedYaml);
    yp.setName(payloadName);

    // add the YamlGitSync instance (if found) to the payload
    if (yamlGitSyncService != null) {
      YamlGitConfig ygs = yamlGitSyncService.get(accountId, entityId);
      if (ygs != null) {
        yp.setGitSync(ygs);
      }
    }

    rr.setResponseMessages(yp.getResponseMessages());

    if (isNotEmpty(yp.getYaml())) {
      rr.setResource(yp);
    }

    return rr;
  }

  public static String toYamlString(BaseYaml theYaml) {
    Yaml yaml = new Yaml(YamlHelper.getRepresenter(), YamlHelper.getDumperOptions());
    return cleanupYaml(yaml.dump(theYaml));
  }

  public static String cleanupYaml(String yaml) {
    // instead of removing the first line - we should remove any line that starts with two exclamation points
    yaml = cleanUpDoubleExclamationLines(yaml);

    // remove empty arrays/lists:
    yaml = yaml.replace("[]", "");

    yaml = fixIndentSpaces(yaml);

    return yaml;
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

        sb.append(line).append('\n');
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
          sb.append(' ');
        }

        sb.append(line).append('\n');
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return sb.toString();
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

  public static long getEntityCreatedAt(WingsPersistence wingsPersistence, YamlVersion yv) {
    String entityId = yv.getEntityId();
    Type type = yv.getType();

    switch (type) {
      case SETUP:
        return wingsPersistence.createQuery(Account.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      case APP:
        return wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      case SERVICE:
        return wingsPersistence.createQuery(Service.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      case SERVICE_COMMAND:
        return wingsPersistence.createQuery(ServiceCommand.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      case ENVIRONMENT:
        return wingsPersistence.createQuery(Environment.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      case SETTING:
        return wingsPersistence.createQuery(SettingAttribute.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      case WORKFLOW:
        return wingsPersistence.createQuery(Workflow.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      case PIPELINE:
        return wingsPersistence.createQuery(Pipeline.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      case TRIGGER:
        return wingsPersistence.createQuery(ArtifactStream.class).field(ID_KEY).equal(entityId).get().getCreatedAt();
      default:
        // nothing to do
    }

    return 0;
  }

  public static GitSyncWebhook verifyWebhookToken(
      WingsPersistence wingsPersistence, String accountId, String webhookToken) {
    GitSyncWebhook gsw = wingsPersistence.createQuery(GitSyncWebhook.class)
                             .field("webhookToken")
                             .equal(webhookToken)
                             .field("accountId")
                             .equal(accountId)
                             .get();

    if (gsw != null) {
      return gsw;
    }

    return null;
  }
}
