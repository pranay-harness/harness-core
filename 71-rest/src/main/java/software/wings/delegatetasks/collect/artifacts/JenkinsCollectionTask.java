package software.wings.delegatetasks.collect.artifacts;

import static software.wings.common.Constants.BUILD_NO;

import com.google.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.JenkinsConfig;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.jenkins.JenkinsUtil;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rishi on 12/14/16.
 */
public class JenkinsCollectionTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(JenkinsCollectionTask.class);

  @Inject private JenkinsUtil jenkinsUtil;
  @Inject private EncryptionService encryptionService;
  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;

  public JenkinsCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public ListNotifyResponseData run(Object[] parameters) {
    return run((JenkinsConfig) parameters[0], (List<EncryptedDataDetail>) parameters[1], (String) parameters[2],
        (List<String>) parameters[3], (Map<String, String>) parameters[4]);
  }

  public ListNotifyResponseData run(JenkinsConfig jenkinsConfig, List<EncryptedDataDetail> encryptionDetails,
      String jobName, List<String> artifactPaths, Map<String, String> arguments) {
    ListNotifyResponseData res = new ListNotifyResponseData();

    try {
      encryptionService.decrypt(jenkinsConfig, encryptionDetails);
      Jenkins jenkins = jenkinsUtil.getJenkins(jenkinsConfig);

      for (String artifactPath : artifactPaths) {
        logger.info("Collecting artifact {} of job {}", artifactPath, jobName);
        Pair<String, InputStream> fileInfo = jenkins.downloadArtifact(jobName, arguments.get(BUILD_NO), artifactPath);
        artifactCollectionTaskHelper.addDataToResponse(
            fileInfo, artifactPath, res, getDelegateId(), getTaskId(), getAccountId());
      }
    } catch (Exception e) {
      logger.warn("Exception: " + Misc.getMessage(e), e);
      // TODO: better error handling

      //      if (e instanceof WingsException)
      //        WingsException ex = (WingsException) e;
      //        errorMessage = Joiner.on(",").join(ex.getResponseMessageList().stream()
      //            .map(responseMessage ->
      //            MessageManager.getInstance().getResponseMessage(responseMessage.getCode(),
      //            ex.getParams()).getMessage()) .collect(toList()));
      //      } else {
      //        errorMessage = e.getMessage();
      //      }
      //      executionStatus = executionStatus.FAILED;
      //      jenkinsExecutionResponse.setErrorMessage(errorMessage);
    }

    return res;
  }
}
