package software.wings.helpers.ext.bamboo;

import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.SRE;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import okhttp3.Credentials;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.BambooConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by anubhaw on 11/29/16.
 */
@Singleton
public class BambooServiceImpl implements BambooService {
  private static final Logger logger = LoggerFactory.getLogger(BambooServiceImpl.class);

  @Inject private EncryptionService encryptionService;
  @Inject private TimeLimiter timeLimiter;

  private BambooRestClient getBambooClient(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(bambooConfig, encryptionDetails);
      String bambooUrl = bambooConfig.getBambooUrl();
      if (bambooUrl != null && !bambooUrl.endsWith("/")) {
        bambooUrl = bambooUrl + "/";
      }
      Retrofit retrofit = new Retrofit.Builder()
                              .baseUrl(bambooUrl)
                              .addConverterFactory(JacksonConverterFactory.create())
                              .client(Http.getUnsafeOkHttpClient(bambooUrl))
                              .build();
      return retrofit.create(BambooRestClient.class);
    } catch (Exception e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message",
              "Could not reach Bamboo Server at :" + bambooConfig.getBambooUrl()
                  + "Reason: " + ExceptionUtils.getRootCauseMessage(e));
    }
  }

  @Override
  public List<String> getJobKeys(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey) {
    logger.info("Retrieving job keys for plan key {}", planKey);
    Call<JsonNode> request =
        getBambooClient(bambooConfig, encryptionDetails)
            .listPlanWithJobDetails(
                Credentials.basic(bambooConfig.getUsername(), new String(bambooConfig.getPassword())), planKey);
    Response<JsonNode> response = null;
    try {
      response = getHttpRequestExecutionResponse(request);
      logger.info("Reading job keys for plan key {} success", planKey);
      return extractJobKeyFromNestedProjectResponseJson(response);
    } catch (Exception ex) {
      logger.error("Job keys fetch failed with exception", ex);
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      throw new WingsException(ARTIFACT_SERVER_ERROR, USER).addParam("message", Misc.getMessage(ex));
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig, encryptionDetails)
            .lastSuccessfulBuildForJob(getBasicAuthCredentials(bambooConfig, encryptionDetails), planKey);
    Response<JsonNode> response = null;
    try {
      response = getHttpRequestExecutionResponse(request);
      JsonNode resultNode = response.body().at("/results/result");
      if (resultNode != null && resultNode.elements().hasNext()) {
        JsonNode next = resultNode.elements().next();
        return aBuildDetails()
            .withNumber(next.get("buildNumber").asText())
            .withRevision(next.get("vcsRevisionKey").asText())
            .withBuildUrl(next.get("link").get("href").asText())
            .build();
      }
    } catch (Exception e) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error(format("Failed to get the last successful build for plan key %s", planKey), e);
      throw new WingsException(ARTIFACT_SERVER_ERROR, SRE).addParam("message", ExceptionUtils.getRootCauseMessage(e));
    }
    return null;
  }

  /**
   * Gets basic auth credentials.
   *
   * @param bambooConfig the bamboo config
   * @return the basic auth credentials
   */
  private String getBasicAuthCredentials(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(bambooConfig, encryptionDetails);
    return Credentials.basic(bambooConfig.getUsername(), new String(bambooConfig.getPassword()));
  }

  @Override
  public Map<String, String> getPlanKeys(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    return getPlanKeys(bambooConfig, encryptionDetails, 10000);
  }

  private Map<String, String> getPlanKeys(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, int maxResults) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        logger.info("Retrieving plan keys for bamboo server {}", bambooConfig);
        Call<JsonNode> request =
            getBambooClient(bambooConfig, encryptionDetails)
                .listProjectPlans(
                    Credentials.basic(bambooConfig.getUsername(), new String(bambooConfig.getPassword())), maxResults);
        Map<String, String> planNameMap = new HashMap<>();
        Response<JsonNode> response = null;
        try {
          response = getHttpRequestExecutionResponse(request);
          if (response.body() != null) {
            JsonNode planJsonNode = response.body().at("/plans/plan");
            planJsonNode.elements().forEachRemaining(jsonNode -> {
              String planKey = jsonNode.get("key").asText();
              String planName = jsonNode.get("shortName").asText();
              planNameMap.put(planKey, planName);
            });
          }
        } catch (Exception e) {
          if (response != null && !response.isSuccessful()) {
            IOUtils.closeQuietly(response.errorBody());
          }
          logger.error(format("Failed to fetch project plans from bamboo server %s", bambooConfig.getBambooUrl()), e);
          throw new WingsException(ARTIFACT_SERVER_ERROR, USER)
              .addParam("message", "Failed to load plans:" + ExceptionUtils.getRootCauseMessage(e));
        }
        logger.info("Retrieving plan keys for bamboo server {} success", bambooConfig);
        return planNameMap;
      }, 20L, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.warn("Bamboo server request did not succeed within 20 secs");
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Bamboo server took too long to respond");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(GENERAL_ERROR, e).addParam("message", Misc.getMessage(e));
    }
  }

  private Response<JsonNode> getHttpRequestExecutionResponse(Call<JsonNode> request) throws IOException {
    Response<JsonNode> response = request.execute();
    handleResponse(response);
    return response;
  }

  private void handleResponse(Response<?> response) throws IOException {
    if (response.isSuccessful()) {
      return;
    }
    if (response.code() == 401) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Invalid Bamboo credentials");
    }
    if (response.errorBody() == null) {
      throw new WingsException(ARTIFACT_SERVER_ERROR).addParam("message", response.message());
    }
    throw new WingsException(ARTIFACT_SERVER_ERROR).addParam("message", response.errorBody().string());
  }

  @Override
  public List<BuildDetails> getBuilds(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey, int maxNumberOfBuilds) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        List<BuildDetails> buildDetailsList = new ArrayList<>();
        Call<JsonNode> request =
            getBambooClient(bambooConfig, encryptionDetails)
                .listBuildsForJob(getBasicAuthCredentials(bambooConfig, encryptionDetails), planKey, maxNumberOfBuilds);
        Response<JsonNode> response = null;
        try {
          response = getHttpRequestExecutionResponse(request);
          if (response.body() != null) {
            JsonNode resultNode = response.body().at("/results/result");
            if (resultNode != null) {
              resultNode.elements().forEachRemaining(jsonNode -> {
                buildDetailsList.add(aBuildDetails()
                                         .withNumber(jsonNode.get("buildNumber").asText())
                                         .withRevision(jsonNode.get("vcsRevisionKey").asText())
                                         .build());
              });
            }
          }
          return buildDetailsList;
        } catch (Exception e) {
          if (response != null && !response.isSuccessful()) {
            IOUtils.closeQuietly(response.errorBody());
          }
          logger.error("BambooService job keys fetch failed with exception", e);
          throw new WingsException(ARTIFACT_SERVER_ERROR)
              .addParam("message",
                  "Error in fetching builds from bamboo server. Reason:" + ExceptionUtils.getRootCauseMessage(e));
        }
      }, 20L, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.warn("Bamboo server request did not succeed within 20 secs");
      throw new WingsException(INVALID_ARTIFACT_SERVER).addParam("message", "Bamboo server took too long to respond");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(GENERAL_ERROR, e).addParam("message", Misc.getMessage(e));
    }
  }

  @Override
  public List<String> getArtifactPath(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        List<String> artifactPaths = new ArrayList<>();
        BuildDetails lastSuccessfulBuild = getLastSuccessfulBuild(bambooConfig, encryptionDetails, planKey);
        if (lastSuccessfulBuild != null) {
          artifactPaths.addAll(getArtifactRelativePaths(
              getBuildArtifactsUrlMap(bambooConfig, encryptionDetails, planKey, lastSuccessfulBuild.getNumber())
                  .values()
                  .stream()
                  .map(Artifact::getLink)
                  .collect(toList())));
        }
        return artifactPaths;
      }, 20L, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.warn("Bamboo server request did not succeed within 20 secs");
      throw new WingsException(INVALID_ARTIFACT_SERVER).addParam("message", "Bamboo server took too long to respond");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(GENERAL_ERROR, e).addParam("message", Misc.getMessage(e));
    }
  }

  @Override
  public String triggerPlan(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey,
      Map<String, String> parameters) {
    logger.info("Trigger bamboo plan for Plan Key {} with parameters {}", planKey, String.valueOf(parameters));
    Response<JsonNode> response = null;
    String buildResultKey = null;
    try {
      if (parameters == null) {
        parameters = new HashMap<>();
      }
      // Replace all the parameters with
      Call<JsonNode> request =
          getBambooClient(bambooConfig, encryptionDetails)
              .triggerPlan(getBasicAuthCredentials(bambooConfig, encryptionDetails), planKey, parameters);
      response = getHttpRequestExecutionResponse(request);
      if (response.body() != null) {
        if (response.body().findValue("buildResultKey") != null) {
          buildResultKey = response.body().findValue("buildResultKey").asText();
        }
      }
      if (buildResultKey == null) {
        throw new WingsException(INVALID_ARTIFACT_SERVER)
            .addParam("message",
                "Failed to trigger bamboo plan [" + planKey + "]. Reason: buildResultKey does not exist in response");
      }
    } catch (Exception e) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error("Failed to trigger bamboo plan [" + planKey + "]", e);
      throw new WingsException(INVALID_ARTIFACT_SERVER)
          .addParam("message",
              "Failed to trigger bamboo plan [" + planKey + "]. Reason:" + ExceptionUtils.getRootCauseMessage(e));
    }
    logger.info(
        "Bamboo plan execution success for Plan Key {} with parameters {}", planKey, String.valueOf(parameters));
    return buildResultKey;
  }

  @Override
  public Result getBuildResult(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String buildResultKey) {
    Response<Result> response = null;
    try {
      Call<Result> request =
          getBambooClient(bambooConfig, encryptionDetails)
              .getBuildResult(getBasicAuthCredentials(bambooConfig, encryptionDetails), buildResultKey);
      response = request.execute();
      handleResponse(response);
      if (response.isSuccessful()) {
        Result result = response.body();
        if (result != null) {
          if (bambooConfig.getBambooUrl().endsWith("/")) {
            result.setBuildUrl(bambooConfig.getBambooUrl() + "browse/" + buildResultKey);
          } else {
            result.setBuildUrl(bambooConfig.getBambooUrl() + "/browse/" + buildResultKey);
          }
        }
        return response.body();
      }
    } catch (Exception e) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error("BambooService job keys fetch failed with exception", e);
      throw new WingsException(ARTIFACT_SERVER_ERROR)
          .addParam("message",
              "Failed to retrieve build status for [ " + buildResultKey
                  + "]. Reason:" + ExceptionUtils.getRootCauseMessage(e));
    }
    return Result.builder().build();
  }

  @Override
  public Status getBuildResultStatus(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String buildResultKey) {
    Response<Status> response = null;
    try {
      Call<Status> request =
          getBambooClient(bambooConfig, encryptionDetails)
              .getBuildResultStatus(getBasicAuthCredentials(bambooConfig, encryptionDetails), buildResultKey);
      response = request.execute();
      if (!response.isSuccessful()) {
        if (response.code() == 404) {
          return Status.builder().finished(true).build();
        }
      }
      return response.body();
    } catch (Exception e) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error("BambooService job keys fetch failed with exception", e);
      throw new WingsException(ARTIFACT_SERVER_ERROR, e)
          .addParam("message", "Failed to trigger bamboo plan " + buildResultKey);
    }
  }

  private List<String> getArtifactRelativePaths(Collection<String> paths) {
    return paths.stream().map(this ::extractRelativePath).filter(Objects::nonNull).collect(toList());
  }

  private String extractRelativePath(String path) {
    List<String> strings = asList(path.split("/"));
    int artifactIdx = strings.indexOf("artifact");
    if (artifactIdx >= 0 && artifactIdx + 2 < strings.size()) {
      artifactIdx += 2; // skip next path element jobShortId as well: "baseUrl/.../../artifact/jobShortId/{relativePath}
      return Joiner.on("/").join(strings.listIterator(artifactIdx));
    }
    return null;
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(BambooConfig bambooConfig,
      List<EncryptedDataDetail> encryptionDetails, String planKey, String buildNumber, String artifactPathRegex) {
    logger.info("Downloading artifact for plan {} and build number {} and artifact path {}", planKey, buildNumber,
        artifactPathRegex);
    Map<String, Artifact> artifactPathMap =
        getBuildArtifactsUrlMap(bambooConfig, encryptionDetails, planKey, buildNumber);
    String artifactSourcePath = artifactPathRegex;
    Pattern pattern = Pattern.compile(artifactPathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
    Set<Entry<String, Artifact>> artifactPathSet = artifactPathMap.entrySet();
    Entry<String, Artifact> artifactPath =
        artifactPathSet.stream()
            .filter(entry
                -> extractRelativePath(entry.getValue().getLink()) != null
                    && pattern.matcher(extractRelativePath(entry.getValue().getLink())).find())
            .findFirst()
            .orElse(null);
    if (artifactPath != null) {
      Artifact value = artifactPath.getValue();
      logger.info("Artifact Path regex {} matching with artifact path {}", artifactPathRegex, value);
      String link = value.getLink();
      try {
        URL url = new URL(link);
        URLConnection uc = url.openConnection();
        uc.setRequestProperty("Authorization", getBasicAuthCredentials(bambooConfig, encryptionDetails));
        logger.info("Artifact url {}", link);
        return ImmutablePair.of(link.substring(link.lastIndexOf('/') + 1), uc.getInputStream());
      } catch (IOException e) {
        String msg = "Failed to download the artifact from url [" + link + "]";
        logger.error(msg, e);
        throw new WingsException(ARTIFACT_SERVER_ERROR, e)
            .addParam("message", msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e));
      }
    } else {
      // It is not matching  direct url, so just prepare the url
      String msg =
          "Artifact path  [" + artifactPathRegex + "] not matching with any values: " + artifactPathMap.values();
      logger.info(msg);
      logger.info("Constructing url path to download");
      Artifact artifactJob = artifactPathMap.values()
                                 .stream()
                                 .filter(artifact -> artifact.getProducerJobKey() != null)
                                 .findFirst()
                                 .orElse(null);
      if (artifactJob != null) {
        String jobName = artifactJob.getProducerJobKey()
                             .replace(planKey, "")
                             .replace(buildNumber, "")
                             .replace("-", ""); // TOD-TOD-JOB1-80;
        String buildKey = planKey + "-" + buildNumber;
        String artifactUrl;
        if (bambooConfig.getBambooUrl().endsWith("/")) {
          artifactUrl = bambooConfig.getBambooUrl() + "browse/" + buildKey + "/artifact";
        } else {
          artifactUrl = bambooConfig.getBambooUrl() + "/browse/" + buildKey + "/artifact";
        }
        artifactUrl = artifactUrl + "/" + jobName + "/" + artifactSourcePath;
        logger.info("Constructed url {}", artifactUrl);
        try {
          URL url = new URL(artifactUrl);
          URLConnection uc = url.openConnection();
          uc.setRequestProperty("Authorization", getBasicAuthCredentials(bambooConfig, encryptionDetails));
          if (artifactSourcePath.contains("/")) {
            artifactSourcePath = artifactSourcePath.substring(artifactSourcePath.lastIndexOf('/') + 1);
          }
          return ImmutablePair.of(artifactSourcePath, uc.getInputStream());
        } catch (IOException e) {
          logger.error(format("Failed to download the artifact from url %s", artifactUrl), e);
          throw new WingsException(ARTIFACT_SERVER_ERROR, e)
              .addParam("message", msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e));
        }
      } else {
        throw new WingsException(ARTIFACT_SERVER_ERROR).addParam("message", msg);
      }
    }
  }

  @Override
  public boolean isRunning(BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails) {
    return getPlanKeys(bambooConfig, encryptionDetails, 1) != null; // TODO:: First check use status API
  }

  /**
   * Gets build artifacts url map.
   *
   * @param bambooConfig the bamboo config
   * @param planKey      the job key
   * @param buildNumber  the build number
   * @return the build artifacts url map
   */
  private Map<String, Artifact> getBuildArtifactsUrlMap(
      BambooConfig bambooConfig, List<EncryptedDataDetail> encryptionDetails, String planKey, String buildNumber) {
    logger.info("Retrieving artifacts from plan {} and build number {}", planKey, buildNumber);
    Call<JsonNode> request =
        getBambooClient(bambooConfig, encryptionDetails)
            .getBuildArtifacts(getBasicAuthCredentials(bambooConfig, encryptionDetails), planKey, buildNumber);
    Map<String, Artifact> artifactPathMap = new HashMap<>();
    Response<JsonNode> response;
    try {
      // stages.stage.results.result.artifacts.artifact
      response = getHttpRequestExecutionResponse(request);
      if (response.body() != null) {
        JsonNode stageNodes = response.body().at("/stages/stage");
        if (stageNodes != null) {
          stageNodes.elements().forEachRemaining(stageNode -> {
            JsonNode resultNodes = stageNode.at("/results/result");
            if (resultNodes != null) {
              resultNodes.elements().forEachRemaining(resultNode -> {
                JsonNode artifactNodes = resultNode.at("/artifacts/artifact");
                if (artifactNodes != null) {
                  artifactNodes.elements().forEachRemaining(artifactNode -> {
                    JsonNode hrefNode = artifactNode.at("/link/href");
                    JsonNode nameNode = artifactNode.get("name");
                    JsonNode producerJobKeyNode = artifactNode.get("producerJobKey");
                    if (hrefNode != null) {
                      Artifact artifact = Artifact.builder().name(nameNode.asText()).link(hrefNode.textValue()).build();
                      if (producerJobKeyNode != null) {
                        artifact.setProducerJobKey(producerJobKeyNode.asText());
                      }
                      artifactPathMap.put(nameNode.asText(), artifact);
                    }
                  });
                }
              });
            }
          });
        }
      }
      logger.info("Retrieving artifacts from plan {} and build number {} success", planKey, buildNumber);
      return artifactPathMap;
    } catch (IOException e) {
      throw new WingsException(ARTIFACT_SERVER_ERROR,
          format("Retrieving artifacts from plan %s and build number %s failed", planKey, buildNumber), e);
    }
  }

  private List<String> extractJobKeyFromNestedProjectResponseJson(Response<JsonNode> response) {
    List<String> jobKeys = new ArrayList<>();
    JsonNode planStages = response.body().at("/stages/stage");
    if (planStages != null) {
      planStages.elements().forEachRemaining(planStage -> {
        JsonNode stagePlans = planStage.at("/plans/plan");
        if (stagePlans != null) {
          stagePlans.elements().forEachRemaining(stagePlan -> { jobKeys.add(stagePlan.get("key").asText()); });
        }
      });
    }
    return jobKeys;
  }
}
