package software.wings.core.cloud;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.LoadBalancer;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.sm.ContextElement;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public class ElasticLoadBalancer implements LoadBalancer<ElasticLoadBalancerConfig> {
  @Override
  public boolean enableInstance(ElasticLoadBalancerConfig loadBalancerConfig, ContextElement contextElement) {
    HostElement hostElement = ((InstanceElement) contextElement).getHostElement();
    String hostName = hostElement.getHostName();
    String awsInstanceId = getInstanceId(hostName, loadBalancerConfig);
    AmazonElasticLoadBalancingClient elbClient =
        (AmazonElasticLoadBalancingClient) AmazonElasticLoadBalancingClientBuilder.standard()
            .withRegion(loadBalancerConfig.getRegion())
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                loadBalancerConfig.getAccessKey(), new String(loadBalancerConfig.getSecretKey()))))
            .build();

    RegisterInstancesWithLoadBalancerResult result =
        elbClient.registerInstancesWithLoadBalancer(new RegisterInstancesWithLoadBalancerRequest()
                                                        .withLoadBalancerName(loadBalancerConfig.getLoadBalancerName())
                                                        .withInstances(new Instance().withInstanceId(awsInstanceId)));
    return result.getInstances()
        .stream()
        .map(Instance::getInstanceId)
        .filter(s -> StringUtils.equals(s, awsInstanceId))
        .findFirst()
        .isPresent();
  }

  @Override
  public boolean disableInstance(ElasticLoadBalancerConfig loadBalancerConfig, ContextElement contextElement) {
    HostElement hostElement = ((InstanceElement) contextElement).getHostElement();
    String hostName = hostElement.getHostName();
    String awsInstanceId = getInstanceId(hostName, loadBalancerConfig);
    AmazonElasticLoadBalancingClient elbClient =
        (AmazonElasticLoadBalancingClient) AmazonElasticLoadBalancingClientBuilder.standard()
            .withRegion(loadBalancerConfig.getRegion())
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                loadBalancerConfig.getAccessKey(), new String(loadBalancerConfig.getSecretKey()))))
            .build();

    DeregisterInstancesFromLoadBalancerResult result = elbClient.deregisterInstancesFromLoadBalancer(
        new DeregisterInstancesFromLoadBalancerRequest()
            .withLoadBalancerName(loadBalancerConfig.getLoadBalancerName())
            .withInstances(new Instance().withInstanceId(awsInstanceId)));
    return !result.getInstances()
                .stream()
                .map(Instance::getInstanceId)
                .filter(s -> StringUtils.equals(s, awsInstanceId))
                .findFirst()
                .isPresent();
  }

  @Override
  public Class<ElasticLoadBalancerConfig> supportedConfig() {
    return ElasticLoadBalancerConfig.class;
  }

  private String getInstanceId(String hostName, ElasticLoadBalancerConfig config) {
    AmazonEC2Client ec2Client =
        (AmazonEC2Client) AmazonEC2ClientBuilder.standard()
            .withRegion(config.getRegion())
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(config.getAccessKey(), new String(config.getSecretKey()))))
            /*.withClientConfiguration(new ClientConfiguration().withDnsResolver(new DnsResolver() {
              @Override
              public InetAddress[] resolve(String host) throws UnknownHostException {
                return new InetAddress[] { InetAddress.getLoopbackAddress() };
              }
            }))*/
            .build();

    String instanceId = null;
    DescribeInstancesResult describeInstancesResult =
        ec2Client.describeInstances(new DescribeInstancesRequest().withFilters(
            new Filter().withName("private-dns-name").withValues(hostName + "*")));
    instanceId = describeInstancesResult.getReservations()
                     .stream()
                     .flatMap(reservation -> reservation.getInstances().stream())
                     .map(instance -> instance.getInstanceId())
                     .findFirst()
                     .orElse(instanceId);

    if (isBlank(instanceId)) {
      describeInstancesResult = ec2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter().withName("private-ip-address").withValues(hostName)));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(instance -> instance.getInstanceId())
                       .findFirst()
                       .orElse(instanceId);
    }

    if (isBlank(instanceId)) {
      describeInstancesResult = ec2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter().withName("dns-name").withValues(hostName + "*")));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(instance -> instance.getInstanceId())
                       .findFirst()
                       .orElse(instanceId);
    }

    if (isBlank(instanceId)) {
      describeInstancesResult = ec2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter().withName("ip-address").withValues(hostName)));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(instance -> instance.getInstanceId())
                       .findFirst()
                       .orElse(instanceId);
    }
    return instanceId;
  }
}
