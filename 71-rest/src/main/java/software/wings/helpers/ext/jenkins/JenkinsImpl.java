package software.wings.helpers.ext.jenkins;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_ADMIN;
import static io.harness.threading.Morpheus.quietSleep;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpClient;
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
import io.harness.exception.WingsException;
import io.harness.network.Http;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;

/**
 * The Class JenkinsImpl.
 */
public class JenkinsImpl implements Jenkins {
  private static final Logger logger = LoggerFactory.getLogger(JenkinsImpl.class);

  private final String FOLDER_JOB_CLASS_NAME = "com.cloudbees.hudson.plugins.folder.Folder";

  @Inject private ExecutorService executorService;
  @Inject private TimeLimiter timeLimiter;

  private JenkinsServer jenkinsServer;
  private JenkinsHttpClient jenkinsHttpClient;

  /**
   * Instantiates a new jenkins impl.
   *
   * @param jenkinsUrl the jenkins url
   * @throws URISyntaxException the URI syntax exception
   */
  @AssistedInject
  public JenkinsImpl(@Assisted(value = "url") String jenkinsUrl) throws URISyntaxException {
    jenkinsHttpClient = new CustomJenkinsHttpClient(new URI(jenkinsUrl), getUnSafeBuilder(jenkinsUrl));
    jenkinsServer = new CustomJenkinsServer(jenkinsHttpClient);
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
    jenkinsHttpClient =
        new CustomJenkinsHttpClient(new URI(jenkinsUrl), username, new String(password), getUnSafeBuilder(jenkinsUrl));
    jenkinsServer = new CustomJenkinsServer(jenkinsHttpClient);
  }

