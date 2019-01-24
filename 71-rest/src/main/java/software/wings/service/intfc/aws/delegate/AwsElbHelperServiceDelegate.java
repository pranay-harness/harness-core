package software.wings.service.intfc.aws.delegate;

import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import io.harness.delegate.task.protocol.AwsElbListener;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AwsElbHelperServiceDelegate {
  List<String> listClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  List<String> listApplicationLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  List<String> listNetworkLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  List<String> listElasticLoadBalancers(
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
}