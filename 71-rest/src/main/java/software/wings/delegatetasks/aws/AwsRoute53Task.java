package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Misc.getMessage;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesRequest;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesResponse;
import software.wings.service.impl.aws.model.AwsRoute53Request;
import software.wings.service.impl.aws.model.AwsRoute53Request.AwsRoute53RequestType;
import software.wings.service.intfc.aws.delegate.AwsRoute53HelperServiceDelegate;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsRoute53Task extends AbstractDelegateRunnableTask {
  @Inject private AwsRoute53HelperServiceDelegate awsRoute53HelperServiceDelegate;

  public AwsRoute53Task(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsRoute53Request request = (AwsRoute53Request) parameters[0];
    try {
      AwsRoute53RequestType requestType = request.getRequestType();
      switch (requestType) {
        case LIST_HOSTED_ZONES: {
          List<AwsRoute53HostedZoneData> hostedZones =
              awsRoute53HelperServiceDelegate.listHostedZones(request.getAwsConfig(), request.getEncryptionDetails(),
                  ((AwsRoute53ListHostedZonesRequest) request).getRegion());
          return AwsRoute53ListHostedZonesResponse.builder().hostedZones(hostedZones).executionStatus(SUCCESS).build();
        }
        default: { throw new InvalidRequestException("Invalid request type [" + requestType + "]", USER); }
      }
    } catch (WingsException exception) {
      throw exception;
    } catch (Exception ex) {
      throw new InvalidRequestException(getMessage(ex), USER);
    }
  }
}
