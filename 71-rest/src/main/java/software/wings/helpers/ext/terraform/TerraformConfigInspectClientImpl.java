package software.wings.helpers.ext.terraform;

import static java.lang.String.format;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.configuration.InstallUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeroturnaround.exec.ProcessExecutor;

@Slf4j
@Singleton
public final class TerraformConfigInspectClientImpl implements TerraformConfigInspectClient {
  private static final String jsonArg = "--json";

  @Override
  public List<String> parseFieldsUnderBlock(String directory, String category) {
    try {
      String cmd = InstallUtils.getTerraformConfigInspectPath();
      String config = executeShellCommand(HarnessStringUtils.join(StringUtils.SPACE, cmd, jsonArg, directory));
      JSONObject parsedConfig = new JSONObject(config);
      JSONObject blockVariables = parsedConfig.getJSONObject(category);
      List<String> fields = new ArrayList<>(blockVariables.keySet());
      Optional<String> parseStatus = parseError(parsedConfig);
      parseStatus.ifPresent(
          error -> { throw new InvalidRequestException("Parse Error :" + error, WingsException.USER); });
      return fields;
    } catch (JSONException err) {
      throw new InvalidRequestException("Parse Error :", WingsException.USER);
    }
  }

  /*
   Return non empty optional<String> if parsing failed
   Json JsonConfig will have a diagnostic block which would contain errors/warnings if any
   */
  private Optional<String> parseError(JSONObject jsonConfig) {
    if (jsonConfig.isNull(BLOCK_TYPE.DIAGNOSTICS.name().toLowerCase())) {
      return Optional.empty();
    }
    try {
      // Returning only the first block of error here
      JSONObject errorBlock = jsonConfig.getJSONArray(BLOCK_TYPE.DIAGNOSTICS.name().toLowerCase()).getJSONObject(0);
      String errorType = errorBlock.getString(BLOCK_TYPE.SEVERITY.name().toLowerCase());
      if (errorType.equalsIgnoreCase(ERROR_TYPE.ERROR.name().toLowerCase())) {
        String error = errorBlock.getString(BLOCK_TYPE.DETAIL.name().toLowerCase());
        return Optional.of(error);
      }
    } catch (JSONException e) {
      log.error("Could not parse tf-config-inspect output for errors {}", jsonConfig.toString());
    }
    return Optional.empty();
  }

  private String executeShellCommand(Map<String, String> env, String cmd) {
    ProcessExecutor executor = new ProcessExecutor();
    if (env != null) {
      executor.environment(env);
    }
    try {
      return executor.command("/bin/sh", "-c", cmd).readOutput(true).execute().outputUTF8().replace("\n", "");
    } catch (Exception ex) {
      throw new UnexpectedException(format("Could not execute command %s, env %s", cmd, env));
    }
  }
  private String executeShellCommand(String cmd) {
    return executeShellCommand(null, cmd);
  }
}
