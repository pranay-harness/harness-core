package io.harness.ccm.service.impl;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import io.harness.aws.CloseableAmazonWebServiceClient;
import io.harness.ccm.commons.beans.billing.CEBucketPolicyJson;
import io.harness.ccm.commons.beans.billing.CEBucketPolicyStatement;
import io.harness.ccm.service.intf.AWSBucketPolicyHelperService;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AWSBucketPolicyHelperServiceImpl implements AWSBucketPolicyHelperService {
  private static final String ceAWSRegion = AWS_DEFAULT_REGION;
  private static final String rolePrefix = "arn:aws:iam";
  private static final String aws = "AWS";

  @Override
  public boolean updateBucketPolicy(
      String crossAccountRoleArn, String awsS3Bucket, String awsAccessKey, String awsSecretKey) {
    AWSCredentialsProvider credentialsProvider = constructBasicAwsCredentials(awsAccessKey, awsSecretKey);
    try (CloseableAmazonWebServiceClient<AmazonS3Client> closeableAmazonS3Client =
             new CloseableAmazonWebServiceClient(getAmazonS3Client(credentialsProvider))) {
      BucketPolicy bucketPolicy = closeableAmazonS3Client.getClient().getBucketPolicy(awsS3Bucket);
      String policyText = bucketPolicy.getPolicyText();
      CEBucketPolicyJson policyJson = new Gson().fromJson(policyText, CEBucketPolicyJson.class);
      List<CEBucketPolicyStatement> listStatements = new ArrayList<>();
      for (CEBucketPolicyStatement statement : policyJson.getStatement()) {
        Map<String, List<String>> principal = statement.getPrincipal();
        List<String> rolesList = principal.get(aws);
        rolesList = rolesList.stream().filter(roleArn -> roleArn.contains(rolePrefix)).collect(Collectors.toList());
        if (rolesList.contains(crossAccountRoleArn)) {
          return true;
        }
        rolesList.add(crossAccountRoleArn);
        principal.put(aws, rolesList);
        statement.setPrincipal(principal);
        listStatements.add(statement);
      }
      policyJson = CEBucketPolicyJson.builder().Version(policyJson.getVersion()).Statement(listStatements).build();
      String updatedBucketPolicy = new Gson().toJson(policyJson);
      closeableAmazonS3Client.getClient().setBucketPolicy(awsS3Bucket, updatedBucketPolicy);
    } catch (Exception e) {
      log.error("Exception updateBucketPolicy", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return true;
  }

  protected AmazonS3Client getAmazonS3Client(AWSCredentialsProvider credentialsProvider) {
    AmazonS3ClientBuilder builder =
        AmazonS3ClientBuilder.standard().withRegion(ceAWSRegion).withForceGlobalBucketAccessEnabled(Boolean.TRUE);
    builder.withCredentials(credentialsProvider);
    return (AmazonS3Client) builder.build();
  }

  public AWSCredentialsProvider constructBasicAwsCredentials(String awsAccessKey, String awsSecretKey) {
    return new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
  }
}
