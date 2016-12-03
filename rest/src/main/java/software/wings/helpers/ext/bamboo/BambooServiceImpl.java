package software.wings.helpers.ext.bamboo;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

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
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

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
  public List<String> getJobKeys(BambooConfig bambooConfig) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig)
            .listProjectsWithJobDetails(Credentials.basic(bambooConfig.getUsername(), bambooConfig.getPassword()));
    try {
      Response<JsonNode> response = request.execute();
      return extractJobKeyFromNestedProjectResponseJson(response);
    } catch (Exception ex) {
      logger.error("Job keys fetch failed with exception " + ex);
      return new ArrayList<>();
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(BambooConfig bambooConfig, String jobKey) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig).lastSuccessfulBuild(Credentials.basic("admin", "admin"), jobKey);
    try {
      Response<JsonNode> response = request.execute();
      JsonNode jsonNode = response.body().at("/results/result");
      if (jsonNode != null && jsonNode.elements().hasNext()) {
        return aBuildDetails().withNumber(jsonNode.elements().next().get("buildNumber").asInt()).build();
      }
    } catch (Exception ex) {
      logger.error("BambooService job keys fetch failed with exception " + ex);
    }
    return null;
  }

  @Override
  public List<BuildDetails> getBuildsForJob(BambooConfig bambooConfig, String jobKey, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetailsList = new ArrayList<>();

    Call<JsonNode> request =
        getBambooClient(bambooConfig).listBuildsForJob(Credentials.basic("admin", "admin"), jobKey, maxNumberOfBuilds);
    try {
      Response<JsonNode> response = request.execute();
      JsonNode resultNode = response.body().at("/results/result");
      if (resultNode != null) {
        resultNode.elements().forEachRemaining(jsonNode -> {
          buildDetailsList.add(aBuildDetails().withNumber(jsonNode.get("buildNumber").asInt()).build());
        });
      }
    } catch (Exception ex) {
      logger.error("BambooService job keys fetch failed with exception " + ex);
    }
    return buildDetailsList;
  }

  @Override
  public List<String> getArtifactPath(BambooConfig bambooConfig, String jobName) {
    return Arrays.asList("*"); // TODO:: find a way to get artifact paths
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(
      BambooConfig bambooConfig, String jobKey, String buildNumber, String artifactPathRegex) {
    Call<JsonNode> request =
        getBambooClient(bambooConfig).getBuildArtifacts(Credentials.basic("admin", "admin"), jobKey, buildNumber);

    Map<String, String> artifactPathMap = new HashMap<>();
    try {
      Response<JsonNode> response = request.execute();
      JsonNode artifactsNode = response.body().at("/artifacts/artifact");
      if (artifactsNode != null) {
        artifactsNode.elements().forEachRemaining(artifactNode -> {
          JsonNode hrefNode = artifactNode.at("/link/href");
          JsonNode nameNode = artifactNode.get("name");
          if (hrefNode != null) {
            artifactPathMap.put(nameNode.asText(), hrefNode.textValue());
          }
        });
      }
      Pattern pattern = Pattern.compile(artifactPathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
      Entry<String, String> artifactPath = artifactPathMap.entrySet()
                                               .stream()
                                               .filter(entry -> pattern.matcher(entry.getValue()).matches())
                                               .findFirst()
                                               .orElse(null);
      return ImmutablePair.of(artifactPath.getKey(), new URL(artifactPath.getValue()).openStream());
    } catch (IOException ex) {
      logger.error("Download artifact failed with exception " + ex.getStackTrace());
    }
    return null;
  }

  private List<String> extractJobKeyFromNestedProjectResponseJson(Response<JsonNode> response) {
    List<String> jobKeys = new ArrayList<>();
    JsonNode projects = response.body().at("/projects/project");
    if (projects != null) {
      projects.elements().forEachRemaining(projectNode -> {
        JsonNode projectPlans = projectNode.at("/plans/plan");
        if (projectPlans != null) {
          projectPlans.elements().forEachRemaining(projectPlan -> {
            JsonNode planStages = projectPlan.at("/stages/stage");
            if (planStages != null) {
              planStages.elements().forEachRemaining(planStage -> {
                JsonNode stagePlans = planStage.at("/plans/plan");
                if (stagePlans != null) {
                  stagePlans.elements().forEachRemaining(stagePlan -> { jobKeys.add(stagePlan.get("key").asText()); });
                }
              });
            }
          });
        }
      });
    }
    return jobKeys;
  }
}
