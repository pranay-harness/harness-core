package software.wings.delegatetasks.aws;

import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesRequest;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesResponse;
import software.wings.service.impl.aws.model.AwsEc2ListRegionsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListSGsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListSGsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListSubnetsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListSubnetsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListTagsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListTagsResponse;
import software.wings.service.impl.aws.model.AwsEc2ListVpcsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListVpcsResponse;
import software.wings.service.impl.aws.model.AwsEc2Request;
import software.wings.service.impl.aws.model.AwsEc2Request.AwsEc2RequestType;
import software.wings.service.impl.aws.model.AwsEc2ValidateCredentialsResponse;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsEc2Task extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AwsEc2Task.class);
  @Inject private AwsEc2HelperServiceDelegate ec2ServiceDelegate;

  public AwsEc2Task(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsEc2Request request = (AwsEc2Request) parameters[0];
    try {
      AwsEc2RequestType requestType = request.getRequestType();
      switch (requestType) {
        case VALIDATE_CREDENTIALS: {
          boolean validCredentials =
              ec2ServiceDelegate.validateAwsAccountCredential(request.getAwsConfig(), request.getEncryptionDetails());
          return AwsEc2ValidateCredentialsResponse.builder().valid(validCredentials).executionStatus(SUCCESS).build();
        }
        case LIST_REGIONS: {
          List<String> regions = ec2ServiceDelegate.listRegions(request.getAwsConfig(), request.getEncryptionDetails());
          return AwsEc2ListRegionsResponse.builder().regions(regions).executionStatus(SUCCESS).build();
        }
        case LIST_VPCS: {
          List<String> vpcs = ec2ServiceDelegate.listVPCs(
              request.getAwsConfig(), request.getEncryptionDetails(), ((AwsEc2ListVpcsRequest) request).getRegion());
          return AwsEc2ListVpcsResponse.builder().vpcs(vpcs).executionStatus(SUCCESS).build();
        }
        case LIST_SUBNETS: {
          List<String> subnets = ec2ServiceDelegate.listSubnets(request.getAwsConfig(), request.getEncryptionDetails(),
              ((AwsEc2ListSubnetsRequest) request).getRegion(), ((AwsEc2ListSubnetsRequest) request).getVpcIds());
          return AwsEc2ListSubnetsResponse.builder().subnets(subnets).executionStatus(SUCCESS).build();
        }
        case LIST_SGS: {
          List<String> securityGroups =
              ec2ServiceDelegate.listSGs(request.getAwsConfig(), request.getEncryptionDetails(),
                  ((AwsEc2ListSGsRequest) request).getRegion(), ((AwsEc2ListSGsRequest) request).getVpcIds());
          return AwsEc2ListSGsResponse.builder().securityGroups(securityGroups).executionStatus(SUCCESS).build();
        }
        case LIST_TAGS: {
          Set<String> tags = ec2ServiceDelegate.listTags(
              request.getAwsConfig(), request.getEncryptionDetails(), ((AwsEc2ListTagsRequest) request).getRegion());
          return AwsEc2ListTagsResponse.builder().tags(tags).executionStatus(SUCCESS).build();
        }
        case LIST_INSTANCES: {
          List<Instance> instances = ec2ServiceDelegate.listEc2Instances(request.getAwsConfig(),
              request.getEncryptionDetails(), ((AwsEc2ListInstancesRequest) request).getRegion(),
              ((AwsEc2ListInstancesRequest) request).getFilters());
          return AwsEc2ListInstancesResponse.builder().instances(instances).executionStatus(SUCCESS).build();
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