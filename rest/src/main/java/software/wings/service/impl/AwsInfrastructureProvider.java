package software.wings.service.impl;

import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.infrastructure.AwsHost.Builder.anAwsHost;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import software.wings.beans.AwsConfig;
import software.wings.beans.Base;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureProvider;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 10/4/16.
 */
@Singleton
public class AwsInfrastructureProvider implements InfrastructureProvider {
  @Inject private AwsHelperService awsHelperService;

  @Override
  public PageResponse<Host> listHosts(SettingAttribute computeProviderSetting, PageRequest<Host> req) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AwsConfig)) {
      throw new WingsException(INVALID_ARGUMENT, "message", "InvalidConfiguration");
    }

    AwsConfig awsConfig = (AwsConfig) computeProviderSetting.getValue();

    AmazonEC2Client amazonEC2Client =
        awsHelperService.getAmazonEc2Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances();

    List<Host> awsHosts = describeInstancesResult.getReservations()
                              .stream()
                              .flatMap(reservation -> reservation.getInstances().stream())
                              .map(instance
                                  -> anAwsHost()
                                         .withAppId(Base.GLOBAL_APP_ID)
                                         .withHostName(instance.getPrivateDnsName())
                                         .withInstance(instance)
                                         .build())
                              .collect(Collectors.toList());
    return PageResponse.Builder.aPageResponse().withResponse(awsHosts).build();
  }
}
