package io.harness.aws;

public interface AwsClient {
  void validateAwsAccountCredential(AwsConfig awsConfig);
  String getAmazonEcrAuthToken(AwsConfig awsConfig, String account, String region);
}
