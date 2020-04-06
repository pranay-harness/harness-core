package software.wings.service.intfc.aws.delegate;

import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import io.harness.delegate.task.aws.LbDetailsForAlbTrafficShift;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AwsElbHelperServiceDelegate {
  List<AwsLoadBalancerDetails> listApplicationLoadBalancerDetails(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  List<AwsLoadBalancerDetails> listNetworkLoadBalancerDetails(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  List<AwsLoadBalancerDetails> listElasticLoadBalancerDetails(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  List<String> listClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  Map<String, String> listTargetGroupsForAlb(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName);
  void waitForAsgInstancesToRegisterWithTargetGroup(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String targetGroupArn, String asgName, int timeout, ExecutionLogCallback logCallback);
  void waitForAsgInstancesToDeRegisterWithTargetGroup(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String targetGroupArn, String asgName, int timeout, ExecutionLogCallback logCallback);
  void waitForAsgInstancesToRegisterWithClassicLB(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String classicLB, String asgName, int timeout, ExecutionLogCallback logCallback);
  void waitForAsgInstancesToDeRegisterWithClassicLB(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String classicLB, String asgName, int timeout, ExecutionLogCallback logCallback);
  TargetGroup cloneTargetGroup(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String targetGroupArn, String newTargetGroupName);
  Listener getElbListener(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String listenerArn);
  List<AwsElbListener> getElbListenersForLoadBalaner(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName);
  Listener createStageListener(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String listenerArn, Integer port, String targetGroupArn);
  Optional<TargetGroup> getTargetGroup(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String targetGroupArn);
  Optional<TargetGroup> getTargetGroupByName(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String targetGroupName);
  Optional<LoadBalancer> getLoadBalancer(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName);
  void updateListenersForEcsBG(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String prodListenerArn,
      String stageListenerArn, String region);
  DescribeListenersResult describeListenerResult(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String prodListenerArn, String region);
  String getTargetGroupForDefaultAction(Listener listener, ExecutionLogCallback executionLogCallback);
  void updateListenersForBGDeployment(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      List<LoadBalancerDetailsForBGDeployment> lbDetailsForBGDeployments, String region,
      ExecutionLogCallback logCallback);
  void modifySpecificRule(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String ruleArn, String targetGroupArn, ExecutionLogCallback logCallback);
  TargetGroup fetchTargetGroupForSpecificRules(AwsElbListener listener, String ruleArn,
      ExecutionLogCallback logCallback, AwsConfig awsConfig, String region,
      List<EncryptedDataDetail> encryptionDetails);
  LbDetailsForAlbTrafficShift loadTrafficShiftTargetGroupData(AwsConfig awsConfig, String region,
      List<EncryptedDataDetail> encryptionDetails, LbDetailsForAlbTrafficShift originalLbDetails,
      ExecutionLogCallback logCallback);
}