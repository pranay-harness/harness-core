package software.wings.helpers.ext.bamboo;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.base.Joiner;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.Credentials;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.ErrorCodes;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
                            .build();
    BambooRestClient bambooRestClient = retrofit.create(BambooRestClient.class);
    return bambooRestClient;
  }

  @Override
  public List<String> getJobKeys(BambooConfig bambooConfig, String planKey) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig)
            .listPlanWithJobDetails(Credentials.basic(bambooConfig.getUsername(), bambooConfig.getPassword()), planKey);
    try {
      Response<JsonNode> response = request.execute();
      return extractJobKeyFromNestedProjectResponseJson(response);
    } catch (Exception ex) {
      logger.error("Job keys fetch failed with exception " + ex);
      return new ArrayList<>();
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(BambooConfig bambooConfig, String planKey) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig).lastSuccessfulBuildForJob(getBasicAuthCredentials(bambooConfig), planKey);
    try {
      Response<JsonNode> response = request.execute();
      JsonNode jsonNode = response.body();
      return aBuildDetails()
          .withNumber(jsonNode.get("buildNumber").asInt())
          .withRevision(jsonNode.get("vcsRevisionKey").asText())
          .build();
    } catch (Exception ex) {
      logger.error("BambooService job keys fetch failed with exception " + ex.getStackTrace());
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
    return Credentials.basic(bambooConfig.getUsername(), bambooConfig.getPassword());
  }

  @Override
  public Map<String, String> getPlanKeys(BambooConfig bambooConfig) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig)
            .listProjectPlans(Credentials.basic(bambooConfig.getUsername(), bambooConfig.getPassword()));
    Map<String, String> planNameMap = new HashMap<>();

    try {
      Response<JsonNode> response = request.execute();
      JsonNode planJsonNode = response.body().at("/plans/plan");
      planJsonNode.elements().forEachRemaining(jsonNode -> {
        String planKey = jsonNode.get("key").asText();
        String planName = jsonNode.get("shortName").asText();
        planNameMap.put(planKey, planName);
      });
    } catch (Exception ex) {
      logger.error("Job keys fetch failed with exception " + ex.getStackTrace());
    }
    return planNameMap;
  }

  @Override
  public List<BuildDetails> getBuildsForJob(BambooConfig bambooConfig, String planKey, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetailsList = new ArrayList<>();

    Call<JsonNode> request = getBambooClient(bambooConfig)
                                 .listBuildsForJob(getBasicAuthCredentials(bambooConfig), planKey, maxNumberOfBuilds);
    try {
      Response<JsonNode> response = request.execute();
      JsonNode resultNode = response.body().at("/results/result");
      if (resultNode != null) {
        resultNode.elements().forEachRemaining(jsonNode -> {
          JsonNode nextNode = jsonNode.elements().next();
          buildDetailsList.add(aBuildDetails()
                                   .withNumber(nextNode.get("buildNumber").asInt())
                                   .withRevision(nextNode.get("vcsRevisionKey").asText())
                                   .build());
        });
      }
    } catch (Exception ex) {
      logger.error("BambooService job keys fetch failed with exception " + ex.getStackTrace());
    }
    return buildDetailsList;
  }

  @Override
  public List<String> getArtifactPath(BambooConfig bambooConfig, String planKey) {
    List<String> artifactPaths = new ArrayList<>();
    BuildDetails lastSuccessfulBuild = getLastSuccessfulBuild(bambooConfig, planKey);
    if (lastSuccessfulBuild != null) {
      Map<String, String> buildArtifactsUrlMap =
          getBuildArtifactsUrlMap(bambooConfig, planKey, Integer.toString(lastSuccessfulBuild.getNumber()));
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
    try {
      return ImmutablePair.of(artifactPath.getKey(), new URL(artifactPath.getValue()).openStream());
    } catch (IOException ex) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Invalid artifact path " + ex.getStackTrace());
    }
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
    try {
      // stages.stage.results.result.artifacts.artifact
      Response<JsonNode> response = request.execute();
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
      logger.error("Download artifact failed with exception " + ex.getStackTrace());
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
