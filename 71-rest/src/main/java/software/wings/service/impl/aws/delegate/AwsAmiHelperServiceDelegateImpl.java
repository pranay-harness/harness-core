package software.wings.service.impl.aws.delegate;

import static com.google.common.base.Joiner.on;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.LogColor.Yellow;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_DESIRED_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MAX_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MIN_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_NAME;
import static software.wings.utils.AsgConvention.getRevisionFromTag;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.BlockDeviceMapping;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.LaunchTemplate;
import com.amazonaws.services.autoscaling.model.LaunchTemplateSpecification;
import com.amazonaws.services.autoscaling.model.MixedInstancesPolicy;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionRequest;
import com.amazonaws.services.ec2.model.CreateLaunchTemplateVersionResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.LaunchTemplateVersion;
import com.amazonaws.services.ec2.model.RequestLaunchTemplateData;
import io.fabric8.utils.Lists;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.impl.aws.model.AwsAmiPreDeploymentData;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse.AwsAmiServiceSetupResponseBuilder;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesRequest;
import software.wings.service.impl.aws.model.AwsAmiSwitchRoutesResponse;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;
import software.wings.service.intfc.aws.delegate.AwsAmiHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;
import software.wings.utils.AsgConvention;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Singleton
@Slf4j
public class AwsAmiHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsAmiHelperServiceDelegate {
  private static final String AUTOSCALING_GROUP_RESOURCE_TYPE = "auto-scaling-group";
  @VisibleForTesting static final String NAME_TAG = "Name";
  private static final int MAX_OLD_ASG_VERSION_TO_KEEP = 3;
  @Inject private ExecutorService executorService;
  @Inject private AwsAsgHelperServiceDelegate awsAsgHelperServiceDelegate;
  @Inject private AwsElbHelperServiceDelegate awsElbHelperServiceDelegate;
  @Inject private AwsEc2HelperServiceDelegate awsEc2HelperServiceDelegate;

  @Override
  public AwsAmiSwitchRoutesResponse switchAmiRoutes(
      AwsAmiSwitchRoutesRequest request, ExecutionLogCallback logCallback) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      String region = request.getRegion();
      logCallback.saveExecutionLog("Starting to switch routes in AMI Deploy", INFO);
      List<String> primaryClassicLBs = request.getPrimaryClassicLBs();
      List<String> primaryTargetGroupARNs = request.getPrimaryTargetGroupARNs();
      List<String> stageClassicLBs = request.getStageClassicLBs();
      List<String> stageTargetGroupARNs = request.getStageTargetGroupARNs();
      int timeout = request.getRegistrationTimeout();
      String oldAsgName = request.getOldAsgName();
      String newAsgName = request.getNewAsgName();

      logCallback.saveExecutionLog("Starting Ami B/G swap");
      if (isNotEmpty(newAsgName)) {
        if (isNotEmpty(stageTargetGroupARNs)) {
          logCallback.saveExecutionLog(format("Sending request to detach target groups:[%s] from Asg:[%s]",
              on(",").join(stageTargetGroupARNs), newAsgName));
          awsAsgHelperServiceDelegate.deRegisterAsgWithTargetGroups(
              awsConfig, encryptionDetails, region, newAsgName, stageTargetGroupARNs, logCallback);
          stageTargetGroupARNs.forEach(arn -> {
            logCallback.saveExecutionLog(
                format("Waiting for Asg: [%s] to de register with target group: [%s]", newAsgName, arn));
            awsElbHelperServiceDelegate.waitForAsgInstancesToDeRegisterWithTargetGroup(
                awsConfig, encryptionDetails, region, arn, newAsgName, timeout, logCallback);
          });
        }

        if (isNotEmpty(stageClassicLBs)) {
          logCallback.saveExecutionLog(format(
              "Sending request to detach classic LBs:[%s] from Asg:[%s]", on(",").join(stageClassicLBs), newAsgName));
          awsAsgHelperServiceDelegate.deRegisterAsgWithClassicLBs(
              awsConfig, encryptionDetails, region, newAsgName, stageClassicLBs, logCallback);
          stageClassicLBs.forEach(classsicLb -> {
            logCallback.saveExecutionLog(
                format("Waiting for Asg: [%s] to de register with classic Lb: [%s]", newAsgName, classsicLb));
            awsElbHelperServiceDelegate.waitForAsgInstancesToDeRegisterWithClassicLB(
                awsConfig, encryptionDetails, region, classsicLb, newAsgName, timeout, logCallback);
          });
        }

        if (isNotEmpty(primaryTargetGroupARNs)) {
          logCallback.saveExecutionLog(format("Sending request to attach target groups:[%s] to Asg:[%s]",
              on(",").join(primaryTargetGroupARNs), newAsgName));
          awsAsgHelperServiceDelegate.registerAsgWithTargetGroups(
              awsConfig, encryptionDetails, region, newAsgName, primaryTargetGroupARNs, logCallback);
          primaryTargetGroupARNs.forEach(group -> {
            logCallback.saveExecutionLog(
                format("Waiting for Target Group: [%s] to have all instances of Asg: [%s]", group, newAsgName));
            awsElbHelperServiceDelegate.waitForAsgInstancesToRegisterWithTargetGroup(
                awsConfig, encryptionDetails, region, group, newAsgName, timeout, logCallback);
          });
        }

        if (isNotEmpty(primaryClassicLBs)) {
          logCallback.saveExecutionLog(format("Sending request to attach classic load balancers:[%s] to Asg:[%s]",
              on(",").join(primaryClassicLBs), newAsgName));
          awsAsgHelperServiceDelegate.registerAsgWithClassicLBs(
              awsConfig, encryptionDetails, region, newAsgName, primaryClassicLBs, logCallback);
          primaryClassicLBs.forEach(classicLB -> {
            logCallback.saveExecutionLog(
                format("Waiting for classic Lb: [%s] to have all the instances of Asg: [%s]", classicLB, newAsgName));
            awsElbHelperServiceDelegate.waitForAsgInstancesToRegisterWithClassicLB(
                awsConfig, encryptionDetails, region, classicLB, newAsgName, timeout, logCallback);
          });
        }

        awsAsgHelperServiceDelegate.attachScalingPoliciesToAsg(
            awsConfig, encryptionDetails, region, newAsgName, request.getBaseScalingPolicyJSONs(), logCallback);
      }

