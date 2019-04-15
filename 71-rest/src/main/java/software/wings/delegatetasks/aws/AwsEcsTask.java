package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsEcsListClustersResponse;
import software.wings.service.impl.aws.model.AwsEcsRequest;
import software.wings.service.impl.aws.model.AwsEcsRequest.AwsEcsRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class AwsEcsTask extends AbstractDelegateRunnableTask {
  @Inject private AwsEcsHelperServiceDelegate ecsHelperServiceDelegate;

  public AwsEcsTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    AwsEcsRequest request = (AwsEcsRequest) parameters;
    try {
      AwsEcsRequestType requestType = request.getRequestType();
      switch (requestType) {
        case LIST_CLUSTERS: {
          List<String> clusters = ecsHelperServiceDelegate.listClusters(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsEcsListClustersResponse.builder().clusters(clusters).executionStatus(SUCCESS).build();
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (WingsException exception) {
      throw exception;
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}