  /**
   * Instantiates a new jenkins impl.
   *
   */
  @AssistedInject
  public JenkinsImpl(@Assisted(value = "url") String jenkinsUrl, @Assisted(value = "token") char[] token)
      throws URISyntaxException {
    jenkinsHttpClient =
        new CustomJenkinsHttpClient(new URI(jenkinsUrl), new String(token), getUnSafeBuilder(jenkinsUrl));
    jenkinsServer = new CustomJenkinsServer(jenkinsHttpClient);
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#getJob(java.lang.String)
   */
  @Override
  public JobWithDetails getJob(String jobname) {
    logger.info("Retrieving job {}", jobname);
    try {
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          if (jobname == null) {
            sleep(ofSeconds(1L));
            continue;
          }

          String decodedJobName = URLDecoder.decode(jobname, "UTF-8");
          String parentJobName = null;
          String parentJobUrl = null;
          String childJobName;
          String[] jobNameSplit = decodedJobName.split("/");
          int parts = jobNameSplit.length;
          if (parts > 1) {
            parentJobUrl = constructParentJobUrl(jobNameSplit);
            parentJobName = jobNameSplit[parts - 2];
            childJobName = jobNameSplit[parts - 1];
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
            if (e.getStatusCode() == 500 || Misc.getMessage(e).contains("Server Error")) {
              logger.warn(format("Error occurred while retrieving job %s. Retrying ", jobname), e);
              sleep(ofSeconds(1L));
              continue;
            } else {
              throw e;
            }
          }
          logger.info("Retrieving job {} success", jobname);
          return singletonList(jobWithDetails).get(0);
        }
      }, 120L, TimeUnit.SECONDS, true);
    } catch (Exception e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", Misc.getMessage(e));
    }
  }

  private String constructParentJobUrl(String[] jobNameSplit) {
    if (isEmpty(jobNameSplit)) {
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

    sb.append('/');
    return sb.toString();
  }

  @Override
  public List<JobDetails> getJobs(String parentJob) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          List<JobDetails> details = getJobDetails(parentJob);
          if (details != null) {
            return details;
          }
          sleep(ofMillis(100L));
        }
      }, 120L, TimeUnit.SECONDS, true);
    } catch (Exception e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", Misc.getMessage(e));
    }
  }

  private List<JobDetails> getJobDetails(String parentJob) {
    List<JobDetails> result = new ArrayList<>(); // TODO:: extend jobDetails to keep track of prefix.
    try {
      Stack<Job> jobs = new Stack<>();
      Queue<Future> futures = new ConcurrentLinkedQueue<>();
      if (isBlank(parentJob)) {
        return jenkinsServer.getJobs()
            .values()
            .stream()
            .map(job -> new JobDetails(getJobNameFromUrl(job.getUrl()), job.getUrl(), isFolderJob(job)))
            .collect(toList());
      } else {
        jobs.addAll(jenkinsServer.getJobs(new FolderJob(parentJob, "/job/" + parentJob + "/")).values());
      }

      while (!jobs.empty() || !futures.isEmpty()) {
        while (!jobs.empty()) {
          Job job = jobs.pop();
          if (isFolderJob(job)) {
            futures.add(executorService.submit(
                () -> jobs.addAll(jenkinsServer.getJobs(new FolderJob(job.getName(), job.getUrl())).values())));
          } else {
            String jobName = getJobNameFromUrl(job.getUrl());
            result.add(new JobDetails(jobName, job.getUrl(), false));
          }
        }
        while (!futures.isEmpty() && futures.peek().isDone()) {
          futures.poll().get();
        }
        quietSleep(ofMillis(10));
      }
      return result;
    } catch (Exception ex) {
      logger.error("Error in fetching job lists ", ex);
      // handleException(ex);
      return result;
    }
  }

  protected String getNormalizedName(String jobName) {
    try {
      if (isNotEmpty(jobName)) {
        return URLDecoder.decode(jobName, Charsets.UTF_8.name());
      }
    } catch (UnsupportedEncodingException e) {
      logger.warn("Failed to decode jobName {}", jobName, e);
    }
    return jobName;
  }

  /**
   * This method generates the ui display name from the jenkins url.
   * The jenkins url looks like &lt;jenkins_base_url&gt;/job/job1/job/job2/job/job3
   * from which we have to show job1/job2/job3 in the ui.
   * @param url
   * @return
   */
  private String getJobNameFromUrl(String url) {
    // TODO:: remove it post review. Extend jobDetails object
    // Each jenkins server could have a different base url.
    // Whichever is the format, the url after the base would always start with "/job/"
    String relativeUrl;
    String pattern = ".*?/job/";

    relativeUrl = url.replaceFirst(pattern, "");

    // URI uri = new URI(relativeUrl);
    String[] parts = relativeUrl.split("/");
    StringBuilder nameBuilder = new StringBuilder();
    // We start with index 0 since /job/ has already been
    for (int idx = 0; idx <= parts.length - 1; idx = idx + 2) {
      nameBuilder.append('/').append(parts[idx]);
    }
    String name = nameBuilder.toString();
    name = name.charAt(0) == '/' ? name.substring(1) : name;
    return getNormalizedName(name);
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
            .filter(BuildWithDetails.class ::isInstance)
            .map(build -> (BuildWithDetails) build)
            .filter(build
                -> (build.getResult() == BuildResult.SUCCESS || build.getResult() == BuildResult.UNSTABLE)
                    && isNotEmpty(build.getArtifacts()))
            .map(this ::getBuildDetails)
            .collect(toList()));
  }

  public BuildDetails getBuildDetails(BuildWithDetails buildWithDetails) {
    BuildDetails buildDetails = aBuildDetails()
                                    .withNumber(String.valueOf(buildWithDetails.getNumber()))
                                    .withRevision(extractRevision(buildWithDetails))
                                    .withDescription(buildWithDetails.getDescription())
                                    .withBuildDisplayName(buildWithDetails.getDisplayName())
                                    .withBuildUrl(buildWithDetails.getUrl())
                                    .withBuildFullDisplayName(buildWithDetails.getFullDisplayName())
                                    .build();
    populateBuildParams(buildWithDetails, buildDetails);
    return buildDetails;
  }

  public void populateBuildParams(BuildWithDetails buildWithDetails, BuildDetails buildDetails) {
    try {
      if (buildWithDetails.getParameters() != null) {
        buildDetails.setBuildParameters(buildWithDetails.getParameters());
      }
    } catch (Exception e) { // cause buildWithDetails.getParameters() can throw NPE
      // unexpected exception
      logger.warn(
          "Error occurred while retrieving build parameters for build number {} ", buildWithDetails.getNumber(), e);
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuildForJob(String jobName) throws IOException {
    JobWithDetails jobWithDetails = getJob(jobName);
    if (jobWithDetails == null) {
      logger.info("Job {} does not exist", jobName);
      return null;
    }

    Build lastSuccessfulBuild = jobWithDetails.getLastSuccessfulBuild();
    if (lastSuccessfulBuild == null) {
      logger.info("There is no last successful build for job {}", jobName);
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
    if (jobWithDetails == null) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER_ADMIN)
          .addParam("message", "No job [" + jobname + "] found");
    }
    try {
      QueueReference queueReference;
      logger.info("Triggering job {} ", jobWithDetails.getUrl());
      if (isEmpty(parameters)) {
        ExtractHeader location =
            jobWithDetails.getClient().post(jobWithDetails.getUrl() + "build", null, ExtractHeader.class, true);
        queueReference = new QueueReference(location.getLocation());
      } else {
        queueReference = jobWithDetails.build(parameters, true);
      }
      logger.info("Triggering job {} success ", jobWithDetails.getUrl());
      return queueReference;
    } catch (HttpResponseException e) {
      throw e;
    } catch (IOException e) {
      throw new IOException(format("Failed to trigger job %s with url %s", jobname, jobWithDetails.getUrl()), e);
    }
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#checkStatus(java.lang.String)
   */
  @Override
  public String checkStatus(String jobname) {
    throw new NotImplementedException("Not implemented");
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#checkStatus(java.lang.String, java.lang.String)
   */
  @Override
  public String checkStatus(String jobname, String buildNo) {
    throw new NotImplementedException("Not implemented");
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#checkArtifactStatus(java.lang.String, java.lang.String)
   */
  @Override
  public String checkArtifactStatus(String jobname, String artifactpathRegex) {
    throw new NotImplementedException("Not implemented");
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#checkArtifactStatus(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public String checkArtifactStatus(String jobname, String buildNo, String artifactpathRegex) {
    throw new NotImplementedException("Not implemented");
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
    logger.info("Downloading artifactpathRegex {}, buildNo {} and jobname {}", artifactpathRegex, buildNo, jobname);
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
    try {
      this.jenkinsHttpClient.get("/");
      return true;
    } catch (IOException e) {
      throw prepareWingsException(e);
    }
  }

  private WingsException prepareWingsException(IOException e) {
    if (e instanceof HttpResponseException) {
      if (((HttpResponseException) e).getStatusCode() == 401) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", "Invalid Jenkins credentials");
      } else if (((HttpResponseException) e).getStatusCode() == 403) {
        throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
            .addParam("message", "User not authorized to access jenkins");
      }
    }
    throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", Misc.getMessage(e));
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
                                         .filter(artifact -> pattern.matcher(artifact.getRelativePath()).find())
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
    } else if (buildWithDetails.getChangeSet() != null && "svn".equals(buildWithDetails.getChangeSet().getKind())) {
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

  private HttpClientBuilder getUnSafeBuilder(String jenkinsUrl) {
    HttpClientBuilder builder = HttpClientBuilder.create();
    try {
      // Set ssl context
      builder.setSSLContext(Http.getSslContext());
      // Create all-trusting host name verifier
      HostnameVerifier allHostsValid = (s, sslSession) -> true;
      builder.setSSLHostnameVerifier(allHostsValid);
    } catch (Exception ex) {
      logger.warn("Installing trust managers");
    }
    return builder;
  }
}
