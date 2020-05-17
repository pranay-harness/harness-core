package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsCodeDeployListAppResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployListAppRevisionRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListAppRevisionResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentConfigResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentGroupRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentGroupResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentInstancesRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentInstancesResponse;
import software.wings.service.impl.aws.model.AwsCodeDeployRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployRequest.AwsCodeDeployRequestType;
import software.wings.service.impl.aws.model.AwsCodeDeployS3LocationData;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsCodeDeployHelperServiceDelegate;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class AwsCodeDeployTask extends AbstractDelegateRunnableTask {
  @Inject private AwsCodeDeployHelperServiceDelegate awsCodeDeployHelperServiceDelegate;

  public AwsCodeDeployTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsCodeDeployRequest request = (AwsCodeDeployRequest) parameters[0];
    AwsCodeDeployRequestType requestType = request.getRequestType();
    try {
      switch (requestType) {
        case LIST_APPLICATIONS: {
          List<String> applications = awsCodeDeployHelperServiceDelegate.listApplications(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsCodeDeployListAppResponse.builder().applications(applications).executionStatus(SUCCESS).build();
        }
        case LIST_DEPLOYMENT_CONFIGURATION: {
          List<String> configs = awsCodeDeployHelperServiceDelegate.listDeploymentConfiguration(
              request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion());
          return AwsCodeDeployListDeploymentConfigResponse.builder()
              .deploymentConfig(configs)
              .executionStatus(SUCCESS)
              .build();
        }
        case LIST_DEPLOYMENT_GROUP: {
          List<String> groups = awsCodeDeployHelperServiceDelegate.listDeploymentGroups(request.getAwsConfig(),
              request.getEncryptionDetails(), request.getRegion(),
              ((AwsCodeDeployListDeploymentGroupRequest) request).getAppName());
          return AwsCodeDeployListDeploymentGroupResponse.builder()
              .deploymentGroups(groups)
              .executionStatus(SUCCESS)
              .build();
        }
        case LIST_DEPLOYMENT_INSTANCES: {
          List<Instance> instances = awsCodeDeployHelperServiceDelegate.listDeploymentInstances(request.getAwsConfig(),
              request.getEncryptionDetails(), request.getRegion(),
              ((AwsCodeDeployListDeploymentInstancesRequest) request).getDeploymentId());
          return AwsCodeDeployListDeploymentInstancesResponse.builder()
              .instances(instances)
              .executionStatus(SUCCESS)
              .build();
        }
        case LIST_APP_REVISION: {
          AwsCodeDeployS3LocationData s3LocationData =
              awsCodeDeployHelperServiceDelegate.listAppRevision(request.getAwsConfig(), request.getEncryptionDetails(),
                  request.getRegion(), ((AwsCodeDeployListAppRevisionRequest) request).getAppName(),
                  ((AwsCodeDeployListAppRevisionRequest) request).getDeploymentGroupName());
          return AwsCodeDeployListAppRevisionResponse.builder()
              .s3LocationData(s3LocationData)
              .executionStatus(SUCCESS)
              .build();
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