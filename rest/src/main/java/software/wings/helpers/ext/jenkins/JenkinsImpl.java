package software.wings.helpers.ext.jenkins;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.awaitility.Awaitility.with;
import static org.hamcrest.CoreMatchers.notNullValue;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.collect.Lists;
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
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.utils.HttpUtil;
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
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;

/**
 * The Class JenkinsImpl.
 */
public class JenkinsImpl implements Jenkins {
  private final String FOLDER_JOB_CLASS_NAME = "com.cloudbees.hudson.plugins.folder.Folder";
  private final int MAX_FOLDER_DEPTH = 10;
  private JenkinsServer jenkinsServer;
  private JenkinsHttpClient jenkinsHttpClient;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject ExecutorService executorService;

  /**
   * Instantiates a new jenkins impl.
   *
   * @param jenkinsUrl the jenkins url
   * @throws URISyntaxException the URI syntax exception
   */
  @AssistedInject
  public JenkinsImpl(@Assisted(value = "url") String jenkinsUrl) throws URISyntaxException {
    jenkinsHttpClient = new HarnessJenkinsHttpClient(new URI(jenkinsUrl), getUnSafeBuilder());
    jenkinsServer = new JenkinsServer(jenkinsHttpClient);
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
        new HarnessJenkinsHttpClient(new URI(jenkinsUrl), username, new String(password), getUnSafeBuilder());
    jenkinsServer = new JenkinsServer(jenkinsHttpClient);
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#getJob(java.lang.String)
   */
  @Override
  public JobWithDetails getJob(String jobname) throws IOException {
    logger.info("Retrieving job {}", jobname);
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
                    logger.warn(String.format("Error occurred while retrieving job %s. Retrying ", jobname), e);
                    return null;
                  } else {
                    throw e;
                  }
                }
                logger.info("Retrieving job {} success", jobname);
                return Collections.singletonList(jobWithDetails);
              },
              notNullValue())
          .get(0);
    } catch (ConditionTimeoutException e) {
      logger.warn("Jenkins server request did not succeed within 25 secs even after 5 retries", e);
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
  public List<JobDetails> getJobs(String parentJob) throws IOException {
    try {
      return with()
          .pollInterval(3L, TimeUnit.SECONDS)
          .atMost(new Duration(25L, TimeUnit.SECONDS))
          .until(() -> getJobDetails(parentJob), notNullValue());
    } catch (ConditionTimeoutException e) {
      jenkinsExceptionHandler(e);
    }
    return Collections.emptyList();
  }

  private List<JobDetails> getJobDetails(String parentJob) {
    try {
      List<JobDetails> result = new ArrayList<>(); // TODO:: extend jobDetails to keep track of prefix.
      Stack<Job> jobs = new Stack<>();
      Queue<Future> futures = new ConcurrentLinkedQueue<>();

      if (Misc.isNullOrEmpty(parentJob)) {
        return jenkinsServer.getJobs()
            .values()
            .stream()
            .map(job -> new JobDetails(getJobNameFromUrl(job.getUrl()), job.getUrl(), isFolderJob(job)))
            .collect(Collectors.toList());
      } else {
        jobs.addAll(jenkinsServer.getJobs(new FolderJob(parentJob, "/job/" + parentJob + "/")).values());
      }

      while (!jobs.empty() || !futures.isEmpty()) {
        while (!jobs.empty()) {
          Job job = jobs.pop();
          if (isFolderJob(job)) {
            futures.add(executorService.submit((Callable<Void>) () -> {
              jobs.addAll(jenkinsServer.getJobs(new FolderJob(job.getName(), job.getUrl())).values());
              return null;
            }));
          } else {
            String jobName = getJobNameFromUrl(job.getUrl());
            result.add(new JobDetails(jobName, job.getUrl(), false));
          }
        }
        while (!futures.isEmpty() && futures.peek().isDone()) {
          futures.poll().get();
        }
        Misc.quietSleep(10, TimeUnit.MILLISECONDS); // avoid busy wait
      }
      return result;
    } catch (Exception ex) {
      logger.error("Error in fetching job lists ", ex);
      jenkinsExceptionHandler(ex);
      return Collections.emptyList();
    }
  }

  private String getJobNameFromUrl(String url) {
    // TODO:: remove it post review. Extend jobDetails object
    try {
      URI uri = new URI(url);
      String[] parts = uri.getPath().split("/");
      String name = "";
      for (int idx = 2; idx <= parts.length - 1; idx = idx + 2) {
        name += "/" + parts[idx];
      }
      return name.startsWith("/") ? name.substring(1) : name;
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return url;
    }
  }

  private String getJobName(String url) {
    return null;
  }

  //  @Override
  //  public List<JobDetails> getJobs(String parentFolderJobName) throws IOException {
  //    logger.info("Retrieving jenkins jobs");
  //    List<JobDetails> jobDetailsList = new Vector<>();
  //    int depth = 1;
  //    // We need to maintain a counter to know how many async callables were spun up.
  //    AtomicInteger callableCount = new AtomicInteger();
  //    final Lock lock = new ReentrantLock();
  //    final Condition allJobsDoneCondition = lock.newCondition();
  //
  //    if (parentFolderJobName != null && parentFolderJobName.length() > 0) {
  //      // passing null for parentJobDisplayName since the root is expanded in the ui model, we don't need to add it
  //      again browseJobsRecursively(null, null, parentFolderJobName, depth, jobDetailsList, true, callableCount, lock,
  //      allJobsDoneCondition);
  //
  //    } else {
  //      // passing null for parentJobDisplayName since the root is expanded in the ui model, we don't need to add it
  //      again
  //      // at the root level, we only want to fetch the first level jobs
  //      browseJobsRecursively(null, null, parentFolderJobName, depth, jobDetailsList, false, callableCount, lock,
  //      allJobsDoneCondition);
  //    }
  //
  //    // Wait for all async callable jobs to complete. If not completed within the timeout, return the job details
  //    list collected so far. lock.lock(); try {
  //      allJobsDoneCondition.await(20, TimeUnit.SECONDS);
  //    } catch (InterruptedException ex) {
  //      logger.warn("Failed to retrieve all jobs within 20 secs");
  //    } finally {
  //      lock.unlock();
  //    }
  //    logger.info("Jenkins jobs retrieved successfully");
  //    return jobDetailsList;
  //  }

  private void browseJobsRecursively(String folderJobUrl, String folderJobDisplayName, String folderJobName,
      final int depth, List<JobDetails> jobDetailsList, boolean recurseThrough, AtomicInteger callableCount, Lock lock,
      Condition allJobsDoneCondition) throws IOException {
    if (depth == MAX_FOLDER_DEPTH) {
      return;
    }
    // Each time a new parentJob needs to be fetched,
    callableCount.incrementAndGet();
    Future<Void> submit = executorService.submit((Callable<Void>) () -> {
      FolderJob folderJob = null;
      String jobUrl = null;
      String jobDisplayName = null;
      if (folderJobName != null && folderJobName.length() > 0) {
        if (folderJobUrl != null) {
          jobUrl = folderJobUrl + "job/" + folderJobName + "/";

        } else {
          jobUrl = "/job/" + folderJobName + "/";
        }

        folderJob = new FolderJob(folderJobName, jobUrl);
        // We need to show the parent job name starting from depth 2 since ui presents the first level as an expanded
        // tree. From depth 2, we flatten it
        if (depth >= 2) {
          jobDisplayName =
              (folderJobDisplayName != null) ? (folderJobDisplayName + "/" + folderJobName) : folderJobName;
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
            browseJobsRecursively(jobUrl, jobDisplayName, job.getName(), depth + 1, jobDetailsList, recurseThrough,
                callableCount, lock, allJobsDoneCondition);
          } else {
            jobDetailsList.add(new JobDetails(
                (jobDisplayName != null) ? (jobDisplayName + "/" + job.getName()) : job.getName(), false));
          }
        }
      }

      // The last job signals that the main caller job can return the results now
      lock.lock();
      try {
        callableCount.decrementAndGet();
        if (callableCount.intValue() == 0) {
          allJobsDoneCondition.signalAll();
        }
      } finally {
        lock.unlock();
      }
      return null;
    });

    try {
      submit.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      e.printStackTrace();
    }

    return;
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
    BuildDetails buildDetails = aBuildDetails()
                                    .withNumber(String.valueOf(buildWithDetails.getNumber()))
                                    .withRevision(extractRevision(buildWithDetails))
                                    .withDescription(buildWithDetails.getDescription())
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
      logger.error(
          "Error occurred while retrieving build parameters for build number {} ", buildWithDetails.getNumber(), e);
    }
  }

  @Override
  public BuildDetails getLastSuccessfulBuildForJob(String jobName) throws IOException {
    logger.info("Retrieving last successful build for job name {}", jobName);
    JobWithDetails jobWithDetails = getJob(jobName);
    if (jobWithDetails == null) {
      logger.info("Job {} does not exist", jobName);
      return null;
    }

    Build lastSuccessfulBuild = jobWithDetails.getLastSuccessfulBuild();
    if (lastSuccessfulBuild == null) {
      logger.info("There is no last successful build for job {]", jobName);
      return null;
    }
    BuildWithDetails buildWithDetails = lastSuccessfulBuild.details();
    logger.info("Last successful build {} for job {}", buildWithDetails.getNumber(), jobName);
    return getBuildDetails(buildWithDetails);
  }

  /* (non-Javadoc)
   * @see software.wings.helpers.ext.jenkins.Jenkins#trigger(java.lang.String)
   */
  @Override
  public QueueReference trigger(String jobname, Map<String, String> parameters) throws IOException {
    JobWithDetails jobWithDetails = getJob(jobname);
    if (jobWithDetails == null) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "No job [" + jobname + "] found");
    }
    try {
      QueueReference queueReference;
      if (MapUtils.isEmpty(parameters)) {
        ExtractHeader location =
            jobWithDetails.getClient().post(jobWithDetails.getUrl() + "build", null, ExtractHeader.class, true);
        queueReference = new QueueReference(location.getLocation());
      } else {
        queueReference = jobWithDetails.build(parameters, true);
      }
      return queueReference;
    } catch (IOException e) {
      logger.error("Failed to trigger job {} with url {} ", jobname, jobWithDetails.getUrl(), e);
      throw e;
    }
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
    try {
      this.jenkinsHttpClient.get("/");
      return true;
    } catch (Exception e) {
      jenkinsExceptionHandler(e);
      return false;
    }
  }

  private void jenkinsExceptionHandler(Exception e) {
    if (e instanceof HttpResponseException) {
      if (((HttpResponseException) e).getStatusCode() == 401) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "Invalid Jenkins credentials");
      } else if (((HttpResponseException) e).getStatusCode() == 403) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", "User not authorized to access jenkins");
      }
      final WingsException wingsException = new WingsException(ErrorCode.JENKINS_ERROR);
      wingsException.addParam("message", "Jenkins server may not be running");
      wingsException.addParam("jenkinsResponse", e.getMessage());
      throw wingsException;
    } else if (e instanceof ConditionTimeoutException) {
      logger.warn("Jenkins server request did not succeed within 25 secs", e);
      final WingsException wingsException = new WingsException(ErrorCode.JENKINS_ERROR);
      wingsException.addParam("message", "Failed to get job details");
      wingsException.addParam("jenkinsResponse", "Server Error");
      throw wingsException;
    } else {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, "message", e.getMessage());
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

  private HttpClientBuilder getUnSafeBuilder() {
    HttpClientBuilder builder = HttpClientBuilder.create();
    try {
      // Set ssl context
      builder.setSSLContext(HttpUtil.getSslContext());
      // Create all-trusting host name verifier
      HostnameVerifier allHostsValid = (s, sslSession) -> true;
      builder.setSSLHostnameVerifier(allHostsValid);

    } catch (Exception ex) {
      logger.warn("Installing trust managers");
    }
    return builder;
  }
}
