package software.wings.service.intfc.newrelic;

import software.wings.beans.NewRelicConfig;
import software.wings.beans.NewRelicDeploymentMarkerPayload;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.service.impl.newrelic.NewRelicMetricData;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicDelegateService {
  @DelegateTaskType(TaskType.NEWRELIC_VALIDATE_CONFIGURATION_TASK)
  boolean validateConfig(@NotNull NewRelicConfig newRelicConfig) throws IOException, CloneNotSupportedException;

  @DelegateTaskType(TaskType.NEWRELIC_GET_APP_TASK)
  List<NewRelicApplication> getAllApplications(@NotNull NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog)
      throws IOException, CloneNotSupportedException;

  @DelegateTaskType(TaskType.NEWRELIC_GET_APP_INSTANCES_TASK)
  List<NewRelicApplicationInstance> getApplicationInstances(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicApplicationId, ThirdPartyApiCallLog apiCallLog)
      throws IOException, CloneNotSupportedException;

  @DelegateTaskType(TaskType.NEWRELIC_GET_METRICES_DATA)
  NewRelicMetricData getMetricDataApplicationInstance(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicApplicationId, long instanceId,
      Collection<String> metricNames, long fromTime, long toTime, ThirdPartyApiCallLog apiCallLog) throws IOException;

  NewRelicMetricData getMetricDataApplication(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicApplicationId, Collection<String> metricNames,
      long fromTime, long toTime, boolean summarize, ThirdPartyApiCallLog apiCallLog) throws IOException;

  @DelegateTaskType(TaskType.NEWRELIC_POST_DEPLOYMENT_MARKER)
  String postDeploymentMarker(NewRelicConfig config, List<EncryptedDataDetail> encryptedDataDetails,
      long newRelicApplicationId, NewRelicDeploymentMarkerPayload body, ThirdPartyApiCallLog apiCallLog)
      throws IOException;

  Set<NewRelicMetric> getTxnNameToCollect(NewRelicConfig newRelicConfig, List<EncryptedDataDetail> encryptedDataDetails,
      long newRelicAppId, ThirdPartyApiCallLog thirdPartyApiCallLog) throws IOException;
}
