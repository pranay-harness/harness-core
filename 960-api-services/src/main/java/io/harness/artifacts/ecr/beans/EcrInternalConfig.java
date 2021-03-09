package io.harness.artifacts.ecr.beans;

import com.amazonaws.auth.AWSCredentialsProvider;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcrInternalConfig {
  String region;
  AWSCredentialsProvider credentialsProvider;
}
