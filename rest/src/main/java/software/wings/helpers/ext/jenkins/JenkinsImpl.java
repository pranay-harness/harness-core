package software.wings.helpers.ext.jenkins;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.ExtractHeader;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * The Class JenkinsImpl.
 */
public class JenkinsImpl implements Jenkins {
  private JenkinsServer jenkinsServer;

  /**
   * Instantiates a new jenkins impl.
   *
   * @param jenkinsUrl the jenkins url
   * @throws URISyntaxException the URI syntax exception
   */
  @AssistedInject
  public JenkinsImpl(@Assisted(value = "url") String jenkinsUrl) throws URISyntaxException {
    jenkinsServer = new JenkinsServer(new URI(jenkinsUrl));
  }

  /**
   * Instantiates a new jenkins impl.
   *
   * @param jenkinsUrl the jenkins url
   * @param username   the username
   * @param password   the password
   * @throws URISyntaxException the URI syntax exception
   */
  @AssistedInject
  public JenkinsImpl(@Assisted(value = "url") String jenkinsUrl, @Assisted(value = "username") String username,
      @Assisted(value = "password") String password) throws URISyntaxException {
    jenkinsServer = new JenkinsServer(new URI(jenkinsUrl), username, password);
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#getJob(java.lang.String)
   */
  @Override
  public JobWithDetails getJob(String jobname) throws IOException {
    return jenkinsServer.getJob(jobname);
  }

  @Override
  public Map<String, Job> getJobs() throws IOException {
    return jenkinsServer.getJobs();
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#getBuildsForJob(java.lang.String, int)
   */
  @Override
  public List<BuildDetails> getBuildsForJob(String jobname, int lastN) throws IOException {
    JobWithDetails jobWithDetails = getJob(jobname);
    if (jobWithDetails == null) {
      return null;
    }
    return jobWithDetails.getBuilds()
        .parallelStream()
        .limit(lastN)
        .map(build -> {
          try {
            return build.details();
          } catch (IOException e) {
            return build;
          }
        })
        .filter(build -> BuildWithDetails.class.isInstance(build))
        .map(build -> (BuildWithDetails) build)
        .filter(build
            -> (build.getResult() == BuildResult.SUCCESS || build.getResult() == BuildResult.UNSTABLE)
                && isNotEmpty(build.getArtifacts()))
        .map(buildWithDetails
            -> aBuildDetails()
                   .withNumber(buildWithDetails.getNumber())
                   .withRevision(extractRevision(buildWithDetails))
                   .build())
        .collect(toList());
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#trigger(java.lang.String)
   */
  @Override
  public QueueReference trigger(String jobname, Map<String, String> parameters) throws IOException {
    JobWithDetails jobWithDetails = getJob(jobname);
    QueueReference queueReference;
    if (MapUtils.isEmpty(parameters)) {
      ExtractHeader location =
          jobWithDetails.getClient().post(jobWithDetails.getUrl() + "build", null, ExtractHeader.class, true);
      queueReference = new QueueReference(location.getLocation());
    } else {
      queueReference = jobWithDetails.build(parameters, true);
    }
    return queueReference;
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#checkStatus(java.lang.String)
   */
  @Override
  public String checkStatus(String jobname) {
    throw new NotImplementedException();
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#checkStatus(java.lang.String, java.lang.String)
   */
  @Override
  public String checkStatus(String jobname, String buildNo) {
    throw new NotImplementedException();
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#checkArtifactStatus(java.lang.String, java.lang.String)
   */
  @Override
  public String checkArtifactStatus(String jobname, String artifactpathRegex) {
    throw new NotImplementedException();
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#checkArtifactStatus(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public String checkArtifactStatus(String jobname, String buildNo, String artifactpathRegex) {
    throw new NotImplementedException();
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#downloadArtifact(java.lang.String, java.lang.String)
   */
  @Override
  public Pair<String, InputStream> downloadArtifact(String jobname, String artifactpathRegex)
      throws IOException, URISyntaxException {
    JobWithDetails jobDetails = getJob(jobname);
    if (jobDetails == null) {
      return null;
    }
    Build build = jobDetails.getLastCompletedBuild();
    return downloadArtifactFromABuild(build, artifactpathRegex);
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#downloadArtifact(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public Pair<String, InputStream> downloadArtifact(String jobname, String buildNo, String artifactpathRegex)
      throws IOException, URISyntaxException {
    JobWithDetails jobDetails = getJob(jobname);
    if (jobDetails == null) {
      return null;
    }
    Build build = jobDetails.getBuildByNumber(Integer.parseInt(buildNo));
    return downloadArtifactFromABuild(build, artifactpathRegex);
  }

  @Override
  public Build getBuild(QueueReference queueItem) throws IOException {
    QueueItem queueItem1 = jenkinsServer.getQueueItem(queueItem);
    if (queueItem1.getExecutable() != null) {
      return jenkinsServer.getBuild(queueItem1);
    } else {
      return null;
    }
  }

  private Pair<String, InputStream> downloadArtifactFromABuild(Build build, String artifactpathRegex)
      throws IOException, URISyntaxException {
    if (build == null) {
      return null;
    }
    Pattern pattern = Pattern.compile(artifactpathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
    BuildWithDetails buildWithDetails = build.details();
    Optional<Artifact> artifactOpt = buildWithDetails.getArtifacts()
                                         .stream()
                                         .filter(artifact -> pattern.matcher(artifact.getRelativePath()).matches())
                                         .findFirst();
    if (artifactOpt.isPresent()) {
      return ImmutablePair.of(artifactOpt.get().getFileName(), buildWithDetails.downloadArtifact(artifactOpt.get()));
    } else {
      return null;
    }
  }

  private String extractRevision(BuildWithDetails buildWithDetails) {
    Optional<String> gitRevOpt = buildWithDetails.getActions()
                                     .stream()
                                     .filter(o -> ((Map<String, Object>) o).containsKey("lastBuiltRevision"))
                                     .map(o
                                         -> ((Map<String, Object>) (((Map<String, Object>) o).get("lastBuiltRevision")))
                                                .get("SHA1")
                                                .toString()
                                                .substring(0, 8))
                                     .findFirst();
    if (gitRevOpt.isPresent()) {
      return gitRevOpt.get();
    } else if ("svn".equals(buildWithDetails.getChangeSet().getKind())) {
      try {
        SvnBuildDetails svnBuildDetails =
            buildWithDetails.getClient().get(buildWithDetails.getUrl(), SvnBuildDetails.class);
        OptionalInt revision =
            svnBuildDetails.getChangeSet().getRevisions().stream().mapToInt(SvnRevision::getRevision).max();
        return Integer.toString(revision.getAsInt());
      } catch (Exception e) {
        return Long.toString(buildWithDetails.getTimestamp());
      }
    } else {
      return Long.toString(buildWithDetails.getTimestamp());
    }
  }
}
