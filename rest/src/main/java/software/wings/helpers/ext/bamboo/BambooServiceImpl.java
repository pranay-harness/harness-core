package software.wings.helpers.ext.bamboo;

import static org.awaitility.Awaitility.with;
import static org.hamcrest.CoreMatchers.notNullValue;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.Credentials;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.ErrorCode;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.HttpUtil;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 11/29/16.
 */
@Singleton
public class BambooServiceImpl implements BambooService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  private BambooRestClient getBambooClient(BambooConfig bambooConfig) {
    try {
      String bambooUrl = bambooConfig.getBambooUrl();
      if (bambooUrl != null && !bambooUrl.endsWith("/")) {
        bambooUrl = bambooUrl + "/";
      }
      Retrofit retrofit = new Retrofit.Builder()
                              .baseUrl(bambooUrl)
                              .addConverterFactory(JacksonConverterFactory.create())
                              .client(HttpUtil.getUnsafeOkHttpClient())
                              .build();
      BambooRestClient bambooRestClient = retrofit.create(BambooRestClient.class);
      return bambooRestClient;
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message",
          "Could not reach Bamboo Server at :" + bambooConfig.getBambooUrl()
              + "Reason: " + ExceptionUtils.getRootCauseMessage(e));
    }
  }

  @Override
  public List<String> getJobKeys(BambooConfig bambooConfig, String planKey) {
    logger.info("Retrieving job keys for plan key {}", planKey);
    Call<JsonNode> request =
        getBambooClient(bambooConfig)
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
      throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "message", ex.getMessage());
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(BambooConfig bambooConfig, String planKey) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig).lastSuccessfulBuildForJob(getBasicAuthCredentials(bambooConfig), planKey);
    Response<JsonNode> response = null;
    try {
      response = getHttpRequestExecutionResponse(request);
      JsonNode resultNode = response.body().at("/results/result");
      if (resultNode != null && resultNode.elements().hasNext()) {
        JsonNode next = resultNode.elements().next();
        return aBuildDetails()
            .withNumber(next.get("buildNumber").asText())
            .withRevision(next.get("vcsRevisionKey").asText())
            .build();
      }
    } catch (Exception e) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error("Failed to get the last successful build for plan key {}", e);
      throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "message", ExceptionUtils.getRootCauseMessage(e));
    }
    return null;
  }

  /**
   * Gets basic auth credentials.
   *
   * @param bambooConfig the bamboo config
   * @return the basic auth credentials
   */
  public String getBasicAuthCredentials(BambooConfig bambooConfig) {
    return Credentials.basic(bambooConfig.getUsername(), new String(bambooConfig.getPassword()));
  }

  @Override
  public Map<String, String> getPlanKeys(BambooConfig bambooConfig) {
    return getPlanKeys(bambooConfig, 10000);
  }

  public Map<String, String> getPlanKeys(BambooConfig bambooConfig, int maxResults) {
    try {
      return with().atMost(new Duration(20L, TimeUnit.SECONDS)).until(() -> {
        logger.info("Retrieving plan keys for bamboo server {}", bambooConfig);
        Call<JsonNode> request =
            getBambooClient(bambooConfig)
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
        } catch (WingsException e) {
          throw e;
        } catch (IOException e) {
          if (response != null && !response.isSuccessful()) {
            IOUtils.closeQuietly(response.errorBody());
          }
          logger.error("Failed to fetch project plans from bamboo server {}", bambooConfig.getBambooUrl(), e);
          throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "message",
              "Failed to load plans:" + ExceptionUtils.getRootCauseMessage(e));
        }
        logger.info("Retrieving plan keys for bamboo server {} success", bambooConfig);
        return planNameMap;
      }, notNullValue());
    } catch (ConditionTimeoutException e) {
      logger.warn("Bamboo server request did not succeed within 20 secs", e);
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Bamboo server took too long to respond");
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
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Bamboo credentials");
    }
    if (response.errorBody() == null) {
      throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "message", response.message());
    }
    throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "message", response.errorBody().string());
  }

  @Override
  public List<BuildDetails> getBuilds(BambooConfig bambooConfig, String planKey, int maxNumberOfBuilds) {
    try {
      return with().atMost(new Duration(20L, TimeUnit.SECONDS)).until(() -> {
        List<BuildDetails> buildDetailsList = new ArrayList<>();
        Call<JsonNode> request =
            getBambooClient(bambooConfig)
                .listBuildsForJob(getBasicAuthCredentials(bambooConfig), planKey, maxNumberOfBuilds);
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
          throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "message",
              "Error in fetching builds from bamboo server. Reason:" + ExceptionUtils.getRootCauseMessage(e));
        }
      }, notNullValue());
    } catch (ConditionTimeoutException e) {
      logger.warn("Bamboo server request did not succeed within 20 secs", e);
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Bamboo server took too long to respond");
    }
  }

  @Override
  public List<String> getArtifactPath(BambooConfig bambooConfig, String planKey) {
    try {
      return with().atMost(new Duration(20L, TimeUnit.SECONDS)).until(() -> {
        List<String> artifactPaths = new ArrayList<>();
        BuildDetails lastSuccessfulBuild = getLastSuccessfulBuild(bambooConfig, planKey);
        if (lastSuccessfulBuild != null) {
          Map<String, Artifact> buildArtifactsUrlMap =
              getBuildArtifactsUrlMap(bambooConfig, planKey, lastSuccessfulBuild.getNumber());
          artifactPaths.addAll(getArtifactRelativePaths(
              buildArtifactsUrlMap.values().stream().map(Artifact::getLink).collect(Collectors.toList())));
        }
        return artifactPaths;

      }, notNullValue());
    } catch (ConditionTimeoutException e) {
      logger.warn("Bamboo server request did not succeed within 20 secs", e);
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Bamboo server took too long to respond");
    }
  }

  @Override
  public String triggerPlan(BambooConfig bambooConfig, String planKey, Map<String, String> parameters) {
    logger.info("Trigger bamboo plan for Plan Key {} with parameters {}", planKey, String.valueOf(parameters));
    Response<JsonNode> response = null;
    String buildResultKey = null;
    try {
      if (parameters == null) {
        parameters = new HashMap<>();
      }
      // Replace all the parameters with
      Call<JsonNode> request =
          getBambooClient(bambooConfig).triggerPlan(getBasicAuthCredentials(bambooConfig), planKey, parameters);
      response = getHttpRequestExecutionResponse(request);
      if (response.body() != null) {
        if (response.body().findValue("buildResultKey") != null) {
          buildResultKey = response.body().findValue("buildResultKey").asText();
        }
      }
      if (buildResultKey == null) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message",
            "Failed to trigger bamboo plan [" + planKey + "]. Reason: buildResultKey does not exist in response");
      }
    } catch (Exception e) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error("Failed to trigger bamboo plan [" + planKey + "]", e);
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message",
          "Failed to trigger bamboo plan [" + planKey + "]. Reason:" + ExceptionUtils.getRootCauseMessage(e));
    }
    logger.info(
        "Bamboo plan execution success for Plan Key {} with parameters {}", planKey, String.valueOf(parameters));
    return buildResultKey;
  }

  @Override
  public Result getBuildResult(BambooConfig bambooConfig, String buildResultKey) {
    Response<Result> response = null;
    try {
      Call<Result> request =
          getBambooClient(bambooConfig).getBuildResult(getBasicAuthCredentials(bambooConfig), buildResultKey);
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
      throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "message",
          "Failed to retrieve build status for [ " + buildResultKey
              + "]. Reason:" + ExceptionUtils.getRootCauseMessage(e));
    }
    return Result.builder().build();
  }

  @Override
  public Status getBuildResultStatus(BambooConfig bambooConfig, String buildResultKey) {
    Response<Status> response = null;
    try {
      Call<Status> request =
          getBambooClient(bambooConfig).getBuildResultStatus(getBasicAuthCredentials(bambooConfig), buildResultKey);
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
      throw new WingsException(
          ErrorCode.ARTIFACT_SERVER_ERROR, "message", "Failed to trigger bamboo plan " + buildResultKey, e);
    }
  }

  private List<String> getArtifactRelativePaths(Collection<String> paths) {
    return paths.stream().map(this ::extractRelativePath).filter(Objects::nonNull).collect(Collectors.toList());
  }

  private String extractRelativePath(String path) {
    List<String> strings = Arrays.asList(path.split("/"));
    int artifactIdx = strings.indexOf("artifact");
    if (artifactIdx >= 0 && artifactIdx + 2 < strings.size()) {
      artifactIdx += 2; // skip next path element jobShortId as well: "baseUrl/.../../artifact/jobShortId/{relativePath}
      String relativePath = Joiner.on("/").join(strings.listIterator(artifactIdx));
      return relativePath;
    }
    return null;
  }

  //  @Override
  //  public Pair<String, InputStream> downloadArtifact(BambooConfig bambooConfig, String planKey, String buildNumber,
  //  String artifactPathRegex) {
  //    Map<String, String> artifactPathMap = getBuildArtifactsUrlMap(bambooConfig, planKey, buildNumber);
  //    Pattern pattern = Pattern.compile(artifactPathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
  //    Entry<String, String> artifactPath = artifactPathMap.entrySet().stream()
  //        .filter(entry -> extractRelativePath(entry.getValue()) != null &&
  //        pattern.matcher(extractRelativePath(entry.getValue())).matches()).findFirst() .orElse(null);
  //    try {
  //      return ImmutablePair.of(artifactPath.getKey(), new URL(artifactPath.getValue()).openStream());
  //    } catch (IOException ex) {
  //      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid artifact path " + ex.getStackTrace());
  //    }
  //  }

  @Override
  public ListNotifyResponseData downloadArtifacts(BambooConfig bambooConfig, String planKey, String buildNumber,
      List<String> artifactPaths, String delegateId, String taskId, String accountId)
      throws IOException, URISyntaxException {
    ListNotifyResponseData res = new ListNotifyResponseData();

    for (String artifactPath : artifactPaths) {
      downloadArtifactFromABuild(bambooConfig, planKey, buildNumber, artifactPath, res, delegateId, taskId, accountId);
    }
    return res;
  }

  private void downloadArtifactFromABuild(BambooConfig bambooConfig, String planKey, String buildNumber,
      String artifactPathRegex, ListNotifyResponseData res, String delegateId, String taskId, String accountId)
      throws IOException, URISyntaxException {
    logger.info("Downloading artifact for plan {} and build number {} and artifact path {}", planKey, buildNumber,
        artifactPathRegex);
    Map<String, Artifact> artifactPathMap = getBuildArtifactsUrlMap(bambooConfig, planKey, buildNumber);
    Pattern pattern = Pattern.compile(artifactPathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
    Set<Entry<String, Artifact>> artifactPathSet = artifactPathMap.entrySet();
    Set<Entry<String, Artifact>> artifactSet =
        artifactPathSet.stream()
            .filter(entry
                -> extractRelativePath(entry.getValue().getLink()) != null
                    && pattern.matcher(extractRelativePath(entry.getValue().getLink())).find())
            .collect(Collectors.toSet());

    for (Entry<String, Artifact> artifactEntry : artifactSet) {
      Pair<String, InputStream> stringInputStreamPair =
          downloadArtifact(bambooConfig, planKey, buildNumber, artifactPathRegex, artifactPathMap, artifactEntry);
      artifactCollectionTaskHelper.addDataToResponse(
          stringInputStreamPair, artifactPathRegex, res, delegateId, taskId, accountId);
    }
  }

  private Pair<String, InputStream> downloadArtifact(BambooConfig bambooConfig, String planKey, String buildNumber,
      String artifactPathRegex, Map<String, Artifact> artifactPathMap, Entry<String, Artifact> artifactEntry) {
    String artifactSourcePath = artifactPathRegex;
    if (artifactEntry != null) {
      Artifact value = artifactEntry.getValue();
      logger.info("Artifact Path regex {} matching with artifact path {}", artifactPathRegex, value);
      String link = value.getLink();
      try {
        URL url = new URL(link);
        URLConnection uc = url.openConnection();
        uc.setRequestProperty("Authorization", getBasicAuthCredentials(bambooConfig));
        logger.info("Artifact url {}", link);
        return ImmutablePair.of(link.substring(link.lastIndexOf("/") + 1), uc.getInputStream());
      } catch (IOException e) {
        String msg = "Failed to download the artifact from url [" + link + "]";
        logger.error(msg, e);
        throw new WingsException(
            ErrorCode.ARTIFACT_SERVER_ERROR, "message", msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e), e);
      }
    } else {
      // It is not matching  direct url, so just prepare the url
      String msg = "Artifact path  [" + artifactPathRegex
          + "] not matching with any values: " + String.valueOf(artifactPathMap.values());
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
        logger.info("Constructed url {]", artifactUrl);
        try {
          URL url = new URL(artifactUrl);
          URLConnection uc = url.openConnection();
          uc.setRequestProperty("Authorization", getBasicAuthCredentials(bambooConfig));
          if (artifactSourcePath.contains("/")) {
            artifactSourcePath = artifactSourcePath.substring(artifactSourcePath.lastIndexOf("/") + 1);
          }
          return ImmutablePair.of(artifactSourcePath, uc.getInputStream());
        } catch (IOException e) {
          logger.error("Failed to download the artifact from url {}", artifactUrl, e);
          throw new WingsException(
              ErrorCode.ARTIFACT_SERVER_ERROR, "message", msg + "Reason:" + ExceptionUtils.getRootCauseMessage(e), e);
        }
      } else {
        throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "message", msg);
      }
    }
  }

  @Override
  public boolean isRunning(BambooConfig bambooConfig) {
    return getPlanKeys(bambooConfig, 1) != null; // TODO:: First check use status API
  }

  /**
   * Gets build artifacts url map.
   *
   * @param bambooConfig the bamboo config
   * @param planKey      the job key
   * @param buildNumber  the build number
   * @return the build artifacts url map
   */
  private Map<String, Artifact> getBuildArtifactsUrlMap(BambooConfig bambooConfig, String planKey, String buildNumber) {
    logger.info("Retrieving artifacts from plan {} and build number {}", planKey, buildNumber);
    Call<JsonNode> request =
        getBambooClient(bambooConfig).getBuildArtifacts(getBasicAuthCredentials(bambooConfig), planKey, buildNumber);
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
      logger.info("Retrieving artifacts from plan {} and build number {} failed", planKey, buildNumber, e);
      throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR, "Failed to download artifact", e);
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

  public static void main(String... args) throws Exception {
    String url = "http:/dv0127d.chicago.cme.com:8085/"; //"https://127.0.0.1:8000"; //;

    //  url = "https://localhost:8000";

    BambooConfig bambooConfig = BambooConfig.Builder.aBambooConfig()
                                    .withBambooUrl(url)
                                    .withUsername("wingsbuild")
                                    .withPassword("0db28aa0f4fc0685df9a216fc7af0ca96254b7c2".toCharArray())
                                    .build();
    BambooServiceImpl bambooService = new BambooServiceImpl();

    bambooService.isRunning(bambooConfig);
    // Get all Plan Keys
    // Map<String, String> planKeys = bambooService.getPlanKeys(bambooConfig);

    // planKeys.forEach((s, s2) -> System.out.println("s = " + s));

    // Trigger Plan
    // bambooService.triggerPlan(bambooConfig, "TOD-TOD", new HashMap<>());
    // Get the build status result
    // System.out.println("bambooService status = " + bambooService.getBuildResultStatus(bambooConfig, "TOD-TOD-49"));

    // System.out.println("bambooService result = " + bambooService.getBuildResult(bambooConfig, "TOD-TOD-49"));
  }
}
