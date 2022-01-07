/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.chartmuseum;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.chartmuseum.ChartMuseumConstants.AMAZON_S3_COMMAND_TEMPLATE;
import static io.harness.chartmuseum.ChartMuseumConstants.AWS_ACCESS_KEY_ID;
import static io.harness.chartmuseum.ChartMuseumConstants.AWS_SECRET_ACCESS_KEY;
import static io.harness.chartmuseum.ChartMuseumConstants.BUCKET_REGION_ERROR_CODE;
import static io.harness.chartmuseum.ChartMuseumConstants.CHART_MUSEUM_SERVER_START_RETRIES;
import static io.harness.chartmuseum.ChartMuseumConstants.GCS_COMMAND_TEMPLATE;
import static io.harness.chartmuseum.ChartMuseumConstants.GOOGLE_APPLICATION_CREDENTIALS;
import static io.harness.chartmuseum.ChartMuseumConstants.HEALTH_CHECK_TIME_GAP_SECONDS;
import static io.harness.chartmuseum.ChartMuseumConstants.INVALID_ACCESS_KEY_ID_ERROR;
import static io.harness.chartmuseum.ChartMuseumConstants.INVALID_ACCESS_KEY_ID_ERROR_CODE;
import static io.harness.chartmuseum.ChartMuseumConstants.NO_SUCH_BBUCKET_ERROR;
import static io.harness.chartmuseum.ChartMuseumConstants.NO_SUCH_BBUCKET_ERROR_CODE;
import static io.harness.chartmuseum.ChartMuseumConstants.PORTS_BOUND;
import static io.harness.chartmuseum.ChartMuseumConstants.PORTS_START_POINT;
import static io.harness.chartmuseum.ChartMuseumConstants.SERVER_HEALTH_CHECK_RETRIES;
import static io.harness.chartmuseum.ChartMuseumConstants.SIGNATURE_DOES_NOT_MATCH_ERROR;
import static io.harness.chartmuseum.ChartMuseumConstants.SIGNATURE_DOES_NOT_MATCH_ERROR_CODE;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.shell.ScriptProcessExecutor;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

@OwnedBy(CDP)
@Slf4j
@Singleton
public class ChartMuseumClientHelper {
  private static final SecureRandom random = new SecureRandom();

  @Inject private K8sGlobalConfigService k8sGlobalConfigService;

  public ChartMuseumServer startS3ChartMuseumServer(String bucket, String basePath, String region,
      boolean useEc2IamCredentials, char[] accessKey, char[] secretKey, boolean useIRSA,
      boolean useLatestChartMuseumVersion) throws Exception {
    Map<String, String> environment = getEnvForAwsConfig(accessKey, secretKey, useEc2IamCredentials, useIRSA);
    String evaluatedTemplate = AMAZON_S3_COMMAND_TEMPLATE.replace("${BUCKET_NAME}", bucket)
                                   .replace("${FOLDER_PATH}", basePath == null ? "" : basePath)
                                   .replace("${REGION}", region);

    StringBuilder builder = new StringBuilder(128);
    builder.append(encloseWithQuotesIfNeeded(k8sGlobalConfigService.getChartMuseumPath(useLatestChartMuseumVersion)))
        .append(' ')
        .append(evaluatedTemplate);

    return startServer(builder.toString(), environment);
  }

  public ChartMuseumServer startGCSChartMuseumServer(String bucket, String basePath, char[] serviceAccountKey,
      String resourceDirectory, boolean useLatestChartMuseumVersion) throws Exception {
    Map<String, String> environment = new HashMap<>();
    if (serviceAccountKey != null) {
      String credentialFilePath = writeGCSCredentialsFile(resourceDirectory, serviceAccountKey);
      environment.put(GOOGLE_APPLICATION_CREDENTIALS, credentialFilePath);
    }

    String evaluatedTemplate = GCS_COMMAND_TEMPLATE.replace("${BUCKET_NAME}", bucket)
                                   .replace("${FOLDER_PATH}", basePath == null ? "" : basePath);

    StringBuilder builder = new StringBuilder(128);
    builder.append(encloseWithQuotesIfNeeded(k8sGlobalConfigService.getChartMuseumPath(useLatestChartMuseumVersion)))
        .append(' ')
        .append(evaluatedTemplate);

    return startServer(builder.toString(), environment);
  }

