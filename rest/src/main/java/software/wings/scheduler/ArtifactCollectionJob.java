package software.wings.scheduler;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/8/16.
 */
public class ArtifactCollectionJob implements Job {
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private ExecutorService executorService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString("artifactStreamId");
    String appId = jobExecutionContext.getMergedJobDataMap().getString("appId");
    executorService.submit(() -> collectNewArtifactsFromArtifactStream(appId, artifactStreamId));
  }

  private void collectNewArtifactsFromArtifactStream(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    Validator.notNullCheck("artifactStream", artifactStream);

    if (artifactStream.getArtifactStreamType().equals(DOCKER.name())) {
      List<BuildDetails> builds = buildSourceService.getBuilds(appId, artifactStreamId, artifactStream.getSettingId());
      List<Artifact> artifacts = artifactService
                                     .list(aPageRequest()
                                               .addFilter("appId", EQ, appId)
                                               .addFilter("artifactStreamId", EQ, artifactStreamId)
                                               .build(),
                                         false)
                                     .getResponse();

      Map<String, String> existingBuilds =
          artifacts.stream().collect(Collectors.toMap(a -> a.getMetadata().get("buildNo"), a -> a.getUuid()));

      builds.forEach(buildDetails -> {
        if (!existingBuilds.containsKey(buildDetails.getNumber())) {
          logger.info(
              "New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. Add entry in Artifact collection",
              buildDetails.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
          Artifact artifact = anArtifact()
                                  .withAppId(appId)
                                  .withArtifactStreamId(artifactStreamId)
                                  .withDisplayName(artifactStream.getArtifactDisplayName(buildDetails.getNumber()))
                                  .withMetadata(ImmutableMap.of("buildNo", buildDetails.getNumber()))
                                  .withRevision(buildDetails.getRevision())
                                  .build();
          artifactService.create(artifact);
        }
      });
    } else {
      BuildDetails lastSuccessfulBuild =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());

      if (lastSuccessfulBuild != null) {
        Artifact lastCollectedArtifact = artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId);
        int buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get("buildNo") != null)
            ? Integer.parseInt(lastCollectedArtifact.getMetadata().get("buildNo"))
            : 0;
        if (Integer.parseInt(lastSuccessfulBuild.getNumber()) > buildNo) {
          logger.info(
              "Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
              buildNo, lastSuccessfulBuild.getNumber(), artifactStreamId);
          Artifact artifact =
              anArtifact()
                  .withAppId(appId)
                  .withArtifactStreamId(artifactStreamId)
                  .withDisplayName(artifactStream.getArtifactDisplayName(lastSuccessfulBuild.getNumber()))
                  .withMetadata(ImmutableMap.of("buildNo", lastSuccessfulBuild.getNumber()))
                  .withRevision(lastSuccessfulBuild.getRevision())
                  .build();
          artifactService.create(artifact);
        }
      }
    }
  }
}
