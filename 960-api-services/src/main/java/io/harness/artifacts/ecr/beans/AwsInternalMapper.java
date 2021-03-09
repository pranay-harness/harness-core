package io.harness.artifacts.ecr.beans;

import com.amazonaws.auth.AWSCredentialsProvider;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AwsInternalMapper {
  EcrInternalConfig toEcrInternalConfig(String region, AWSCredentialsProvider credentialsProvider) {
    return EcrInternalConfig.builder().region(region).credentialsProvider(credentialsProvider).build();
  }
}
