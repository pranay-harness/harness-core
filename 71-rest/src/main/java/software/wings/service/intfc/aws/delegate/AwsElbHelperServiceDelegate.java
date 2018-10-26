package software.wings.service.intfc.aws.delegate;

import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

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
}