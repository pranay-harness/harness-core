package software.wings.helpers.ext.chartmuseum;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.AMAZON_S3_COMMAND_TEMPLATE;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.AWS_ACCESS_KEY_ID;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.AWS_SECRET_ACCESS_KEY;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.BUCKET_REGION_ERROR_CODE;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.CHART_MUSEUM_SERVER_START_RETRIES;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.INVALID_ACCESS_KEY_ID_ERROR;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.INVALID_ACCESS_KEY_ID_ERROR_CODE;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.NO_SUCH_BBUCKET_ERROR;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.NO_SUCH_BBUCKET_ERROR_CODE;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.PORTS_BOUND;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.PORTS_START_POINT;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.SIGNATURE_DOES_NOT_MATCH_ERROR;
import static software.wings.helpers.ext.chartmuseum.ChartMuseumConstants.SIGNATURE_DOES_NOT_MATCH_ERROR_CODE;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import software.wings.beans.AwsConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.core.ssh.executors.ScriptProcessExecutor.StringBufferOutputStream;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.settings.SettingValue;

import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChartMuseumClientImpl implements ChartMuseumClient {
  @Inject private K8sGlobalConfigService k8sGlobalConfigService;

  @Override
  public ChartMuseumServer startChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig)
      throws Exception {
    logger.info("Starting chart museum server");

    if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      return startAmazonS3ChartMuseumServer(helmRepoConfig, connectorConfig);
    }

    throw new WingsException("Unhandled type of helm repo config. Type : " + helmRepoConfig.getSettingType());
  }

  private ChartMuseumServer startAmazonS3ChartMuseumServer(HelmRepoConfig helmRepoConfig, SettingValue connectorConfig)
      throws Exception {
    AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) helmRepoConfig;
    AwsConfig awsConfig = (AwsConfig) connectorConfig;

    Map<String, String> environment = new HashMap<>();
    environment.put(AWS_ACCESS_KEY_ID, awsConfig.getAccessKey());
    environment.put(AWS_SECRET_ACCESS_KEY, new String(awsConfig.getSecretKey()));

    String evaluatedTemplate =
        AMAZON_S3_COMMAND_TEMPLATE.replace("${BUCKET_NAME}", amazonS3HelmRepoConfig.getBucketName())
            .replace("${FOLDER_PATH}", amazonS3HelmRepoConfig.getFolderPath())
            .replace("${REGION}", amazonS3HelmRepoConfig.getRegion());

    StringBuilder builder = new StringBuilder(128);
    builder.append(encloseWithQuotesIfNeeded(k8sGlobalConfigService.getChartMuseumPath()))
        .append(' ')
        .append(evaluatedTemplate);

    return startServer(builder.toString(), environment);
  }

  private ChartMuseumServer startServer(String command, Map<String, String> environment) throws Exception {
    int port = 0;
    StartedProcess process = null;
    int retries = 0;
    Random rand = new Random();
    StringBuffer stringBuffer = null;

    while (retries < CHART_MUSEUM_SERVER_START_RETRIES) {
      port = getNextRandomPort(rand);
      command = command.replace("${PORT}", Integer.toString(port));
      logger.info(format("Starting server at port %d. Retry #%s", port, retries));

      stringBuffer = new StringBuffer();
      process = startProcess(command, environment, stringBuffer);

      if (waitForServerReady(process, port)) {
        logger.info(stringBuffer.toString());
        break;
      } else {
        String processOutput = stringBuffer.toString();
        logger.info(processOutput);

        if (checkAddressInUseError(processOutput, port)) {
          retries++;
        } else {
          break;
        }
      }
    }

    if (process == null || !process.getProcess().isAlive()) {
      throw new WingsException(getErrorMessage(stringBuffer.toString()));
    }

    return ChartMuseumServer.builder().startedProcess(process).port(port).build();
  }

  private StartedProcess startProcess(String command, Map<String, String> environment, StringBuffer stringBuffer)
      throws Exception {
    try (StringBufferOutputStream stringBufferOutputStream = new StringBufferOutputStream(stringBuffer)) {
      return new ProcessExecutor()
          .environment(environment)
          .timeout(5, TimeUnit.MINUTES) // ToDo anshul fix this..reduce it to 1 ..also do we need it
          .commandSplit(command)
          .readOutput(true)
          .redirectOutput(stringBufferOutputStream)
          .redirectError(stringBufferOutputStream)
          .start();
    }
  }

  private int getNextRandomPort(Random rand) {
    return rand.nextInt(PORTS_BOUND) + PORTS_START_POINT;
  }

  public void stopChartMuseumServer(StartedProcess process) throws Exception {
    if (process != null) {
      process.getProcess().destroyForcibly().waitFor();
    }
  }

  private boolean waitForServerReady(StartedProcess process, int port) {
    int count = -1;

    while (count < 3) {
      logger.info("Waiting for chart museum server to get ready");
      count++;
      sleep(ofSeconds(5));

      if (!process.getProcess().isAlive()) {
        return false;
      }

      if (isPortInUse(port)) {
        return true;
      }
    }

    return false;
  }

  private boolean isPortInUse(int port) {
    // Assume no connection is possible.
    boolean result = false;

    try {
      (new Socket("localhost", port)).close();
      result = true;
    } catch (SocketException e) {
      // Could not connect.
    } catch (Exception e) {
      result = true;
    }

    return result;
  }

  private String getErrorMessage(String processOutput) {
    String errorPrefix = "Failed with error: ";

    if (processOutput.contains(NO_SUCH_BBUCKET_ERROR_CODE)) {
      return errorPrefix + NO_SUCH_BBUCKET_ERROR;
    }

    if (processOutput.contains(INVALID_ACCESS_KEY_ID_ERROR_CODE)) {
      return errorPrefix + INVALID_ACCESS_KEY_ID_ERROR;
    }

    if (processOutput.contains(SIGNATURE_DOES_NOT_MATCH_ERROR_CODE)) {
      return errorPrefix + SIGNATURE_DOES_NOT_MATCH_ERROR;
    }

    if (processOutput.contains(BUCKET_REGION_ERROR_CODE)) {
      String[] outputLines = processOutput.split(System.lineSeparator());

      for (String line : outputLines) {
        if (line.contains(BUCKET_REGION_ERROR_CODE)) {
          return errorPrefix + line.substring(line.indexOf(BUCKET_REGION_ERROR_CODE));
        }
      }

      return errorPrefix + BUCKET_REGION_ERROR_CODE;
    }

    return format("Could not start chart museum server. Failed after %s retries", CHART_MUSEUM_SERVER_START_RETRIES);
  }

  private boolean checkAddressInUseError(String processOutput, int port) {
    return processOutput.contains(format("listen tcp :%d: bind: address already in use", port));
  }
}
