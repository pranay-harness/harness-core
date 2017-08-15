package software.wings.helpers.ext.bamboo;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.base.Joiner;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.Credentials;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 11/29/16.
 */
@Singleton
public class BambooServiceImpl implements BambooService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private BambooRestClient getBambooClient(BambooConfig bambooConfig) {
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(bambooConfig.getBambooUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .client(HttpUtil.getUnsafeOkHttpClient())
                            .build();
    BambooRestClient bambooRestClient = retrofit.create(BambooRestClient.class);
    return bambooRestClient;
  }

  @Override
  public List<String> getJobKeys(BambooConfig bambooConfig, String planKey) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig)
            .listPlanWithJobDetails(
                Credentials.basic(bambooConfig.getUsername(), new String(bambooConfig.getPassword())), planKey);
    Response<JsonNode> response = null;
    try {
      response = getHttpRequestExecutionResponse(request);
      return extractJobKeyFromNestedProjectResponseJson(response);
    } catch (Exception ex) {
      logger.error("Job keys fetch failed with exception", ex);
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      return new ArrayList<>();
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
    } catch (Exception ex) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error("BambooService job keys fetch failed with exception", ex);
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
    Call<JsonNode> request =
        getBambooClient(bambooConfig)
            .listProjectPlans(Credentials.basic(bambooConfig.getUsername(), new String(bambooConfig.getPassword())));
    Map<String, String> planNameMap = new HashMap<>();
    Response<JsonNode> response = null;

    try {
      response = getHttpRequestExecutionResponse(request);
      JsonNode planJsonNode = response.body().at("/plans/plan");
      planJsonNode.elements().forEachRemaining(jsonNode -> {
        String planKey = jsonNode.get("key").asText();
        String planName = jsonNode.get("shortName").asText();
        planNameMap.put(planKey, planName);
      });
    } catch (IOException ex) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error("Job keys fetch failed with exception", ex);
      throw new WingsException(
          ErrorCode.UNKNOWN_ERROR, "message", "Error in fetching project plans from bamboo server", ex);
    }
    return planNameMap;
  }

  private Response<JsonNode> getHttpRequestExecutionResponse(Call<JsonNode> request) throws IOException {
    Response<JsonNode> response = request.execute();

    if (response.code() != 200) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Bamboo credentials");
    }

    return response;
  }

  @Override
  public List<BuildDetails> getBuilds(BambooConfig bambooConfig, String planKey, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetailsList = new ArrayList<>();

    Call<JsonNode> request = getBambooClient(bambooConfig)
                                 .listBuildsForJob(getBasicAuthCredentials(bambooConfig), planKey, maxNumberOfBuilds);
    Response<JsonNode> response = null;
    try {
      response = getHttpRequestExecutionResponse(request);
      JsonNode resultNode = response.body().at("/results/result");
      if (resultNode != null) {
        resultNode.elements().forEachRemaining(jsonNode -> {
          buildDetailsList.add(aBuildDetails()
                                   .withNumber(jsonNode.get("buildNumber").asText())
                                   .withRevision(jsonNode.get("vcsRevisionKey").asText())
                                   .build());
        });
      }
    } catch (IOException ex) {
      if (response != null && !response.isSuccessful()) {
        IOUtils.closeQuietly(response.errorBody());
      }
      logger.error("BambooService job keys fetch failed with exception", ex);
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "message", "Error in fetching builds from bamboo server", ex);
    }
    return buildDetailsList;
  }

  @Override
  public List<String> getArtifactPath(BambooConfig bambooConfig, String planKey) {
    List<String> artifactPaths = new ArrayList<>();
    BuildDetails lastSuccessfulBuild = getLastSuccessfulBuild(bambooConfig, planKey);
    if (lastSuccessfulBuild != null) {
      Map<String, String> buildArtifactsUrlMap =
          getBuildArtifactsUrlMap(bambooConfig, planKey, lastSuccessfulBuild.getNumber());
      artifactPaths.addAll(getArtifactRelativePaths(buildArtifactsUrlMap.values()));
    }
    return artifactPaths;
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
  public Pair<String, InputStream> downloadArtifact(
      BambooConfig bambooConfig, String planKey, String buildNumber, String artifactPathRegex) {
    Map<String, String> artifactPathMap = getBuildArtifactsUrlMap(bambooConfig, planKey, buildNumber);
    Pattern pattern = Pattern.compile(artifactPathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
    Entry<String, String> artifactPath =
        artifactPathMap.entrySet()
            .stream()
            .filter(entry
                -> extractRelativePath(entry.getValue()) != null
                    && pattern.matcher(extractRelativePath(entry.getValue())).matches())
            .findFirst()
            .orElse(null);
    //    artifactPath.setValue("http://ec2-34-202-14-12.compute-1.amazonaws.com:8085/browse/TOD-TOD-39/artifact/JOB1/artifacts/todolist.war");
    try {
      //      try {
      //        Response<ResponseBody> response =
      //        getBambooClient(bambooConfig).downloadArtifact(getBasicAuthCredentials(bambooConfig),
      //        artifactPath.getValue()).execute(); File file = new File("/tmp/todolist.war"); FileOutputStream
      //        fileOutputStream = new FileOutputStream(file); IOUtils.write(response.body().bytes(), fileOutputStream);
      //      }
      //      catch (Exception ex){
      //        ex.printStackTrace();
      //      }

      URL url = new URL(artifactPath.getValue());
      URLConnection uc = url.openConnection();
      uc.setRequestProperty("Authorization", getBasicAuthCredentials(bambooConfig));
      return ImmutablePair.of(artifactPath.getKey(), uc.getInputStream());
    } catch (IOException ex) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Invalid artifact path " + ex.getStackTrace());
    }
  }
  @Override
  public boolean isRunning(BambooConfig bambooConfig) {
    return getPlanKeys(bambooConfig) != null; // TODO:: use status API
  }

  /**
   * Gets build artifacts url map.
   *
   * @param bambooConfig the bamboo config
   * @param planKey      the job key
   * @param buildNumber  the build number
   * @return the build artifacts url map
   */
  public Map<String, String> getBuildArtifactsUrlMap(BambooConfig bambooConfig, String planKey, String buildNumber) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig).getBuildArtifacts(getBasicAuthCredentials(bambooConfig), planKey, buildNumber);
    Map<String, String> artifactPathMap = new HashMap<>();
    Response<JsonNode> response = null;
    try {
      // stages.stage.results.result.artifacts.artifact
      response = getHttpRequestExecutionResponse(request);
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
                  if (hrefNode != null) {
                    artifactPathMap.put(nameNode.asText(), hrefNode.textValue());
                  }
                });
              }
            });
          }
        });
      }
    } catch (IOException ex) {
      logger.error("Download artifact failed with exception", ex);
    }
    return artifactPathMap;
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
