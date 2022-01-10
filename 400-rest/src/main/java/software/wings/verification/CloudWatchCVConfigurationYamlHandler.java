/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.exception.WingsException;

import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.intfc.CloudWatchService;
import software.wings.sm.StateType;
import software.wings.verification.cloudwatch.CloudWatchCVConfigurationYaml;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CloudWatchCVConfigurationYamlHandler
    extends CVConfigurationYamlHandler<CloudWatchCVConfigurationYaml, CloudWatchCVServiceConfiguration> {
  @Inject CloudWatchService cloudWatchService;

  @Override
  public CloudWatchCVConfigurationYaml toYaml(CloudWatchCVServiceConfiguration bean, String appId) {
    CloudWatchCVConfigurationYaml yaml = CloudWatchCVConfigurationYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(StateType.CLOUD_WATCH.name());
    yaml.setLoadBalancerMetrics(bean.getLoadBalancerMetrics());
    yaml.setEcsMetrics(bean.getEcsMetrics());
    yaml.setLambdaFunctionsMetrics(bean.getLambdaFunctionsMetrics());
    yaml.setEc2InstanceNames(bean.getEc2InstanceNames());
    yaml.setEc2Metrics(bean.getEc2Metrics());
    yaml.setRegion(bean.getRegion());
    return yaml;
  }

  @Override
  public CloudWatchCVServiceConfiguration upsertFromYaml(
      ChangeContext<CloudWatchCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();

    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    CloudWatchCVServiceConfiguration bean = CloudWatchCVServiceConfiguration.builder().build();
    super.toBean(changeContext, bean, appId, yamlFilePath);

    CloudWatchCVConfigurationYaml yaml = changeContext.getYaml();

    try {
      validateYaml(yaml, accountId);

      bean.setEc2InstanceNames(yaml.getEc2InstanceNames());
      bean.setEc2Metrics(yaml.getEc2Metrics());
      bean.setEcsMetrics(yaml.getEcsMetrics());
      bean.setLambdaFunctionsMetrics(yaml.getLambdaFunctionsMetrics());
      bean.setLoadBalancerMetrics(yaml.getLoadBalancerMetrics());
      bean.setRegion(yaml.getRegion());
      bean.setStateType(StateType.CLOUD_WATCH);
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      bean.setUuid(generateUuid());
      cvConfigurationService.saveToDatabase(bean, true);
    }

    return bean;
  }

  private void validateYaml(CloudWatchCVConfigurationYaml yaml, String accountId) {
    SettingAttribute cloudWatchConnector =
        settingsService.getSettingAttributeByName(accountId, yaml.getConnectorName());

    String settingId = cloudWatchConnector.getUuid();
    String region = yaml.getRegion();

    if (isEmpty(yaml.getLoadBalancerMetrics()) && isEmpty(yaml.getEc2InstanceNames())
        && isEmpty(yaml.getLambdaFunctionsMetrics()) && isEmpty(yaml.getEcsMetrics())) {
      throw new WingsException("No metric provided in Configuration");
    }

    List<String> ec2InstanceNames = yaml.getEc2InstanceNames();
    if (isNotEmpty(ec2InstanceNames)) {
      Map<String, String> validInstances = cloudWatchService.getEC2Instances(settingId, region);
      Set<String> validInstanceNames = validInstances.keySet();
      ec2InstanceNames.forEach(instance -> {
        if (!validInstanceNames.contains(instance)) {
          throw new WingsException("Invalid EC2 Instance " + instance);
        }
      });
    }

    Map<String, List<CloudWatchMetric>> ecsMetrics = yaml.getEcsMetrics();
    if (isNotEmpty(ecsMetrics)) {
      List<String> validEcsClusters = cloudWatchService.getECSClusterNames(settingId, region);
      ecsMetrics.keySet().forEach(cluster -> {
        if (!validEcsClusters.contains(cluster)) {
          throw new WingsException("Invalid ECS Cluster " + cluster);
        }
      });
    }

    Map<String, List<CloudWatchMetric>> lambdaFunctionsMetrics = yaml.getLambdaFunctionsMetrics();
    if (isNotEmpty(lambdaFunctionsMetrics)) {
      List<String> validLambdaFunctions = cloudWatchService.getLambdaFunctionsNames(settingId, region);
      lambdaFunctionsMetrics.keySet().forEach(lambdaFunction -> {
        if (!validLambdaFunctions.contains(lambdaFunction)) {
          throw new WingsException("Invalid Lambda function " + lambdaFunction);
        }
      });
    }

    Map<String, List<CloudWatchMetric>> loadBalancerMetrics = yaml.getLoadBalancerMetrics();
    if (isNotEmpty(loadBalancerMetrics)) {
      Set<String> validLoadBalancers = cloudWatchService.getLoadBalancerNames(settingId, region);
      loadBalancerMetrics.keySet().forEach(loadBalancer -> {
        if (!validLoadBalancers.contains(loadBalancer)) {
          throw new WingsException("Invalid Load Balancer " + loadBalancer);
        }
      });
    }
  }

  @Override
  public Class getYamlClass() {
    return CloudWatchCVConfigurationYaml.class;
  }

  @Override
  public CloudWatchCVServiceConfiguration get(String accountId, String yamlFilePath) {
    return (CloudWatchCVServiceConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }
}
