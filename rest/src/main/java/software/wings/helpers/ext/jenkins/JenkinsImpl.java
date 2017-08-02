package software.wings.helpers.ext.jenkins;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.awaitility.Awaitility.with;
import static org.hamcrest.CoreMatchers.notNullValue;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.ExtractHeader;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.HttpResponseException;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.utils.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * The Class JenkinsImpl.
 */
public class JenkinsImpl implements Jenkins {
  private final String FOLDER_JOB_CLASS_NAME = "com.cloudbees.hudson.plugins.folder.Folder";
  private final int MAX_FOLDER_DEPTH = 10;
  private JenkinsServer jenkinsServer;
  private final Logger logger = LoggerFactory.getLogger(getClass());

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
      @Assisted(value = "password") char[] password) throws URISyntaxException {
    jenkinsServer = new JenkinsServer(new URI(jenkinsUrl), username, new String(password));
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#getJob(java.lang.String)
   */
  @Override
  public JobWithDetails getJob(String jobname) throws IOException {
    try {
      return with()
          .pollInterval(3L, TimeUnit.SECONDS)
          .atMost(new Duration(25L, TimeUnit.SECONDS))
          .until(
              ()
                  -> {
                if (jobname == null) {
                  return null;
                }

                String decodedJobName = URLDecoder.decode(jobname, "UTF-8");
                String parentJobName = null;
                String parentJobUrl = null;
                String childJobName;
                String[] jobNameSplit = decodedJobName.split("/");
                int parts = jobNameSplit.length;
                if (parts > 1) {
                  parentJobUrl = constructParentJobUrl(jobNameSplit);
                  parentJobName = jobNameSplit[(parts - 2)];
                  childJobName = jobNameSplit[(parts - 1)];
                } else {
                  childJobName = decodedJobName;
                }

                JobWithDetails jobWithDetails;
                FolderJob folderJob = null;

                try {
                  if (parentJobName != null && parentJobName.length() > 0) {
                    folderJob = new FolderJob(parentJobName, parentJobUrl);
                  }

                  jobWithDetails = jenkinsServer.getJob(folderJob, childJobName);
                } catch (HttpResponseException e) {
                  if (e.getStatusCode() == 500 || e.getMessage().contains("Server Error")) {
                    Misc.warn(logger, String.format("Error occurred while retrieving job %s. Retrying ", jobname), e);
                    return null;
                  } else {
                    throw e;
                  }
                }
                return Collections.singletonList(jobWithDetails);
              },
              notNullValue())
          .get(0);
    } catch (ConditionTimeoutException e) {
      Misc.warn(logger, "Jenkins server request did not succeed within 15 secs even after 5 retries", e);
      final WingsException wingsException = new WingsException(ErrorCode.JENKINS_ERROR);
      wingsException.addParam("message", "Failed to get job details for " + jobname);
      wingsException.addParam("jenkinsResponse", "Server Error");
      throw wingsException;
    }
  }

  private String constructParentJobUrl(String[] jobNameSplit) {
    if (jobNameSplit == null || jobNameSplit.length == 0) {
      return "/";
    }

    int parts = jobNameSplit.length;
    int currentIndex = 0;
    StringBuilder sb = new StringBuilder();
    for (String jobName : jobNameSplit) {
      if (currentIndex++ < (parts - 1)) {
        sb.append("/job/");
        sb.append(jobName);
      }
    }

    sb.append("/");
    return sb.toString();
  }

  @Override
  public List<JobDetails> getJobs(String parentFolderJobName) throws IOException {
    int timeoutInSeconds = 30;
    try {
      return with()
          .pollInterval(3L, TimeUnit.SECONDS)
          .atMost(new Duration(timeoutInSeconds, TimeUnit.SECONDS))
          .until(() -> {
            try {
              FolderJob folderJob = null;
              int depth = 1;
              List<JobDetails> jobDetailsList = new ArrayList<>();
              if (parentFolderJobName != null && parentFolderJobName.length() > 0) {
                // passing null for parentJobDisplayName since the root is expanded in the ui model, we don't need to
                // add it again
                return browseJobsRecursively(null, null, parentFolderJobName, depth, jobDetailsList, true);
              } else {
                // passing null for parentJobDisplayName since the root is expanded in the ui model, we don't need to
                // add it again at the root level, we only want to fetch the first level jobs
                return browseJobsRecursively(null, null, parentFolderJobName, depth, jobDetailsList, false);
              }

            } catch (HttpResponseException e) {
              if (e.getStatusCode() == 500 || e.getMessage().contains("Server Error")) {
                Misc.warn(logger, "Error occurred while retrieving jobs. Retrying", e);
                return null;
              } else {
                throw e;
              }
            }
          }, notNullValue());
    } catch (ConditionTimeoutException e) {
      Misc.warn(
          logger, "Jenkins server request did not succeed within " + timeoutInSeconds + "secs even after 5 retries", e);
      final WingsException wingsException = new WingsException(ErrorCode.JENKINS_ERROR);
      wingsException.addParam("message", "Failed to get jobs from jenkins sever");
      wingsException.addParam("jenkinsResponse", "Server Error");
      throw wingsException;
    }
  }

  private List<JobDetails> browseJobsRecursively(String parentJobUrl, String parentJobDisplayName, String folderJobName,
      int depth, List<JobDetails> jobDetailsList, boolean recurseThrough) throws IOException {
    if (depth == MAX_FOLDER_DEPTH) {
      return jobDetailsList;
    }

    FolderJob folderJob = null;
    String jobUrl = null;
    String jobDisplayName = null;
    if (folderJobName != null && folderJobName.length() > 0) {
      if (parentJobUrl != null) {
        jobUrl = parentJobUrl + "job/" + folderJobName + "/";

      } else {
        jobUrl = "/job/" + folderJobName + "/";
      }

      folderJob = new FolderJob(folderJobName, jobUrl);
      // We need to show the parent job name starting from depth 2 since ui presents the first level as an expanded
      // tree. From depth 2, we flatten it
      if (depth >= 2) {
        jobDisplayName = (parentJobDisplayName != null) ? (parentJobDisplayName + "/" + folderJobName) : folderJobName;
      }
    }

    Map<String, Job> jobsMap = jenkinsServer.getJobs(folderJob);
    Collection<Job> jobCollection = jobsMap.values();
    for (Job job : jobCollection) {
      boolean isFolderJob = isFolderJob(job);
      if (!recurseThrough) {
        jobDetailsList.add(new JobDetails(job.getName(), isFolderJob));
      } else {
        if (isFolderJob) {
          browseJobsRecursively(jobUrl, jobDisplayName, job.getName(), ++depth, jobDetailsList, recurseThrough);
        } else {
          jobDetailsList.add(
              new JobDetails((jobDisplayName != null) ? (jobDisplayName + "/" + job.getName()) : job.getName(), false));
        }
      }
    }
    return jobDetailsList;
  }

  private boolean isFolderJob(Job job) {
    // job.get_class().equals(FOLDER_JOB_CLASS_NAME) is to find if the jenkins job is of type folder.
    // (job instanceOf FolderJob) doesn't work
    return job.get_class().equals(FOLDER_JOB_CLASS_NAME);
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#getBuilds(java.lang.String, int)
   */
  @Override
  public List<BuildDetails> getBuildsForJob(String jobname, int lastN) throws IOException {
    JobWithDetails jobWithDetails = getJob(jobname);
    if (jobWithDetails == null) {
      return null;
    }
    return Lists.newArrayList(
        jobWithDetails.getBuilds()
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
            .map(buildWithDetails -> getBuildDetails(buildWithDetails))
            .collect(toList()));
  }

  public BuildDetails getBuildDetails(BuildWithDetails buildWithDetails) {
    return aBuildDetails()
        .withNumber(String.valueOf(buildWithDetails.getNumber()))
        .withRevision(extractRevision(buildWithDetails))
        .withDescription(buildWithDetails.getDescription())
        .withBuildParameters(buildWithDetails.getParameters())
        .build();
  }

  @Override
  public BuildDetails getLastSuccessfulBuildForJob(String jobName) throws IOException {
    JobWithDetails jobWithDetails = getJob(jobName);
    if (jobWithDetails == null) {
      return null;
    }

    Build lastSuccessfulBuild = jobWithDetails.getLastSuccessfulBuild();

    if (lastSuccessfulBuild == null) {
      return null;
    }
    BuildWithDetails buildWithDetails = lastSuccessfulBuild.details();
    return getBuildDetails(buildWithDetails);
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

  @Override
  public boolean isRunning() {
    return jenkinsServer.isRunning();
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
    Optional<String> gitRevOpt =
        buildWithDetails.getActions()
            .stream()
            .filter(o -> ((Map<String, Object>) o).containsKey("lastBuiltRevision"))
            .map(o
                -> ((Map<String, Object>) (((Map<String, Object>) o).get("lastBuiltRevision"))).get("SHA1").toString())
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