      if (isNotEmpty(oldAsgName)) {
        if (isNotEmpty(primaryTargetGroupARNs)) {
          logCallback.saveExecutionLog(format("Sending request to detach target groups:[%s] from Asg:[%s]",
              on(",").join(primaryTargetGroupARNs), oldAsgName));
          awsAsgHelperServiceDelegate.deRegisterAsgWithTargetGroups(
              awsConfig, encryptionDetails, region, oldAsgName, primaryTargetGroupARNs, logCallback);
          primaryTargetGroupARNs.forEach(arn -> {
            logCallback.saveExecutionLog(
                format("Waiting for Asg: [%s] to deregister with target group: [%s]", oldAsgName, arn));
            awsElbHelperServiceDelegate.waitForAsgInstancesToDeRegisterWithTargetGroup(
                awsConfig, encryptionDetails, region, arn, oldAsgName, timeout, logCallback);
          });
        }

        if (isNotEmpty(primaryClassicLBs)) {
          logCallback.saveExecutionLog(format(
              "Sending request to detach classic LBs:[%s] from Asg:[%s]", on(",").join(primaryClassicLBs), oldAsgName));
          awsAsgHelperServiceDelegate.deRegisterAsgWithClassicLBs(
              awsConfig, encryptionDetails, region, oldAsgName, primaryClassicLBs, logCallback);
          primaryClassicLBs.forEach(classicLb -> {
            logCallback.saveExecutionLog(
                format("Waiting for Asg: [%s] to de register with classicLb: [%s]", oldAsgName, classicLb));
            awsElbHelperServiceDelegate.waitForAsgInstancesToDeRegisterWithClassicLB(
                awsConfig, encryptionDetails, region, classicLb, oldAsgName, timeout, logCallback);
          });
        }

        if (request.isDownscaleOldAsg()) {
          logCallback.saveExecutionLog(format("Downscaling autoScaling Group [%s]", oldAsgName));
          awsAsgHelperServiceDelegate.clearAllScalingPoliciesForAsg(
              awsConfig, encryptionDetails, region, oldAsgName, logCallback);
          awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(
              awsConfig, encryptionDetails, region, oldAsgName, 0, logCallback);
          awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
              awsConfig, encryptionDetails, region, oldAsgName, 0, logCallback, timeout);
        }
      }

      logCallback.saveExecutionLog("Completed switch routes");
      return AwsAmiSwitchRoutesResponse.builder().executionStatus(SUCCESS).build();
    } catch (Exception ex) {
      String errorMessage = ExceptionUtils.getMessage(ex);
      logCallback.saveExecutionLog(format("Exception: [%s].", errorMessage), ERROR);
      logger.error(errorMessage, ex);
      return AwsAmiSwitchRoutesResponse.builder().errorMessage(errorMessage).executionStatus(FAILED).build();
    }
  }

  @Override
  public AwsAmiSwitchRoutesResponse rollbackSwitchAmiRoutes(
      AwsAmiSwitchRoutesRequest request, ExecutionLogCallback logCallback) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      String region = request.getRegion();
      logCallback.saveExecutionLog("Starting to switch routes in AMI Deploy", INFO);
      List<String> primaryClassicLBs = request.getPrimaryClassicLBs();
      List<String> primaryTargetGroupARNs = request.getPrimaryTargetGroupARNs();
      int timeout = request.getRegistrationTimeout();
      String oldAsgName = request.getOldAsgName();
      String newAsgName = request.getNewAsgName();
      AwsAmiPreDeploymentData preDeploymentData = request.getPreDeploymentData();

      logCallback.saveExecutionLog("Rolling back Ami B/G swap");

      if (isNotEmpty(oldAsgName)) {
        logCallback.saveExecutionLog(format("Upgrading old Asg: [%s] back to initial state", oldAsgName));
        int desiredCount = preDeploymentData.getPreDeploymentDesiredCapacity();
        int minCount = preDeploymentData.getPreDeploymentMinCapacity();
        List<String> oldScalingPolicyJSONs = preDeploymentData.getPreDeploymenyScalingPolicyJSON();

        awsAsgHelperServiceDelegate.clearAllScalingPoliciesForAsg(
            awsConfig, encryptionDetails, region, oldAsgName, logCallback);
        awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(
            awsConfig, encryptionDetails, region, oldAsgName, desiredCount, logCallback);
        awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
            awsConfig, encryptionDetails, region, oldAsgName, desiredCount, logCallback, timeout);
        awsAsgHelperServiceDelegate.setMinInstancesForAsg(
            awsConfig, encryptionDetails, region, oldAsgName, minCount, logCallback);

        if (isNotEmpty(primaryTargetGroupARNs)) {
          logCallback.saveExecutionLog(format("Sending request to attach target groups:[%s] to Asg:[%s]",
              on(",").join(primaryTargetGroupARNs), oldAsgName));
          awsAsgHelperServiceDelegate.registerAsgWithTargetGroups(
              awsConfig, encryptionDetails, region, oldAsgName, primaryTargetGroupARNs, logCallback);
          primaryTargetGroupARNs.forEach(group -> {
            logCallback.saveExecutionLog(
                format("Waiting for Target Group: [%s] to have all instances of Asg: [%s]", group, oldAsgName));
            awsElbHelperServiceDelegate.waitForAsgInstancesToRegisterWithTargetGroup(
                awsConfig, encryptionDetails, region, group, oldAsgName, timeout, logCallback);
          });
        }

        if (isNotEmpty(primaryClassicLBs)) {
          logCallback.saveExecutionLog(format("Sending request to attach classic load balancers:[%s] to Asg:[%s]",
              on(",").join(primaryClassicLBs), oldAsgName));
          awsAsgHelperServiceDelegate.registerAsgWithClassicLBs(
              awsConfig, encryptionDetails, region, oldAsgName, primaryClassicLBs, logCallback);
          primaryClassicLBs.forEach(classicLB -> {
            logCallback.saveExecutionLog(
                format("Waiting for classic Lb: [%s] to have all the instances of Asg: [%s]", classicLB, oldAsgName));
            awsElbHelperServiceDelegate.waitForAsgInstancesToRegisterWithClassicLB(
                awsConfig, encryptionDetails, region, classicLB, oldAsgName, timeout, logCallback);
          });
        }
        awsAsgHelperServiceDelegate.attachScalingPoliciesToAsg(
            awsConfig, encryptionDetails, region, oldAsgName, oldScalingPolicyJSONs, logCallback);
      }

      if (isNotEmpty(newAsgName)) {
        if (isNotEmpty(primaryTargetGroupARNs)) {
          logCallback.saveExecutionLog(format("Sending request to detach target groups:[%s] from Asg:[%s]",
              on(",").join(primaryTargetGroupARNs), newAsgName));
          awsAsgHelperServiceDelegate.deRegisterAsgWithTargetGroups(
              awsConfig, encryptionDetails, region, newAsgName, primaryTargetGroupARNs, logCallback);
          primaryTargetGroupARNs.forEach(arn -> {
            logCallback.saveExecutionLog(
                format("Waiting for Asg: [%s] to de register with target group: [%s]", newAsgName, arn));
            awsElbHelperServiceDelegate.waitForAsgInstancesToDeRegisterWithTargetGroup(
                awsConfig, encryptionDetails, region, arn, newAsgName, timeout, logCallback);
          });
        }

        if (isNotEmpty(primaryClassicLBs)) {
          logCallback.saveExecutionLog(format(
              "Sending request to detach classic LBs:[%s] from Asg:[%s]", on(",").join(primaryClassicLBs), newAsgName));
          awsAsgHelperServiceDelegate.deRegisterAsgWithClassicLBs(
              awsConfig, encryptionDetails, region, newAsgName, primaryClassicLBs, logCallback);
          primaryClassicLBs.forEach(classicLb -> {
            logCallback.saveExecutionLog(
                format("Waiting for Asg: [%s] to de register with classic Lb: [%s]", newAsgName, classicLb));
            awsElbHelperServiceDelegate.waitForAsgInstancesToDeRegisterWithClassicLB(
                awsConfig, encryptionDetails, region, classicLb, newAsgName, timeout, logCallback);
          });
        }

        logCallback.saveExecutionLog(format("Downscaling autoScaling Group [%s]", newAsgName));
        awsAsgHelperServiceDelegate.clearAllScalingPoliciesForAsg(
            awsConfig, encryptionDetails, region, newAsgName, logCallback);
        awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(
            awsConfig, encryptionDetails, region, newAsgName, 0, logCallback);
        awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
            awsConfig, encryptionDetails, region, newAsgName, 0, logCallback, timeout);
        logCallback.saveExecutionLog(format("Asg: [%s] being deleted after shutting down to 0 instances", newAsgName));
        awsAsgHelperServiceDelegate.deleteAutoScalingGroups(awsConfig, encryptionDetails, region,
            singletonList(
                awsAsgHelperServiceDelegate.getAutoScalingGroup(awsConfig, encryptionDetails, region, newAsgName)),
            logCallback);
      }

      logCallback.saveExecutionLog("Completed rollback switch routes");
      return AwsAmiSwitchRoutesResponse.builder().executionStatus(SUCCESS).build();
    } catch (Exception ex) {
      String errorMessage = ExceptionUtils.getMessage(ex);
      logCallback.saveExecutionLog(format("Exception: [%s].", errorMessage), ERROR);
      logger.error(errorMessage, ex);
      return AwsAmiSwitchRoutesResponse.builder().errorMessage(errorMessage).executionStatus(FAILED).build();
    }
  }

  @Override
  public AwsAmiServiceDeployResponse deployAmiService(
      AwsAmiServiceDeployRequest request, ExecutionLogCallback logCallback) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      logCallback.saveExecutionLog("Starting AWS AMI Deploy", INFO);

      logCallback.saveExecutionLog("Getting existing instance Ids");

      Set<String> existingInstanceIds = new HashSet<>();
      if (isNotEmpty(request.getExistingInstanceIds())) {
        existingInstanceIds.addAll(request.getExistingInstanceIds());
      }

      logCallback.saveExecutionLog("Resizing Asgs", INFO);
      resizeAsgs(request.getRegion(), awsConfig, encryptionDetails, request.getNewAutoScalingGroupName(),
          request.getNewAsgFinalDesiredCount(), request.getAsgDesiredCounts(), logCallback, request.isResizeNewFirst(),
          request.getAutoScalingSteadyStateTimeout(), request.getMaxInstances(), request.getMinInstances(),
          request.getPreDeploymentData(), request.getInfraMappingTargetGroupArns(), request.getInfraMappingClassisLbs(),
          request.isRollback(), request.getBaseScalingPolicyJSONs(), request.getDesiredInstances());

      List<Instance> allInstancesOfNewAsg = awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
          awsConfig, encryptionDetails, request.getRegion(), request.getNewAutoScalingGroupName());

      List<Instance> instancesAdded = allInstancesOfNewAsg.stream()
                                          .filter(instance -> !existingInstanceIds.contains(instance.getInstanceId()))
                                          .collect(toList());

      List<Instance> existingInstancesForOldASGGroup =
          fetchExistingInstancesForOlderASG(awsConfig, encryptionDetails, request, logCallback);

      return AwsAmiServiceDeployResponse.builder()
          .instancesAdded(instancesAdded)
          .instancesExisting(existingInstancesForOldASGGroup)
          .executionStatus(SUCCESS)
          .build();
    } catch (Exception ex) {
      String errorMessage = ExceptionUtils.getMessage(ex);
      logCallback.saveExecutionLog(format("Exception: [%s].", errorMessage), ERROR);
      logger.error(errorMessage, ex);
      return AwsAmiServiceDeployResponse.builder().errorMessage(errorMessage).executionStatus(FAILED).build();
    }
  }

  @VisibleForTesting
  List<Instance> fetchExistingInstancesForOlderASG(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      AwsAmiServiceDeployRequest request, ExecutionLogCallback logCallback) {
    List<Instance> existingInstancesForOldASG = new ArrayList<>();

    if (request.isRollback()) {
      return existingInstancesForOldASG;
    }

    if (isEmpty(request.getAsgDesiredCounts())) {
      if (isNotEmpty(request.getOldAutoScalingGroupName())) {
        existingInstancesForOldASG.addAll(awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
            awsConfig, encryptionDetails, request.getRegion(), request.getOldAutoScalingGroupName()));
      }

      return existingInstancesForOldASG;
    }

    for (AwsAmiResizeData awsAmiResizeData : request.getAsgDesiredCounts()) {
      if (isNotEmpty(awsAmiResizeData.getAsgName()) && awsAmiResizeData.getDesiredCount() > 0) {
        try {
          existingInstancesForOldASG.addAll(awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
              awsConfig, encryptionDetails, request.getRegion(), awsAmiResizeData.getAsgName()));
        } catch (Exception e) {
          logCallback.saveExecutionLog("Failed to fetch instances for ASG: " + awsAmiResizeData.getAsgName());
          logCallback.saveExecutionLog("Verification cant use these instances");
        }
      }
    }

    return existingInstancesForOldASG;
  }

  private void resizeNewAsgAndWait(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String newAutoScalingGroupName, Integer newAsgFinalDesiredCount, ExecutionLogCallback executionLogCallback,
      Integer autoScalingSteadyStateTimeout, int maxInstances, int minInstances, List<String> targetGroupsArns,
      List<String> classicLBs, boolean rollback, List<String> baseScalingPolicyJSONs, int desiredInstances) {
    if (isNotBlank(newAutoScalingGroupName)) {
      awsAsgHelperServiceDelegate.clearAllScalingPoliciesForAsg(
          awsConfig, encryptionDetails, region, newAutoScalingGroupName, executionLogCallback);
      executionLogCallback.saveExecutionLog(
          format("Resizing AutoScaling Group: [%s] to [%d]", newAutoScalingGroupName, newAsgFinalDesiredCount));
      awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(
          awsConfig, encryptionDetails, region, newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback);
      awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails,
          region, newAutoScalingGroupName, newAsgFinalDesiredCount, executionLogCallback,
          autoScalingSteadyStateTimeout);
      if (newAsgFinalDesiredCount >= minInstances) {
        AutoScalingGroup newAutoScalingGroup = awsAsgHelperServiceDelegate.getAutoScalingGroup(
            awsConfig, encryptionDetails, region, newAutoScalingGroupName);
        if (newAutoScalingGroup != null && minInstances != newAutoScalingGroup.getMinSize()) {
          awsAsgHelperServiceDelegate.setMinInstancesForAsg(
              awsConfig, encryptionDetails, region, newAutoScalingGroupName, minInstances, executionLogCallback);
        }
      }
      if (!rollback) {
        if (isNotEmpty(targetGroupsArns)) {
          targetGroupsArns.forEach(arn -> {
            executionLogCallback.saveExecutionLog(format(
                "Waiting for Target Group: [%s] to have all instances of Asg: [%s]", arn, newAutoScalingGroupName));
            awsElbHelperServiceDelegate.waitForAsgInstancesToRegisterWithTargetGroup(awsConfig, encryptionDetails,
                region, arn, newAutoScalingGroupName, autoScalingSteadyStateTimeout, executionLogCallback);
          });
        }
        if (isNotEmpty(classicLBs)) {
          classicLBs.forEach(classicLB -> {
            executionLogCallback.saveExecutionLog(
                format("Waiting for classic Lb: [%s] to have all the instances of Asg: [%s]", classicLB,
                    newAutoScalingGroupName));
            awsElbHelperServiceDelegate.waitForAsgInstancesToRegisterWithClassicLB(awsConfig, encryptionDetails, region,
                classicLB, newAutoScalingGroupName, autoScalingSteadyStateTimeout, executionLogCallback);
          });
        }
        if (newAsgFinalDesiredCount == desiredInstances) {
          awsAsgHelperServiceDelegate.attachScalingPoliciesToAsg(awsConfig, encryptionDetails, region,
              newAutoScalingGroupName, baseScalingPolicyJSONs, executionLogCallback);
        }
      } else {
        if (newAsgFinalDesiredCount <= 0) {
          // Delete new Asg and LC
          executionLogCallback.saveExecutionLog(
              format("Asg: [%s] being deleted after shutting down to 0 instances", newAutoScalingGroupName));
          awsAsgHelperServiceDelegate.deleteAutoScalingGroups(awsConfig, encryptionDetails, region,
              singletonList(awsAsgHelperServiceDelegate.getAutoScalingGroup(
                  awsConfig, encryptionDetails, region, newAutoScalingGroupName)),
              executionLogCallback);
        }
      }
    }
  }

  private void resizeOldAsgsAndWait(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      List<AwsAmiResizeData> oldAsgsDesiredCounts, ExecutionLogCallback executionLogCallback,
      Integer autoScalingSteadyStateTimeout, AwsAmiPreDeploymentData preDeploymentData, List<String> targetGroupsArns,
      List<String> classicLBs, boolean rollback, int newAsgFinalDesiredCount, int desiredInstances) {
    if (isNotEmpty(oldAsgsDesiredCounts)) {
      oldAsgsDesiredCounts.forEach(count -> {
        awsAsgHelperServiceDelegate.clearAllScalingPoliciesForAsg(
            awsConfig, encryptionDetails, region, count.getAsgName(), executionLogCallback);
        executionLogCallback.saveExecutionLog(
            format("Resizing AutoScaling Group: [%s] to [%d]", count.getAsgName(), count.getDesiredCount()));
        awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(
            awsConfig, encryptionDetails, region, count.getAsgName(), count.getDesiredCount(), executionLogCallback);
        awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig,
            encryptionDetails, region, count.getAsgName(), count.getDesiredCount(), executionLogCallback,
            autoScalingSteadyStateTimeout);

        if (rollback) {
          if (preDeploymentData.hasAsgReachedPreDeploymentCount(count.getDesiredCount())) {
            awsAsgHelperServiceDelegate.setMinInstancesForAsg(awsConfig, encryptionDetails, region, count.getAsgName(),
                preDeploymentData.getPreDeploymentMinCapacity(), executionLogCallback);
          }

          if (isNotEmpty(targetGroupsArns)) {
            targetGroupsArns.forEach(arn -> {
              executionLogCallback.saveExecutionLog(
                  format("Waiting for Target Group: [%s] to have all instances of Asg: [%s]", arn, count.getAsgName()));
              awsElbHelperServiceDelegate.waitForAsgInstancesToRegisterWithTargetGroup(awsConfig, encryptionDetails,
                  region, arn, count.getAsgName(), autoScalingSteadyStateTimeout, executionLogCallback);
            });
          }
          if (isNotEmpty(classicLBs)) {
            classicLBs.forEach(classicLB -> {
              executionLogCallback.saveExecutionLog(
                  format("Waiting for classic Lb: [%s] to have all the instances of Asg: [%s]", classicLB,
                      count.getAsgName()));
              awsElbHelperServiceDelegate.waitForAsgInstancesToRegisterWithClassicLB(awsConfig, encryptionDetails,
                  region, classicLB, count.getAsgName(), autoScalingSteadyStateTimeout, executionLogCallback);
            });
          }
          if (preDeploymentData.hasAsgReachedPreDeploymentCount(count.getDesiredCount())) {
            awsAsgHelperServiceDelegate.attachScalingPoliciesToAsg(awsConfig, encryptionDetails, region,
                count.getAsgName(), preDeploymentData.getPreDeploymenyScalingPolicyJSON(), executionLogCallback);
          }
        } else {
          /**
           * In the case of old Asgs in the deploy forward case if the final desired count > 0,
           * we still need to attach the scaling policies when we are on the final deploy step.
           */
          if (newAsgFinalDesiredCount == desiredInstances && count.getDesiredCount() > 0) {
            awsAsgHelperServiceDelegate.attachScalingPoliciesToAsg(awsConfig, encryptionDetails, region,
                count.getAsgName(), preDeploymentData.getPreDeploymenyScalingPolicyJSON(), executionLogCallback);
          }
        }
      });
    }
  }

  @VisibleForTesting
  void resizeAsgs(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String newAutoScalingGroupName, Integer newAsgFinalDesiredCount, List<AwsAmiResizeData> oldAsgsDesiredCounts,
      ExecutionLogCallback executionLogCallback, boolean resizeNewFirst, Integer autoScalingSteadyStateTimeout,
      int maxInstances, int minInstances, AwsAmiPreDeploymentData preDeploymentData, List<String> targetGroupsArns,
      List<String> classicLBs, boolean rollback, List<String> baseScalingPolicyJSONs, int desiredInstances) {
    if (isBlank(newAutoScalingGroupName) && isEmpty(oldAsgsDesiredCounts)) {
      throw new InvalidRequestException("At least one AutoScaling Group must be present");
    }
    if (resizeNewFirst) {
      resizeNewAsgAndWait(region, awsConfig, encryptionDetails, newAutoScalingGroupName, newAsgFinalDesiredCount,
          executionLogCallback, autoScalingSteadyStateTimeout, maxInstances, minInstances, targetGroupsArns, classicLBs,
          rollback, baseScalingPolicyJSONs, desiredInstances);
      resizeOldAsgsAndWait(region, awsConfig, encryptionDetails, oldAsgsDesiredCounts, executionLogCallback,
          autoScalingSteadyStateTimeout, preDeploymentData, targetGroupsArns, classicLBs, rollback,
          newAsgFinalDesiredCount, desiredInstances);
    } else {
      resizeOldAsgsAndWait(region, awsConfig, encryptionDetails, oldAsgsDesiredCounts, executionLogCallback,
          autoScalingSteadyStateTimeout, preDeploymentData, targetGroupsArns, classicLBs, rollback,
          newAsgFinalDesiredCount, desiredInstances);
      resizeNewAsgAndWait(region, awsConfig, encryptionDetails, newAutoScalingGroupName, newAsgFinalDesiredCount,
          executionLogCallback, autoScalingSteadyStateTimeout, maxInstances, minInstances, targetGroupsArns, classicLBs,
          rollback, baseScalingPolicyJSONs, desiredInstances);
    }
  }

  @VisibleForTesting
  LaunchTemplateSpecification extractLaunchTemplateSpecFrom(AutoScalingGroup autoScalingGroup) {
    LaunchTemplateSpecification launchTemplateSpecification = null;
    if (autoScalingGroup != null) {
      launchTemplateSpecification = autoScalingGroup.getLaunchTemplate();
      if (launchTemplateSpecification == null) {
        launchTemplateSpecification = Optional.ofNullable(autoScalingGroup.getMixedInstancesPolicy())
                                          .map(MixedInstancesPolicy::getLaunchTemplate)
                                          .map(LaunchTemplate::getLaunchTemplateSpecification)
                                          .orElse(null);
      }
    }
    return launchTemplateSpecification;
  }

  @Override
  public AwsAmiServiceSetupResponse setUpAmiService(
      AwsAmiServiceSetupRequest request, ExecutionLogCallback logCallback) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      logCallback.saveExecutionLog("Starting AWS AMI Setup", INFO);

      logCallback.saveExecutionLog("Getting base auto scaling group");
      AutoScalingGroup baseAutoScalingGroup = ensureAndGetBaseAutoScalingGroup(
          awsConfig, encryptionDetails, request.getRegion(), request.getInfraMappingAsgName(), logCallback);
      boolean isBaseAsgLtBased = false;
      LaunchConfiguration baseLaunchConfiguration = null;
      LaunchTemplateSpecification baseLaunchTemplateSpecification = extractLaunchTemplateSpecFrom(baseAutoScalingGroup);
      LaunchTemplateVersion baseLaunchTemplateVersion = null;
      LaunchTemplateVersion newLaunchTemplateVersion = null;
      if (baseLaunchTemplateSpecification != null) {
        isBaseAsgLtBased = true;
        logCallback.saveExecutionLog("Getting base launch template");
        baseLaunchTemplateVersion = ensureAndGetLaunchTemplateVersion(baseLaunchTemplateSpecification,
            baseAutoScalingGroup, awsConfig, encryptionDetails, request.getRegion(), logCallback);
        logCallback.saveExecutionLog(format("Found Base Launch Template name=[%s], version=[%s]",
            baseLaunchTemplateVersion.getLaunchTemplateName(), baseLaunchTemplateVersion.getVersionNumber()));
      } else {
        logCallback.saveExecutionLog("Getting base launch configuration");
        baseLaunchConfiguration = ensureAndGetBaseLaunchConfiguration(awsConfig, encryptionDetails, request.getRegion(),
            request.getInfraMappingAsgName(), baseAutoScalingGroup, logCallback);
      }

      logCallback.saveExecutionLog("Getting all Harness managed autoscaling groups");
      List<AutoScalingGroup> harnessManagedAutoScalingGroups = listAllHarnessManagedAsgs(
          request.getAwsConfig(), request.getEncryptionDetails(), request.getRegion(), request.getInfraMappingId());

      logCallback.saveExecutionLog("Getting last deployed autoscaling group with non zero capacity");

      Integer harnessRevision = getNewHarnessVersion(harnessManagedAutoScalingGroups);
      String region = request.getRegion();
      String newAutoScalingGroupName = AsgConvention.getAsgName(request.getNewAsgNamePrefix(), harnessRevision);
      int minInstances;
      Integer maxInstances;
      int desiredInstances;

      List<AutoScalingGroup> autoScalingGroupsWithNonZeroCount =
          listAllExistingAsgsWithNonZeroCount(harnessManagedAutoScalingGroups);

      AutoScalingGroup mostRecentActiveAsg =
          getMostRecentActiveAsg(harnessManagedAutoScalingGroups, autoScalingGroupsWithNonZeroCount);

      downsizeOrDeleteOlderAutoScalaingGroups(
          awsConfig, encryptionDetails, request, harnessManagedAutoScalingGroups, mostRecentActiveAsg, logCallback);

      String lastDeployedAsgName =
          mostRecentActiveAsg == null ? StringUtils.EMPTY : mostRecentActiveAsg.getAutoScalingGroupName();

      if (request.isUseCurrentRunningCount()) {
        AwsAsgGetRunningCountData currentlyRunningInstanceCount = getAsgRunningCountData(mostRecentActiveAsg);
        logCallback.saveExecutionLog(
            format("Using currently running min: [%d], max: [%d], desired: [%d] from Asg: [%s]",
                currentlyRunningInstanceCount.getAsgMin(), currentlyRunningInstanceCount.getAsgMax(),
                currentlyRunningInstanceCount.getAsgDesired(), currentlyRunningInstanceCount.getAsgName()));
        minInstances = currentlyRunningInstanceCount.getAsgMin();
        maxInstances = currentlyRunningInstanceCount.getAsgMax();
        desiredInstances = currentlyRunningInstanceCount.getAsgDesired();
      } else {
        logCallback.saveExecutionLog(format("Using workflow input min: [%d], max: [%d] and desired: [%d]",
            request.getMinInstances(), request.getMaxInstances(), request.getDesiredInstances()));
        minInstances = request.getMinInstances();
        maxInstances = request.getMaxInstances();
        desiredInstances = request.getDesiredInstances();
      }

      if (isBaseAsgLtBased) {
        //  creating LT
        newLaunchTemplateVersion = createAndGetNewLaunchTemplateVersion(
            baseLaunchTemplateVersion, request, logCallback, awsConfig, encryptionDetails, region);
      } else {
        createNewLaunchConfig(request, logCallback, awsConfig, encryptionDetails, baseLaunchConfiguration, region,
            newAutoScalingGroupName);
      }

      logCallback.saveExecutionLog(format("Creating new AutoScalingGroup [%s]", newAutoScalingGroupName));
      awsAsgHelperServiceDelegate.createAutoScalingGroup(awsConfig, encryptionDetails, region,
          createNewAutoScalingGroupRequest(request.getInfraMappingId(), request.getInfraMappingClassisLbs(),
              request.getInfraMappingTargetGroupArns(), newAutoScalingGroupName, baseAutoScalingGroup, harnessRevision,
              maxInstances, newLaunchTemplateVersion),
          logCallback);
      AwsAmiServiceSetupResponseBuilder builder =
          AwsAmiServiceSetupResponse.builder()
              .executionStatus(SUCCESS)
              .lastDeployedAsgName(lastDeployedAsgName)
              .oldAsgNames(isNotBlank(lastDeployedAsgName) ? Arrays.asList(lastDeployedAsgName) : emptyList())
              .newAsgName(newAutoScalingGroupName)
              .harnessRevision(harnessRevision)
              .minInstances(minInstances)
              .maxInstances(maxInstances)
              .desiredInstances(desiredInstances)
              .blueGreen(request.isBlueGreen())
              .baseLaunchTemplateName(
                  baseLaunchTemplateVersion != null ? baseLaunchTemplateVersion.getLaunchTemplateName() : null)
              .baseLaunchTemplateVersion(baseLaunchTemplateVersion != null
                      ? String.valueOf(baseLaunchTemplateVersion.getVersionNumber())
                      : null)
              .newLaunchTemplateName(
                  newLaunchTemplateVersion != null ? newLaunchTemplateVersion.getLaunchTemplateName() : null)
              .newLaunchTemplateVersion(
                  newLaunchTemplateVersion != null ? String.valueOf(newLaunchTemplateVersion.getVersionNumber()) : null)
              .baseAsgScalingPolicyJSONs(awsAsgHelperServiceDelegate.getScalingPolicyJSONs(
                  awsConfig, encryptionDetails, region, baseAutoScalingGroup.getAutoScalingGroupName(), logCallback));

      populatePreDeploymentData(awsConfig, encryptionDetails, region, mostRecentActiveAsg, builder, logCallback);
      logCallback.saveExecutionLog(
          format("Completed AWS AMI Setup with new autoScalingGroupName [%s]", newAutoScalingGroupName), INFO,
          CommandExecutionStatus.SUCCESS);
      return builder.build();
    } catch (Exception exception) {
      logCallback.saveExecutionLog(format("Exception: [%s].", exception.getMessage()), ERROR);
      logger.error(exception.getMessage(), exception);
      return AwsAmiServiceSetupResponse.builder()
          .errorMessage(ExceptionUtils.getMessage(exception))
          .executionStatus(FAILED)
          .build();
    }
  }

  @VisibleForTesting
  AutoScalingGroup getMostRecentActiveAsg(List<AutoScalingGroup> harnessManagedAutoScalingGroups,
      List<AutoScalingGroup> autoScalingGroupsWithNonZeroCount) {
    AutoScalingGroup mostRecentActiveAsg = null;

    if (isNotEmpty(autoScalingGroupsWithNonZeroCount)) {
      mostRecentActiveAsg = autoScalingGroupsWithNonZeroCount.get(0);
    } else if (isNotEmpty(harnessManagedAutoScalingGroups)) {
      mostRecentActiveAsg = harnessManagedAutoScalingGroups.get(0);
    }

    return mostRecentActiveAsg;
  }

  private List<AutoScalingGroup> listAllExistingAsgsWithNonZeroCount(
      List<AutoScalingGroup> harnessManagedAutoScalingGroups) {
    if (isEmpty(harnessManagedAutoScalingGroups)) {
      return harnessManagedAutoScalingGroups;
    }

    return harnessManagedAutoScalingGroups.stream()
        .filter(autoScalingGroup -> autoScalingGroup.getDesiredCapacity() > 0)
        .sorted(Comparator.comparing(AutoScalingGroup::getCreatedTime).reversed())
        .collect(toList());
  }

  @VisibleForTesting
  void downsizeOrDeleteOlderAutoScalaingGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      AwsAmiServiceSetupRequest request, List<AutoScalingGroup> autoScalingGroups,
      AutoScalingGroup mostRecentAsgWithNonZeroInstanceCount, ExecutionLogCallback executionLogCallback) {
    if (isEmpty(autoScalingGroups) || mostRecentAsgWithNonZeroInstanceCount == null) {
      return;
    }

    executionLogCallback.saveExecutionLog("# Downsizing older ASGs to 0");

    int versionsRetained = 1;
    try {
      for (AutoScalingGroup autoScalingGroup : autoScalingGroups) {
        if (mostRecentAsgWithNonZeroInstanceCount.getAutoScalingGroupName().equals(
                autoScalingGroup.getAutoScalingGroupName())) {
          executionLogCallback.saveExecutionLog(color(
              "# Not changing Most Recent Active ASG: " + autoScalingGroup.getAutoScalingGroupName(), Yellow, Bold));
        } else {
          if (versionsRetained < MAX_OLD_ASG_VERSION_TO_KEEP) {
            if (autoScalingGroup.getDesiredCapacity() > 0) {
              downsizeAsgToZero(awsConfig, encryptionDetails, request, executionLogCallback, autoScalingGroup);
            }
            versionsRetained++;
          } else {
            logger.info("ASG Cleanup. Deleting ASG: " + autoScalingGroup.getAutoScalingGroupName());
            executionLogCallback.saveExecutionLog(
                color("# Deleting Existing ASG: " + autoScalingGroup.getAutoScalingGroupName(), Yellow, Bold));
            downsizeAsgToZero(awsConfig, encryptionDetails, request, executionLogCallback, autoScalingGroup);
            awsAsgHelperServiceDelegate.deleteAutoScalingGroups(awsConfig, encryptionDetails, request.getRegion(),
                singletonList(autoScalingGroup), executionLogCallback);
          }
        }
      }
    } catch (Exception e) {
      String msg = "Failed while downsizing/deleting older ASGs";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(msg, ERROR);
    }
  }

  private void downsizeAsgToZero(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      AwsAmiServiceSetupRequest request, ExecutionLogCallback executionLogCallback, AutoScalingGroup autoScalingGroup) {
    executionLogCallback.saveExecutionLog(
        color(format("# Resizing AutoScaling Group: [%s] to [%d]", autoScalingGroup.getAutoScalingGroupName(), 0),
            Yellow, Bold));
    awsAsgHelperServiceDelegate.setAutoScalingGroupLimits(awsConfig, encryptionDetails, request.getRegion(),
        autoScalingGroup.getAutoScalingGroupName(), 0, executionLogCallback);
    awsAsgHelperServiceDelegate.setAutoScalingGroupCapacityAndWaitForInstancesReadyState(awsConfig, encryptionDetails,
        request.getRegion(), autoScalingGroup.getAutoScalingGroupName(), 0, executionLogCallback,
        request.getAutoScalingSteadyStateTimeout());
  }

  @VisibleForTesting
  AwsAsgGetRunningCountData getAsgRunningCountData(AutoScalingGroup autoScalingGroup) {
    String asgName = DEFAULT_AMI_ASG_NAME;
    int asgMin = DEFAULT_AMI_ASG_MIN_INSTANCES;
    int asgMax = DEFAULT_AMI_ASG_MAX_INSTANCES;
    int asgDesired = DEFAULT_AMI_ASG_DESIRED_INSTANCES;
    if (autoScalingGroup != null) {
      asgName = autoScalingGroup.getAutoScalingGroupName();
      asgMin = autoScalingGroup.getMinSize();
      asgMax = autoScalingGroup.getMaxSize();
      asgDesired = autoScalingGroup.getDesiredCapacity();
    }

    return AwsAsgGetRunningCountData.builder()
        .asgMin(asgMin)
        .asgMax(asgMax)
        .asgDesired(asgDesired)
        .asgName(asgName)
        .build();
  }

  private void createNewLaunchConfig(AwsAmiServiceSetupRequest request, ExecutionLogCallback logCallback,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, LaunchConfiguration baseLaunchConfiguration,
      String region, String newAutoScalingGroupName) {
    LaunchConfiguration oldLaunchConfiguration = awsAsgHelperServiceDelegate.getLaunchConfiguration(
        awsConfig, encryptionDetails, region, newAutoScalingGroupName);
    if (oldLaunchConfiguration != null) {
      logCallback.saveExecutionLog(
          format("Deleting old launch configuration [%s]", oldLaunchConfiguration.getLaunchConfigurationName()));
      awsAsgHelperServiceDelegate.deleteLaunchConfig(awsConfig, encryptionDetails, region, newAutoScalingGroupName);
    }
    logCallback.saveExecutionLog(format("Creating new launch configuration [%s]", newAutoScalingGroupName));
    awsAsgHelperServiceDelegate.createLaunchConfiguration(awsConfig, encryptionDetails, region,
        createNewLaunchConfigurationRequest(awsConfig, encryptionDetails, region, request.getArtifactRevision(),
            baseLaunchConfiguration, newAutoScalingGroupName, request.getUserData()));
  }

  @VisibleForTesting
  LaunchTemplateVersion createAndGetNewLaunchTemplateVersion(LaunchTemplateVersion baseLaunchTemplateVersion,
      AwsAmiServiceSetupRequest request, ExecutionLogCallback logCallback, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region) {
    logCallback.saveExecutionLog("Creating new launch template version");

    CreateLaunchTemplateVersionRequest createLaunchTemplateVersionRequest = createNewLaunchTemplateVersionRequest(
        request.getArtifactRevision(), baseLaunchTemplateVersion, request.getUserData());
    CreateLaunchTemplateVersionResult newLaunchTemplateVersionResult =
        awsEc2HelperServiceDelegate.createLaunchTemplateVersion(
            createLaunchTemplateVersionRequest, awsConfig, encryptionDetails, region);
    if (newLaunchTemplateVersionResult == null || newLaunchTemplateVersionResult.getLaunchTemplateVersion() == null) {
      String errorMsg = format("Unable to create new version of Launch Template from base [%s] , version =[%s]",
          baseLaunchTemplateVersion.getLaunchTemplateName(), baseLaunchTemplateVersion.getVersionNumber());
      logCallback.saveExecutionLog(errorMsg, ERROR);
      throw new InvalidRequestException(errorMsg);
    }

    logCallback.saveExecutionLog(format("Created new launch template version =[%s]",
        newLaunchTemplateVersionResult.getLaunchTemplateVersion().getVersionNumber()));
    return newLaunchTemplateVersionResult.getLaunchTemplateVersion();
  }

  @VisibleForTesting
  void populatePreDeploymentData(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, String region,
      AutoScalingGroup mostRecentActiveAsg, AwsAmiServiceSetupResponseBuilder builder,
      ExecutionLogCallback logCallback) {
    List<String> autoScalaingPoliciesJson;
    boolean mostRecentAsgAvailable = mostRecentActiveAsg != null;

    if (mostRecentAsgAvailable) {
      autoScalaingPoliciesJson = awsAsgHelperServiceDelegate.getScalingPolicyJSONs(
          awsConfig, encryptedDataDetails, region, mostRecentActiveAsg.getAutoScalingGroupName(), logCallback);
    } else {
      autoScalaingPoliciesJson = emptyList();
    }

    builder.preDeploymentData(
        AwsAmiPreDeploymentData.builder()
            .minCapacity(mostRecentAsgAvailable ? mostRecentActiveAsg.getMinSize() : 0)
            .desiredCapacity(mostRecentAsgAvailable ? mostRecentActiveAsg.getDesiredCapacity() : 0)
            .scalingPolicyJSON(mostRecentAsgAvailable ? autoScalaingPoliciesJson : emptyList())
            .oldAsgName(mostRecentAsgAvailable ? mostRecentActiveAsg.getAutoScalingGroupName() : StringUtils.EMPTY)
            .build());
  }

  @VisibleForTesting
  CreateAutoScalingGroupRequest createNewAutoScalingGroupRequest(String infraMappingId,
      List<String> infraMappingClassisLbs, List<String> infraMappingTargetGroupArns, String newAutoScalingGroupName,
      AutoScalingGroup baseAutoScalingGroup, Integer harnessRevision, Integer maxInstances,
      LaunchTemplateVersion newLaunchTemplateVersion) {
    List<Tag> tags =
        baseAutoScalingGroup.getTags()
            .stream()
            .filter(tagDescription
                -> !Arrays.asList(HARNESS_AUTOSCALING_GROUP_TAG, NAME_TAG).contains(tagDescription.getKey()))
            /**
             * In case of dynamic base Asg provisioning the base Asg would have a tags like the following,
             * which a user can't create. So we must filter those ones out
             * - aws:cloudformation:logical-id
             * - aws:cloudformation:stack-id
             * - aws:cloudformation:stack-name
             */
            .filter(tagDescription -> !tagDescription.getKey().startsWith("aws:"))
            .map(tagDescription
                -> new Tag()
                       .withKey(tagDescription.getKey())
                       .withValue(tagDescription.getValue())
                       .withPropagateAtLaunch(tagDescription.getPropagateAtLaunch())
                       .withResourceType(tagDescription.getResourceType()))
            .collect(toList());
    tags.add(new Tag()
                 .withKey(HARNESS_AUTOSCALING_GROUP_TAG)
                 .withValue(AsgConvention.getRevisionTagValue(infraMappingId, harnessRevision))
                 .withPropagateAtLaunch(true)
                 .withResourceType(AUTOSCALING_GROUP_RESOURCE_TYPE));
    tags.add(new Tag().withKey(NAME_TAG).withValue(newAutoScalingGroupName).withPropagateAtLaunch(true));

    CreateAutoScalingGroupRequest createAutoScalingGroupRequest =
        new CreateAutoScalingGroupRequest()
            .withAutoScalingGroupName(newAutoScalingGroupName)
            .withDesiredCapacity(0)
            .withMinSize(0)
            .withMaxSize(maxInstances)
            .withTags(tags)
            .withDefaultCooldown(baseAutoScalingGroup.getDefaultCooldown())
            .withAvailabilityZones(baseAutoScalingGroup.getAvailabilityZones())
            .withTerminationPolicies(baseAutoScalingGroup.getTerminationPolicies())
            .withNewInstancesProtectedFromScaleIn(baseAutoScalingGroup.getNewInstancesProtectedFromScaleIn());

    if (newLaunchTemplateVersion != null) {
      createAutoScalingGroupRequest.withLaunchTemplate(
          new LaunchTemplateSpecification()
              .withLaunchTemplateId(newLaunchTemplateVersion.getLaunchTemplateId())
              .withVersion(String.valueOf(newLaunchTemplateVersion.getVersionNumber())));
    } else {
      createAutoScalingGroupRequest.withLaunchConfigurationName(newAutoScalingGroupName);
    }

    if (!Lists.isNullOrEmpty(infraMappingClassisLbs)) {
      createAutoScalingGroupRequest.setLoadBalancerNames(infraMappingClassisLbs);
    }

    if (!Lists.isNullOrEmpty(infraMappingTargetGroupArns)) {
      createAutoScalingGroupRequest.setTargetGroupARNs(infraMappingTargetGroupArns);
    }

    if (baseAutoScalingGroup.getDefaultCooldown() != null) {
      createAutoScalingGroupRequest.setDefaultCooldown(baseAutoScalingGroup.getDefaultCooldown());
    }

    if (isNotBlank(baseAutoScalingGroup.getHealthCheckType())) {
      createAutoScalingGroupRequest.setHealthCheckType(baseAutoScalingGroup.getHealthCheckType());
    }
    if (baseAutoScalingGroup.getHealthCheckGracePeriod() != null) {
      createAutoScalingGroupRequest.setHealthCheckGracePeriod(baseAutoScalingGroup.getHealthCheckGracePeriod());
    }
    if (isNotBlank(baseAutoScalingGroup.getPlacementGroup())) {
      createAutoScalingGroupRequest.setPlacementGroup(baseAutoScalingGroup.getPlacementGroup());
    }

    if (isNotBlank(baseAutoScalingGroup.getVPCZoneIdentifier())) {
      createAutoScalingGroupRequest.setVPCZoneIdentifier(baseAutoScalingGroup.getVPCZoneIdentifier());
    }
    return createAutoScalingGroupRequest;
  }

  @VisibleForTesting
  List<BlockDeviceMapping> getBlockDeviceMappings(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, LaunchConfiguration baseLaunchConfiguration) {
    Set<String> deviceNamesInBaseAmi = awsEc2HelperServiceDelegate.listBlockDeviceNamesOfAmi(
        awsConfig, encryptedDataDetails, region, baseLaunchConfiguration.getImageId());
    List<BlockDeviceMapping> baseMappings = baseLaunchConfiguration.getBlockDeviceMappings();
    if (isNotEmpty(baseMappings)) {
      return baseMappings.stream()
          .filter(mapping -> !deviceNamesInBaseAmi.contains(mapping.getDeviceName()))
          .collect(toList());
    }
    return emptyList();
  }

  @VisibleForTesting
  CreateLaunchConfigurationRequest createNewLaunchConfigurationRequest(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String region, String artifactRevision,
      LaunchConfiguration cloneBaseLaunchConfiguration, String newAutoScalingGroupName, String userData) {
    CreateLaunchConfigurationRequest createLaunchConfigurationRequest =
        new CreateLaunchConfigurationRequest()
            .withLaunchConfigurationName(newAutoScalingGroupName)
            .withImageId(artifactRevision)
            .withSecurityGroups(cloneBaseLaunchConfiguration.getSecurityGroups())
            .withClassicLinkVPCId(cloneBaseLaunchConfiguration.getClassicLinkVPCId())
            .withEbsOptimized(cloneBaseLaunchConfiguration.getEbsOptimized())
            .withAssociatePublicIpAddress(cloneBaseLaunchConfiguration.getAssociatePublicIpAddress());

    if (isNotEmpty(userData)) {
      createLaunchConfigurationRequest.setUserData(userData);
    }

    if (isNotBlank(cloneBaseLaunchConfiguration.getInstanceType())) {
      createLaunchConfigurationRequest.setInstanceType(cloneBaseLaunchConfiguration.getInstanceType());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getKernelId())) {
      createLaunchConfigurationRequest.setKernelId(cloneBaseLaunchConfiguration.getKernelId());
    }

    if (isNotBlank(cloneBaseLaunchConfiguration.getRamdiskId())) {
      createLaunchConfigurationRequest.setRamdiskId(cloneBaseLaunchConfiguration.getRamdiskId());
    }
    if (cloneBaseLaunchConfiguration.getInstanceMonitoring() != null) {
      createLaunchConfigurationRequest.setInstanceMonitoring(cloneBaseLaunchConfiguration.getInstanceMonitoring());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getSpotPrice())) {
      createLaunchConfigurationRequest.setSpotPrice(cloneBaseLaunchConfiguration.getSpotPrice());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getIamInstanceProfile())) {
      createLaunchConfigurationRequest.setIamInstanceProfile(cloneBaseLaunchConfiguration.getIamInstanceProfile());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getPlacementTenancy())) {
      createLaunchConfigurationRequest.setPlacementTenancy(cloneBaseLaunchConfiguration.getPlacementTenancy());
    }
    if (isNotBlank(cloneBaseLaunchConfiguration.getKeyName())) {
      createLaunchConfigurationRequest.setKeyName(cloneBaseLaunchConfiguration.getKeyName());
    }

    return createLaunchConfigurationRequest;
  }

  @VisibleForTesting
  CreateLaunchTemplateVersionRequest createNewLaunchTemplateVersionRequest(
      String artifactRevision, LaunchTemplateVersion baseLaunchTemplateVersion, String userData) {
    RequestLaunchTemplateData launchTemplateData = new RequestLaunchTemplateData().withImageId(artifactRevision);
    if (isNotEmpty(userData)) {
      launchTemplateData = launchTemplateData.withUserData(userData);
    }
    return new CreateLaunchTemplateVersionRequest()
        .withClientToken(UUID.randomUUID().toString())
        .withLaunchTemplateId(baseLaunchTemplateVersion.getLaunchTemplateId())
        .withSourceVersion(String.valueOf(baseLaunchTemplateVersion.getVersionNumber()))
        .withLaunchTemplateData(launchTemplateData);
  }

  @VisibleForTesting
  Integer getNewHarnessVersion(List<AutoScalingGroup> harnessManagedAutoScalingGroups) {
    Integer harnessRevision = 1;
    if (isNotEmpty(harnessManagedAutoScalingGroups)) {
      harnessRevision = harnessManagedAutoScalingGroups.stream()
                            .flatMap(autoScalingGroup -> autoScalingGroup.getTags().stream())
                            .filter(tagDescription -> tagDescription.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG))
                            .mapToInt(tagDescription -> getRevisionFromTag(tagDescription.getValue()))
                            .max()
                            .orElse(0);
      harnessRevision += 1; // bump it by 1
    }
    return harnessRevision;
  }

  @VisibleForTesting
  List<AutoScalingGroup> listAllHarnessManagedAsgs(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String infraMappingId) {
    List<AutoScalingGroup> scalingGroups =
        awsAsgHelperServiceDelegate.listAllAsgs(awsConfig, encryptionDetails, region);
    return scalingGroups.stream()
        .filter(autoScalingGroup
            -> autoScalingGroup.getTags().stream().anyMatch(
                tagDescription -> isHarnessManagedTag(infraMappingId, tagDescription)))
        .sorted(Comparator.comparing(AutoScalingGroup::getCreatedTime).reversed())
        .collect(toList());
  }

  @VisibleForTesting
  LaunchConfiguration ensureAndGetBaseLaunchConfiguration(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName,
      AutoScalingGroup baseAutoScalingGroup, ExecutionLogCallback logCallback) {
    LaunchConfiguration baseAutoScalingGroupLaunchConfiguration = awsAsgHelperServiceDelegate.getLaunchConfiguration(
        awsConfig, encryptionDetails, region, baseAutoScalingGroup.getLaunchConfigurationName());

    if (baseAutoScalingGroupLaunchConfiguration == null) {
      String errorMessage = format(
          "LaunchConfiguration [%s] for referenced AutoScaling Group [%s] provided in Service Infrastructure couldn't be found in AWS region [%s]",
          baseAutoScalingGroup.getAutoScalingGroupName(), autoScalingGroupName, region);
      logCallback.saveExecutionLog(errorMessage, ERROR);
      throw new InvalidRequestException(errorMessage);
    }
    return baseAutoScalingGroupLaunchConfiguration;
  }

  @VisibleForTesting
  AutoScalingGroup ensureAndGetBaseAutoScalingGroup(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String baseAutoScalingGroupName, ExecutionLogCallback logCallback) {
    AutoScalingGroup baseAutoScalingGroup =
        awsAsgHelperServiceDelegate.getAutoScalingGroup(awsConfig, encryptionDetails, region, baseAutoScalingGroupName);
    if (baseAutoScalingGroup == null) {
      String errorMessage =
          format("Couldn't find reference AutoScalingGroup: [%s] in region: [%s]", baseAutoScalingGroupName, region);
      logCallback.saveExecutionLog(errorMessage, ERROR);
      logger.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }
    return baseAutoScalingGroup;
  }

  @VisibleForTesting
  LaunchTemplateVersion ensureAndGetLaunchTemplateVersion(LaunchTemplateSpecification baseLaunchTemplateSpec,
      AutoScalingGroup baseAutoScalingGroup, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, ExecutionLogCallback logCallback) {
    LaunchTemplateVersion baseLaunchTemplateVersion = awsEc2HelperServiceDelegate.getLaunchTemplateVersion(awsConfig,
        encryptionDetails, region, baseLaunchTemplateSpec.getLaunchTemplateId(), baseLaunchTemplateSpec.getVersion());

    if (baseLaunchTemplateVersion == null) {
      String errorMessage = format(
          "LaunchTemplate [%s] , version [%s] for referenced AutoScaling Group [%s] provided in Service Infrastructure couldn't be found in AWS region [%s]",
          baseLaunchTemplateSpec.getLaunchTemplateName(), baseLaunchTemplateSpec.getVersion(),
          baseAutoScalingGroup.getAutoScalingGroupName(), region);
      logCallback.saveExecutionLog(errorMessage, ERROR);
      throw new InvalidRequestException(errorMessage);
    }
    return baseLaunchTemplateVersion;
  }
}