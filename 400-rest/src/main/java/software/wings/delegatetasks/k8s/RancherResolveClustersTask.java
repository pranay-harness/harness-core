package software.wings.delegatetasks.k8s;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidArgumentsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jose4j.lang.JoseException;
import software.wings.beans.RancherConfig;
import software.wings.helpers.ext.k8s.request.RancherResolveClustersTaskParameters;
import software.wings.helpers.ext.k8s.response.RancherResolveClustersResponse;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@Slf4j
@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class RancherResolveClustersTask extends AbstractDelegateRunnableTask {

  @Inject
  private transient K8sTaskHelper k8sTaskHelper;
  @Inject private EncryptionService encryptionService;

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
      throw new InvalidArgumentsException(
              Pair.of("parameters", "Must be instance of RancherResolveClustersTaskParameters"));
    }
    RancherResolveClustersTaskParameters rancherResolveClustersTaskParameters = (RancherResolveClustersTaskParameters) parameters;

    RancherConfig rancherConfig = rancherResolveClustersTaskParameters.getRancherConfig();

    encryptionService.decrypt(rancherConfig, rancherResolveClustersTaskParameters.getEncryptedDataDetails(), false);

    List<String> clusters = k8sTaskHelper.resolveRancherClusters(rancherConfig, rancherResolveClustersTaskParameters.getClusterSelectionCriteria());

    RancherResolveClustersResponse rancherResolveClustersResponse = RancherResolveClustersResponse.builder().clusters(clusters).build();
    return rancherResolveClustersResponse;
  }
}

