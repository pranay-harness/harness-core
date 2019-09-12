package io.harness.perpetualtask.k8s.watch;

import static software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType.APPLY;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.service.intfc.SettingsService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class K8sWatchPerpetualTaskServiceClient
    implements PerpetualTaskServiceClient<K8WatchPerpetualTaskClientParams> {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private SettingsService settingsService;
  @Inject private K8sClusterConfigFactory k8sClusterConfigFactory;

  private static final String CLOUD_PROVIDER_ID = "cloudProviderId";
  private static final String K8_RESOURCE_KIND = "k8sResourceKind";

  @Override
  public String create(String accountId, K8WatchPerpetualTaskClientParams clientParams) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(CLOUD_PROVIDER_ID, clientParams.getCloudProviderId());
    clientParamMap.put(K8_RESOURCE_KIND, clientParams.getK8sResourceKind());
    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(600))
                                         .setTimeout(Durations.fromMillis(30000))
                                         .build();
    return perpetualTaskService.createTask(PerpetualTaskType.K8S_WATCH, accountId, clientContext, schedule, false);
  }

  @Override
  public boolean delete(String accountId, String taskId) {
    // TODO(Tang) delete
    return perpetualTaskService.deleteTask(accountId, taskId);
  }

  @Override
  public K8sWatchTaskParams getTaskParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String cloudProviderId = clientParams.get(CLOUD_PROVIDER_ID);
    String k8sResourceKind = clientParams.get(K8_RESOURCE_KIND);

    SettingAttribute settingAttribute = settingsService.get(cloudProviderId);
    K8sClusterConfig config = k8sClusterConfigFactory.getK8sClusterConfig(settingAttribute);
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(config));

    // TODO(Tang): throw exception upon validation failure

    return K8sWatchTaskParams.newBuilder()
        .setCloudProviderId(cloudProviderId)
        .setK8SClusterConfig(bytes)
        .setK8SResourceKind(k8sResourceKind)
        .build();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    K8sWatchTaskParams params = getTaskParams(clientContext);

    K8sClusterConfig k8sClusterConfig =
        (K8sClusterConfig) KryoUtils.asObject(params.getK8SClusterConfig().toByteArray());

    K8sTaskParameters k8sTaskParameters = new K8sTaskParameters("", "", "", "", k8sClusterConfig, "", "", 0, APPLY);

    return DelegateTask.builder()
        .async(false)
        .accountId(accountId)
        .data(TaskData.builder()
                  .taskType(TaskType.K8S_WATCH_TASK.name())
                  .parameters(new Object[] {k8sTaskParameters})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .build();
  }
}
