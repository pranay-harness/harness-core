package software.wings.sm.states;

import static org.apache.commons.lang.StringUtils.isBlank;
import static software.wings.api.ElbStateExecutionData.Builder.anElbStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.jexl3.JxltEngine.Exception;
import software.wings.api.InstanceElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.stencils.DefaultValue;

/**
 * Created by peeyushaggarwal on 9/9/16.
 */
public abstract class ElasticLoadBalancerBaseState extends State {
  private String loadBalancerName;

  private String accessKey;

  private String secretKey;

  private Regions region = Regions.US_EAST_1;

  private String baseUrl;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   * @param stateType the state type
   */
  public ElasticLoadBalancerBaseState(String name, String stateType) {
    super(name, stateType);
  }

  /**
   * Gets load balancer name.
   *
   * @return the load balancer name
   */
  @Attributes(title = "Load Balancer Name")
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Sets load balancer name.
   *
   * @param loadBalancerName the load balancer name
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Gets access key.
   *
   * @return the access key
   */
  @Attributes(title = "AWS Access Key")
  public String getAccessKey() {
    return accessKey;
  }

  /**
   * Sets access key.
   *
   * @param accessKey the access key
   */
  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  /**
   * Gets secret key.
   *
   * @return the secret key
   */
  @Attributes(title = "AWS Secret Key")
  public String getSecretKey() {
    return secretKey;
  }

  /**
   * Sets secret key.
   *
   * @param secretKey the secret key
   */
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  /**
   * Gets region.
   *
   * @return the region
   */
  @DefaultValue("US_EAST_1")
  @Attributes(title = "Region")
  public Regions getRegion() {
    return region;
  }

  /**
   * Sets region.
   *
   * @param region the region
   */
  public void setRegion(Regions region) {
    this.region = region;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionStatus status;
    String errorMessage = null;
    AmazonWebServiceResult<ResponseMetadata> result = null;
    String hostName = null;
    try {
      InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);
      instanceElement.getHostElement();

      hostName = instanceElement.getHostElement().getHostName();

      String instanceId = getInstanceId(hostName);

      AmazonElasticLoadBalancingClient elbClient =
          (AmazonElasticLoadBalancingClient) AmazonElasticLoadBalancingClientBuilder.standard()
              .withRegion(region)
              .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
              .build();

      result = doOperation(instanceId, elbClient);
      status = getExecutionStatus(result, instanceId);
    } catch (Exception e) {
      errorMessage = e.getMessage();
      status = ExecutionStatus.FAILED;
    }

    return anExecutionResponse()
        .withStateExecutionData(anElbStateExecutionData().withHostName(hostName).build())
        .withExecutionStatus(status)
        .withErrorMessage(errorMessage)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @SchemaIgnore
  @Override
  public ContextElementType getRequiredContextElementType() {
    return ContextElementType.INSTANCE;
  }

  /**
   * Gets execution status.
   *
   * @param result     the result
   * @param instanceId the instance id
   * @return the execution status
   */
  protected abstract ExecutionStatus getExecutionStatus(
      AmazonWebServiceResult<ResponseMetadata> result, String instanceId);

  /**
   * Do operation amazon web service result.
   *
   * @param instanceId the instance id
   * @param elbClient  the elb client
   * @return the amazon web service result
   */
  protected abstract AmazonWebServiceResult<ResponseMetadata> doOperation(
      String instanceId, AmazonElasticLoadBalancingClient elbClient);

  private String getInstanceId(String hostName) {
    AmazonEC2Client ec2Client =
        (AmazonEC2Client) AmazonEC2ClientBuilder.standard()
            .withRegion(region)
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
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
