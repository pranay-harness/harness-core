package software.wings.service.impl;

import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.infrastructure.AwsHost.Builder.anAwsHost;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import software.wings.beans.Base;
import software.wings.beans.infrastructure.AwsInfrastructureProviderConfig;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure.InfrastructureType;
import software.wings.beans.infrastructure.InfrastructureProviderConfig;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureProvider;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 10/4/16.
 */
@Singleton
public class AwsInfrastructureProviderImpl implements InfrastructureProvider {
  @Override
  public boolean infraTypeSupported(InfrastructureType infrastructureType) {
    return infrastructureType.equals(InfrastructureType.AWS);
  }

  @Override
  public List<Host> getAllHost(InfrastructureProviderConfig infrastructureProviderConfig) {
    if (infrastructureProviderConfig == null
        || !(infrastructureProviderConfig instanceof AwsInfrastructureProviderConfig)) {
      throw new WingsException(INVALID_ARGUMENT, "message", "InvalidConfiguration");
    }

    AwsInfrastructureProviderConfig awsConfig = (AwsInfrastructureProviderConfig) infrastructureProviderConfig;

    AmazonEC2Client amazonEC2Client =
        new AmazonEC2Client(new BasicAWSCredentials(awsConfig.getAccessKey(), awsConfig.getSecretKey()));
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
    return awsHosts;
  }
}
