package software.wings.delegatetasks.rancher;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidArgumentsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jose4j.lang.JoseException;
import software.wings.helpers.ext.k8s.request.RancherResolveClustersTaskParameters;
import software.wings.helpers.ext.k8s.response.RancherResolveClustersResponse;
import software.wings.infra.RancherKubernetesInfrastructure.ClusterSelectionCriteriaEntry;

import java.io.IOException;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@Slf4j
@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class RancherResolveClustersTask extends AbstractDelegateRunnableTask {

  @Inject private RancherTaskHelper helper;

  public RancherResolveClustersTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
                  Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, postExecute, preExecute);
  }

  @Override
  public RancherResolveClustersResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public RancherResolveClustersResponse run(TaskParameters parameters) throws IOException, JoseException {
    if (!(parameters instanceof RancherResolveClustersTaskParameters)) {
      throw new InvalidArgumentsException(Pair.of("parameters",
              "Must be instance of RancherResolveClustersTaskParameters"));
    }

    RancherResolveClustersTaskParameters resolveTaskParams = (RancherResolveClustersTaskParameters) parameters;

    RancherClusterDataResponse rancherClusterData;
    try {
      rancherClusterData = helper.resolveRancherClusters(
              resolveTaskParams.getRancherConfig(),
              resolveTaskParams.getEncryptedDataDetails());
    } catch (Exception e) {
      log.error("Caught exception while fetching clusters data from rancher", e);
      RancherResolveClustersResponse response = RancherResolveClustersResponse.builder()
              .executionStatus(ExecutionStatus.FAILED)
              .build();
      response.setErrorMessage(e.getLocalizedMessage());
      return response;
    }

    Map<String, Set<String>> selectionParams = getClusterSelectionParams(resolveTaskParams.getClusterSelectionCriteria());
    List<String> filteredClusters = filterClustersForCriteria(rancherClusterData, selectionParams);

    return RancherResolveClustersResponse.builder()
            .executionStatus(ExecutionStatus.SUCCESS)
            .clusters(filteredClusters)
            .build();
  }

  private List<String> filterClustersForCriteria(final RancherClusterDataResponse rancherClusterData,
                                                 final Map<String, Set<String>> selectionParams) {
    if (MapUtils.isEmpty(selectionParams)) {
      return rancherClusterData.getData().stream()
              .map(RancherClusterDataResponse.ClusterData::getName)
              .collect(Collectors.toList());
    }

    List<String> filteredClusters = new ArrayList<>();
    rancherClusterData.getData().forEach(clusterData -> {
      if (!selectionParams.keySet().stream()
              .allMatch(labelKey -> clusterData.getLabels().containsKey(labelKey))) {
        return;
      }

      if (!selectionParams.keySet().stream()
              .allMatch(labelKey -> selectionParams.get(labelKey).contains(clusterData.getLabels().get(labelKey)))) {
        return;
      }

      filteredClusters.add(clusterData.getName());
    });

    return filteredClusters;
  }

  private Map<String, Set<String>> getClusterSelectionParams(final List<ClusterSelectionCriteriaEntry> clusterSelectionCriteria) {
    if (CollectionUtils.isEmpty(clusterSelectionCriteria)) {
      return new HashMap<>();
    }

    return clusterSelectionCriteria.stream()
            .collect(Collectors.toMap(clusterSelectionCriteriaEntry -> clusterSelectionCriteriaEntry.getLabelName().trim(),
                    entry -> {
                      List<String> labels = Arrays.asList(entry.getLabelValues().split(","));
                      Set<String> trimmedLabels = new HashSet<>();
                      labels.forEach(label -> trimmedLabels.add(label.trim()));

                      return trimmedLabels;
                    }));
  }
}