  public void stopChartMuseumServer(StartedProcess process) {
    try {
      if (process != null) {
        process.getProcess().destroyForcibly().waitFor();
        log.info("Successfully stopped chart museum server");
      } else {
        log.info(
            "Not able to find any process associated with the chart museum server. It may have already been stopped");
      }
    } catch (Exception ex) {
      log.warn("Failed to stop chart museum server " + getMessage(ex));
    }
  }

  public ChartMuseumServer startServer(String command, Map<String, String> environment) throws Exception {
    int port = 0;
    StartedProcess process = null;
    int retries = 0;
    StringBuffer stringBuffer = null;

    log.info(command);

    while (retries < CHART_MUSEUM_SERVER_START_RETRIES) {
      port = getNextRandomPort(random);
      command = command.replace("${PORT}", Integer.toString(port));
      log.info("Starting server at port {}. Retry #{}", port, retries);

      stringBuffer = new StringBuffer();
      process = startProcess(command, environment, stringBuffer);

      if (waitForServerReady(process, port)) {
        log.info(stringBuffer.toString());
        break;
      } else {
        String processOutput = stringBuffer.toString();
        log.info(processOutput);

        if (checkAddressInUseError(processOutput, port)) {
          retries++;
        } else {
          break;
        }
      }
    }

    if (!isPortInUse(port)) {
      log.error("Port {} is still not in use", port);
    }

    if (process == null || !process.getProcess().isAlive()) {
      throw new InvalidRequestException(getErrorMessage(stringBuffer.toString()), WingsException.USER);
    }

    return ChartMuseumServer.builder().startedProcess(process).port(port).build();
  }

  @VisibleForTesting
  StartedProcess startProcess(String command, Map<String, String> environment, StringBuffer stringBuffer)
      throws Exception {
    try (ScriptProcessExecutor.StringBufferOutputStream stringBufferOutputStream =
             new ScriptProcessExecutor.StringBufferOutputStream(stringBuffer)) {
      return new ProcessExecutor()
          .environment(environment)
          .timeout(5, TimeUnit.MINUTES)
          .commandSplit(command)
          .readOutput(true)
          .redirectOutput(stringBufferOutputStream)
          .redirectError(stringBufferOutputStream)
          .start();
    }
  }

  @VisibleForTesting
  String writeGCSCredentialsFile(String resourceDirectory, char[] serviceAccountKey) throws IOException {
    String credentialFilePath = Paths.get(resourceDirectory, "credentials.json").toString();
    FileIo.writeUtf8StringToFile(credentialFilePath, String.valueOf(serviceAccountKey));
    return credentialFilePath;
  }

  @VisibleForTesting
  static Map<String, String> getEnvForAwsConfig(
      char[] accessKey, char[] secretKey, boolean useIamCredentials, boolean useIrsa) {
    Map<String, String> environment = new HashMap<>();
    if (useIrsa) {
      environment.put("AWS_ROLE_ARN", System.getenv("AWS_ROLE_ARN"));
      environment.put("AWS_WEB_IDENTITY_TOKEN_FILE", System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE"));
      environment.put("AWS_ROLE_SESSION_NAME", "aws-sdk-java-" + System.currentTimeMillis());

      // adding this env variable allows access to IRSA creds:
      environment.put("AWS_SDK_LOAD_CONFIG", "true");
    } else if (!useIamCredentials) {
      environment.put(AWS_ACCESS_KEY_ID, new String(accessKey));
      environment.put(AWS_SECRET_ACCESS_KEY, new String(secretKey));
    }
    return environment;
  }

  @VisibleForTesting
  static int getNextRandomPort(Random rand) {
    return rand.nextInt(PORTS_BOUND) + PORTS_START_POINT;
  }

  private static boolean waitForServerReady(StartedProcess process, int port) {
    int count = -1;

    while (count < SERVER_HEALTH_CHECK_RETRIES) {
      log.info("Waiting for chart museum server to get ready");
      count++;
      sleep(ofSeconds(HEALTH_CHECK_TIME_GAP_SECONDS));

      if (!process.getProcess().isAlive()) {
        return false;
      }

      if (isPortInUse(port)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isPortInUse(int port) {
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

  private static String getErrorMessage(String processOutput) {
    log.warn(String.format("Could not start Chart museum server. Process output: [%s]", processOutput));
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

  private static boolean checkAddressInUseError(String processOutput, int port) {
    return processOutput.contains(format("listen tcp :%d: bind: address already in use", port));
  }
}
