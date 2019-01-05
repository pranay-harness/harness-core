package software.wings.service.intfc.aws.delegate;

import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface AwsEcsHelperServiceDelegate {
  List<String> listClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);

  void updateListenersForEcsBG(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String prodListenerArn,
      String stageListenerArn, String region);

  DescribeListenersResult describeListenerResult(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String prodListenerArn, String region);
}