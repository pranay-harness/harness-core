package io.harness.batch.processing.tasklet;

import static io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils.populateNodePoolNameFromLabel;
import static io.harness.ccm.cluster.entities.K8sWorkload.encodeDotsInKey;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.batch.processing.tasklet.reader.PublishedMessageReader;
import io.harness.batch.processing.tasklet.support.HarnessServiceInfoFetcher;
import io.harness.batch.processing.tasklet.util.K8sResourceUtils;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.perpetualtask.k8s.watch.PodInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.instance.HarnessServiceInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class K8sPodInfoTasklet implements Tasklet {
  @Autowired private BatchMainConfig config;
  @Autowired private InstanceDataDao instanceDataDao;
  @Autowired private WorkloadRepository workloadRepository;
  @Autowired private InstanceDataService instanceDataService;
  @Autowired private PublishedMessageDao publishedMessageDao;
  @Autowired private HarnessServiceInfoFetcher harnessServiceInfoFetcher;
  private static final String POD = "Pod";
  private static final String KUBE_SYSTEM_NAMESPACE = "kube-system";
  private static final String KUBE_PROXY_POD_PREFIX = "kube-proxy";

  private JobParameters parameters;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    Long startTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE);
    Long endTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    int batchSize = config.getBatchQueryConfig().getQueryBatchSize();

    String messageType = EventTypeConstants.K8S_POD_INFO;
    List<PublishedMessage> publishedMessageList;
    PublishedMessageReader publishedMessageReader =
        new PublishedMessageReader(publishedMessageDao, accountId, messageType, startTime, endTime, batchSize);
    do {
      publishedMessageList = publishedMessageReader.getNext();
      publishedMessageList.stream()
          .map(this ::processPodInfoMessage)
          .filter(instanceInfo -> instanceInfo.getMetaData().containsKey(InstanceMetaDataConstants.INSTANCE_CATEGORY))
          .forEach(instanceInfo -> instanceDataDao.upsert(instanceInfo));
    } while (publishedMessageList.size() == batchSize);
    return null;
  }

  public InstanceInfo processPodInfoMessage(PublishedMessage publishedMessage) {
    try {
      return process(publishedMessage);
    } catch (Exception ex) {
      logger.error("K8sPodInfoTasklet Exception ", ex);
    }
    return InstanceInfo.builder().metaData(Collections.emptyMap()).build();
  }

  public InstanceInfo process(PublishedMessage publishedMessage) {
    String accountId = publishedMessage.getAccountId();
    PodInfo podInfo = (PodInfo) publishedMessage.getMessage();
    String podUid = podInfo.getPodUid();
    String clusterId = podInfo.getClusterId();

    InstanceData existingInstanceData = instanceDataService.fetchInstanceData(accountId, clusterId, podUid);
    if (null != existingInstanceData) {
      return InstanceInfo.builder().metaData(Collections.emptyMap()).build();
    }

    String workloadName = podInfo.getTopLevelOwner().getName();
    String workloadType = podInfo.getTopLevelOwner().getKind();

    if (podInfo.getNamespace().equals(KUBE_SYSTEM_NAMESPACE)
        && podInfo.getPodName().startsWith(KUBE_PROXY_POD_PREFIX)) {
      workloadName = KUBE_PROXY_POD_PREFIX;
      workloadType = POD;
    }

    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.GCP.name());
    metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, podInfo.getNodeName());
    metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.K8S.name());
    metaData.put(InstanceMetaDataConstants.NAMESPACE, podInfo.getNamespace());
    metaData.put(
        InstanceMetaDataConstants.WORKLOAD_NAME, workloadName.equals("") ? podInfo.getPodName() : workloadName);
    metaData.put(InstanceMetaDataConstants.WORKLOAD_TYPE, workloadType.equals("") ? POD : workloadType);

    InstanceData instanceData = instanceDataService.fetchInstanceDataWithName(
        accountId, clusterId, podInfo.getNodeName(), publishedMessage.getOccurredAt());
    if (null != instanceData) {
      Map<String, String> nodeMetaData = instanceData.getMetaData();
      metaData.put(InstanceMetaDataConstants.REGION, nodeMetaData.get(InstanceMetaDataConstants.REGION));
      metaData.put(InstanceMetaDataConstants.ZONE, nodeMetaData.get(InstanceMetaDataConstants.ZONE));
      metaData.put(
          InstanceMetaDataConstants.INSTANCE_FAMILY, nodeMetaData.get(InstanceMetaDataConstants.INSTANCE_FAMILY));
      metaData.put(
          InstanceMetaDataConstants.INSTANCE_CATEGORY, nodeMetaData.get(InstanceMetaDataConstants.INSTANCE_CATEGORY));
      metaData.put(
          InstanceMetaDataConstants.CLOUD_PROVIDER, nodeMetaData.get(InstanceMetaDataConstants.CLOUD_PROVIDER));
      metaData.put(
          InstanceMetaDataConstants.OPERATING_SYSTEM, nodeMetaData.get(InstanceMetaDataConstants.OPERATING_SYSTEM));
      metaData.put(InstanceMetaDataConstants.POD_NAME, podInfo.getPodName());
      metaData.put(InstanceMetaDataConstants.ACTUAL_PARENT_RESOURCE_ID, instanceData.getInstanceId());
      metaData.put(
          InstanceMetaDataConstants.PARENT_RESOURCE_CPU, String.valueOf(instanceData.getTotalResource().getCpuUnits()));
      metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY,
          String.valueOf(instanceData.getTotalResource().getMemoryMb()));
      if (null != nodeMetaData.get(InstanceMetaDataConstants.COMPUTE_TYPE)) {
        metaData.put(InstanceMetaDataConstants.COMPUTE_TYPE, nodeMetaData.get(InstanceMetaDataConstants.COMPUTE_TYPE));
      }
      if (null != instanceData.getCloudProviderInstanceId()) {
        metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER_INSTANCE_ID, instanceData.getCloudProviderInstanceId());
      }
      populateNodePoolNameFromLabel(nodeMetaData, metaData);
    } else {
      logger.warn("Node detail not found settingId {} node name {} podid {} podname {}", podInfo.getCloudProviderId(),
          podInfo.getNodeName(), podUid, podInfo.getPodName());
    }

    Map<String, String> labelsMap = podInfo.getLabelsMap();
    HarnessServiceInfo harnessServiceInfo = harnessServiceInfoFetcher
                                                .fetchHarnessServiceInfo(accountId, podInfo.getCloudProviderId(),
                                                    podInfo.getNamespace(), podInfo.getPodName(), labelsMap)
                                                .orElse(null);

    try {
      workloadRepository.savePodWorkload(accountId, podInfo);
    } catch (Exception ex) {
      logger.error("Error while saving pod workload {} {}", podInfo.getCloudProviderId(), podUid);
    }

    Resource resource = K8sResourceUtils.getResource(podInfo.getTotalResource().getRequestsMap());
    Resource resourceLimit = Resource.builder().cpuUnits(0.0).memoryMb(0.0).build();
    if (!isEmpty(podInfo.getTotalResource().getLimitsMap())) {
      resourceLimit = K8sResourceUtils.getResource(podInfo.getTotalResource().getLimitsMap());
    }

    Map<String, String> namespaceLabels = encodeDotsInKey(podInfo.getNamespaceLabelsMap());

    return InstanceInfo.builder()
        .accountId(accountId)
        .settingId(podInfo.getCloudProviderId())
        .instanceId(podUid)
        .clusterId(clusterId)
        .clusterName(podInfo.getClusterName())
        .instanceName(podInfo.getPodName())
        .instanceType(InstanceType.K8S_POD)
        .instanceState(InstanceState.INITIALIZING)
        .resource(resource)
        .resourceLimit(resourceLimit)
        .allocatableResource(resource)
        .metaData(metaData)
        .labels(labelsMap)
        .namespaceLabels(namespaceLabels)
        .harnessServiceInfo(harnessServiceInfo)
        .build();
  }
}
