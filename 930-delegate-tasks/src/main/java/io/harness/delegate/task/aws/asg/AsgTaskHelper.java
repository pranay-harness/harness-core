/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.aws.asg;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.asg.AsgSdkManager;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.logging.LogCallback;

import software.wings.service.impl.AwsUtils;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class AsgTaskHelper {
  @Inject private AwsUtils awsUtils;
  @Inject private TimeLimiter timeLimiter;

  public LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }

  public String getAsgLaunchTemplateContent(Map<String, List<String>> asgStoreManifestsContent) {
    return asgStoreManifestsContent.get("AsgLaunchTemplate").get(0);
  }

  public String getAsgConfigurationContent(Map<String, List<String>> asgStoreManifestsContent) {
    return asgStoreManifestsContent.get("AsgConfiguration").get(0);
  }

  public AutoScalingGroupContainer mapToAutoScalingGroupContainer(AutoScalingGroup autoScalingGroup) {
    return AutoScalingGroupContainer.builder()
        .autoScalingGroupName(autoScalingGroup.getAutoScalingGroupName())
        .launchTemplateName(autoScalingGroup.getLaunchTemplate().getLaunchTemplateName())
        .launchTemplateVersion(autoScalingGroup.getLaunchTemplate().getVersion())
        .autoScalingGroupInstanceList(
            autoScalingGroup.getInstances()
                .stream()
                .map(instance
                    -> AutoScalingGroupInstance.builder()
                           .autoScalingGroupName(autoScalingGroup.getAutoScalingGroupName())
                           .instanceId(instance.getInstanceId())
                           .instanceType(instance.getInstanceType())
                           .launchTemplateVersion(autoScalingGroup.getLaunchTemplate().getVersion())
                           .build())
                .collect(Collectors.toList()))
        .build();
  }

  public AsgSdkManager getAsgSdkManager(AsgCommandRequest asgCommandRequest, LogCallback logCallback) {
    Integer timeoutInMinutes = asgCommandRequest.getTimeoutIntervalInMin();
    AsgInfraConfig asgInfraConfig = asgCommandRequest.getAsgInfraConfig();

    String region = asgInfraConfig.getRegion();
    AwsInternalConfig awsInternalConfig = awsUtils.getAwsInternalConfig(asgInfraConfig.getAwsConnectorDTO(), region);

    Supplier<AmazonEC2Client> ec2ClientSupplier =
        () -> awsUtils.getAmazonEc2Client(Regions.fromName(region), awsInternalConfig);
    Supplier<AmazonAutoScalingClient> asgClientSupplier =
        () -> awsUtils.getAmazonAutoScalingClient(Regions.fromName(region), awsInternalConfig);

    return AsgSdkManager.builder()
        .ec2ClientSupplier(ec2ClientSupplier)
        .asgClientSupplier(asgClientSupplier)
        .logCallback(logCallback)
        .steadyStateTimeOutInMinutes(timeoutInMinutes)
        .timeLimiter(timeLimiter)
        .build();
  }
}
