package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.AmazonECSException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.protocol.AwsElbListener;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.api.HostElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEc2HelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsElbHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsIamHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsLambdaHelperServiceManager;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by anubhaw on 10/4/16.
 */
@Singleton
public class AwsInfrastructureProvider implements InfrastructureProvider {
  private static final Logger logger = LoggerFactory.getLogger(AwsInfrastructureProvider.class);

  @Inject private AwsUtils awsUtils;
  @Inject private HostService hostService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private SecretManager secretManager;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private AwsHelperService awsHelperService;
  @Inject private AwsElbHelperServiceManager awsElbHelperServiceManager;
  @Inject private AwsIamHelperServiceManager awsIamHelperServiceManager;
  @Inject private AwsEc2HelperServiceManager awsEc2HelperServiceManager;
  @Inject private AwsAsgHelperServiceManager awsAsgHelperServiceManager;
  @Inject private AwsLambdaHelperServiceManager awsLambdaHelperServiceManager;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public PageResponse<Host> listHosts(AwsInfrastructureMapping awsInfrastructureMapping,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, PageRequest<Host> req) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    DescribeInstancesResult describeInstancesResult;
    List<Instance> instances;
    if (awsInfrastructureMapping.isProvisionInstances()) {
      instances = listAutoScaleHosts(awsInfrastructureMapping, awsConfig, encryptedDataDetails);
    } else {
      instances = listFilteredHosts(awsInfrastructureMapping, awsConfig, encryptedDataDetails);
    }
    if (isNotEmpty(instances)) {
      List<Host> awsHosts =
          instances.stream()
              .map(instance -> {
                DeploymentType deploymentType = serviceResourceService.getDeploymentType(
                    awsInfrastructureMapping, null, awsInfrastructureMapping.getServiceId());

                return aHost()
                    .withHostName(awsUtils.getHostnameFromPrivateDnsName(instance.getPrivateDnsName()))
                    .withPublicDns(awsInfrastructureMapping.isUsePublicDns() ? instance.getPublicDnsName()
                                                                             : instance.getPrivateDnsName())
                    .withEc2Instance(instance)
                    .withAppId(awsInfrastructureMapping.getAppId())
                    .withEnvId(awsInfrastructureMapping.getEnvId())
                    .withHostConnAttr(DeploymentType.SSH.equals(deploymentType)
                            ? awsInfrastructureMapping.getHostConnectionAttrs()
                            : null)
                    .withWinrmConnAttr(DeploymentType.WINRM.equals(deploymentType)
                            ? awsInfrastructureMapping.getHostConnectionAttrs()
                            : null)
                    .withInfraMappingId(awsInfrastructureMapping.getUuid())
                    .withServiceTemplateId(awsInfrastructureMapping.getServiceTemplateId())
                    .build();
              })
              .collect(toList());
      if (awsInfrastructureMapping.getHostNameConvention() != null
          && !awsInfrastructureMapping.getHostNameConvention().equals(Constants.DEFAULT_AWS_HOST_NAME_CONVENTION)) {
        awsHosts.forEach(h -> {
          HostElement hostElement = aHostElement().withEc2Instance(h.getEc2Instance()).build();

          final Map<String, Object> contextMap = new HashMap();
          contextMap.put("host", hostElement);
          h.setHostName(
              awsUtils.getHostnameFromConvention(contextMap, awsInfrastructureMapping.getHostNameConvention()));
        });
      }
      return aPageResponse().withResponse(awsHosts).build();
    }
    return aPageResponse().withResponse(emptyList()).build();
  }

  private List<Instance> listAutoScaleHosts(AwsInfrastructureMapping awsInfrastructureMapping, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails) {
    if (awsInfrastructureMapping.isSetDesiredCapacity()) {
      List<Instance> instances = new ArrayList<>();
      for (int i = 0; i < awsInfrastructureMapping.getDesiredCapacity(); i++) {
        int instanceNum = i + 1;
        instances.add(new Instance()
                          .withPrivateDnsName("private-dns-" + instanceNum)
                          .withPublicDnsName("public-dns-" + instanceNum));
      }
      return instances;
    } else {
      return awsAsgHelperServiceManager.listAutoScalingGroupInstances(awsConfig, encryptedDataDetails,
          awsInfrastructureMapping.getRegion(), awsInfrastructureMapping.getAutoScalingGroupName(),
          awsInfrastructureMapping.getAppId());
    }
  }

  public List<Instance> listFilteredInstances(AwsInfrastructureMapping awsInfrastructureMapping, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails) {
    return listFilteredHosts(awsInfrastructureMapping, awsConfig, encryptedDataDetails);
  }

  private List<Instance> listFilteredHosts(AwsInfrastructureMapping awsInfrastructureMapping, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails) {
    DeploymentType deploymentType = serviceResourceService.getDeploymentType(
        awsInfrastructureMapping, null, awsInfrastructureMapping.getServiceId());
    List<Filter> filters = awsUtils.getAwsFilters(awsInfrastructureMapping, deploymentType);
    try {
      return awsEc2HelperServiceManager.listEc2Instances(awsConfig, encryptedDataDetails,
          awsInfrastructureMapping.getRegion(), filters, awsInfrastructureMapping.getAppId());
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String dnsName) {
    hostService.deleteByDnsName(appId, infraMappingId, dnsName);
  }

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {
    hostService.updateHostConnectionAttrByInfraMapping(infrastructureMapping, hostConnectionAttrs);
  }

  private AwsConfig validateAndGetAwsConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AwsConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }

    return (AwsConfig) computeProviderSetting.getValue();
  }

  @Override
  public Host saveHost(Host host) {
    return hostService.saveHost(host);
  }

  public List<Host> maybeSetAutoScaleCapacityAndGetHosts(String appId, String workflowExecutionId,
      AwsInfrastructureMapping infrastructureMapping, SettingAttribute computeProviderSetting) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, appId, workflowExecutionId);

    if (infrastructureMapping.isSetDesiredCapacity()) {
      awsHelperService.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails,
          infrastructureMapping.getRegion(), infrastructureMapping.getAutoScalingGroupName(),
          infrastructureMapping.getDesiredCapacity(), new ManagerExecutionLogCallback());
    }

    List<Instance> instances = awsAsgHelperServiceManager.listAutoScalingGroupInstances(awsConfig, encryptionDetails,
        infrastructureMapping.getRegion(), infrastructureMapping.getAutoScalingGroupName(),
        infrastructureMapping.getAppId());
    return instances.stream()
        .map(instance
            -> aHost()
                   .withHostName(awsUtils.getHostnameFromPrivateDnsName(instance.getPrivateDnsName()))
                   .withPublicDns(infrastructureMapping.isUsePublicDns() ? instance.getPublicDnsName()
                                                                         : instance.getPrivateDnsName())
                   .withEc2Instance(instance)
                   .withAppId(infrastructureMapping.getAppId())
                   .withEnvId(infrastructureMapping.getEnvId())
                   .withHostConnAttr(infrastructureMapping.getHostConnectionAttrs())
                   .withInfraMappingId(infrastructureMapping.getUuid())
                   .withServiceTemplateId(infrastructureMapping.getServiceTemplateId())
                   .build())
        .collect(toList());
  }

  public List<String> listInstanceTypes(SettingAttribute computeProviderSetting) {
    return mainConfiguration.getAwsInstanceTypes();
  }

  public List<String> listIAMInstanceRoles(SettingAttribute computeProviderSetting, String appId) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsIamHelperServiceManager.listIamInstanceRoles(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), appId);
  }

  public List<String> listLoadBalancers(SettingAttribute computeProviderSetting, String region, String appId) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsElbHelperServiceManager.listApplicationLoadBalancers(awsConfig,
        secretManager.getEncryptionDetails(awsConfig, isNotEmpty(appId) ? appId : null, null), region, appId);
  }

  public List<String> listElasticBalancers(SettingAttribute computeProviderSetting, String region, String appId) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsElbHelperServiceManager.listElasticLoadBalancers(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, appId);
  }

  public List<String> listNetworkBalancers(SettingAttribute computeProviderSetting, String region, String appId) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsElbHelperServiceManager.listNetworkLoadBalancers(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, appId);
  }

  public List<String> listClassicLoadBalancers(SettingAttribute computeProviderSetting, String region, String appId) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsElbHelperServiceManager.listClassicLoadBalancers(awsConfig,
        secretManager.getEncryptionDetails(awsConfig, isNotEmpty(appId) ? appId : null, null), region, appId);
  }

  public List<String> listClassicLoadBalancers(String accessKey, char[] secretKey, String region) {
    AwsConfig awsConfig = AwsConfig.builder().accessKey(accessKey).secretKey(secretKey).build();
    return awsElbHelperServiceManager.listApplicationLoadBalancers(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, "");
  }

  public Map<String, String> listTargetGroups(
      SettingAttribute computeProviderSetting, String region, String loadBalancerName, String appId) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsElbHelperServiceManager.listTargetGroupsForAlb(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, loadBalancerName, appId);
  }

  public List<AwsElbListener> listListeners(
      SettingAttribute computeProviderSetting, String region, String loadBalancerName, String appId) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsElbHelperServiceManager.listListenersForElb(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, loadBalancerName, appId);
  }

  public List<String> listLambdaFunctions(SettingAttribute computeProviderSetting, String region) {
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    return awsLambdaHelperServiceManager.listLambdaFunctions(
        awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region);
  }

  private void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
    logger.error("AWS API call exception", amazonServiceException);
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, new Throwable(amazonServiceException.getErrorMessage()));
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED)
          .addParam("message", amazonServiceException.getErrorMessage());
    } else if (amazonServiceException instanceof AmazonECSException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED)
          .addParam("message", amazonServiceException.getErrorMessage());
    }
    logger.error("Unhandled aws exception");
    throw new WingsException(ErrorCode.ACCESS_DENIED).addParam("message", amazonServiceException.getErrorMessage());
  }

  public Set<String> listTags(SettingAttribute computeProviderSetting, String region) {
    try {
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      return awsEc2HelperServiceManager.listTags(
          awsConfig, secretManager.getEncryptionDetails(awsConfig, null, null), region, "");
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  public List<String> listAutoScalingGroups(SettingAttribute computeProviderSetting, String region, String appId) {
    try {
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      return awsAsgHelperServiceManager.listAutoScalingGroupNames(
          awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, appId);
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }
}