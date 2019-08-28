package io.harness.perpetualtask.k8s.watch;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceFactory;
import io.harness.serializer.KryoUtils;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.intfc.SettingsService;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

@Singleton
public class K8sWatchServiceImpl implements K8sWatchService, PerpetualTaskServiceClient {
  private final PerpetualTaskService perpetualTaskService;
  private final SettingsService settingsService;
  private final K8sClusterConfigFactory k8sClusterConfigFactory;

  @Inject
  public K8sWatchServiceImpl(PerpetualTaskServiceFactory serviceFactory, SettingsService settingsService,
      K8sClusterConfigFactory k8sClusterConfigFactory) {
    this.perpetualTaskService = serviceFactory.getInstance();
    this.settingsService = settingsService;
    this.k8sClusterConfigFactory = k8sClusterConfigFactory;
  }

  @Override
  public String create(K8sWatchTaskParams params) {
    String clientHandle = generateWatchHandle(params);
    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(0))
                                         .setTimeout(Durations.fromMillis(60))
                                         .build();
    perpetualTaskService.createTask(this.getClass().getSimpleName(), clientHandle, schedule);
    return clientHandle;
  }

  @Override
  public List<String> list() {
    return null;
  }

  @Override
  public void delete(String clientHandle) {
    perpetualTaskService.deleteTask(this.getClass().getSimpleName(), clientHandle);
  }

  @Override
  public PerpetualTaskParams getTaskParams(String clientHandle) {
    List<String> clientHandles = Arrays.asList(clientHandle.split(","));
    String cloudProviderId = clientHandles.get(0);
    String k8sResourceKind = clientHandles.get(1);

    // get the attribute from mongo db collection
    SettingAttribute settingAttribute = settingsService.get(cloudProviderId);
    K8sClusterConfig config = k8sClusterConfigFactory.getK8sClusterConfig(settingAttribute);
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(config));

    // prepare the params for the Watch Task
    K8sWatchTaskParams params = K8sWatchTaskParams.newBuilder()
                                    .setCloudProviderId(cloudProviderId)
                                    .setK8SClusterConfig(bytes)
                                    .setK8SResourceKind(k8sResourceKind)
                                    .build();

    // TODO: throw exception upon validation failure

    return PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(params)).build();
  }

  private String generateWatchHandle(K8sWatchTaskParams params) {
    StringJoiner joiner = new StringJoiner(",");
    joiner.add(params.getCloudProviderId()).add(params.getK8SResourceKind());
    return joiner.toString();
  }
}
