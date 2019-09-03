package io.harness.perpetualtask.k8s.watch;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.serializer.KryoUtils;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.intfc.SettingsService;

import java.util.HashMap;
import java.util.Map;

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
                                         .setTimeout(Durations.fromMillis(180000))
                                         .build();

    return perpetualTaskService.createTask(PerpetualTaskType.K8S_WATCH, accountId, clientContext, schedule, false);
  }

  @Override
  public boolean delete(String accountId, String taskId) {
    // TODO(Tang) delete
    return false;
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

    // prepare the params for the Watch Task
    return K8sWatchTaskParams.newBuilder()
        .setCloudProviderId(cloudProviderId)
        .setK8SClusterConfig(bytes)
        .setK8SResourceKind(k8sResourceKind)
        .build();
  }
}
