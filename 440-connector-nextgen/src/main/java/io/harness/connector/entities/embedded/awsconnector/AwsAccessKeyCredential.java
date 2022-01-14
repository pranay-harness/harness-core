package io.harness.connector.entities.embedded.awsconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AwsAccessKeyCredentialKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.awsconnector.AwsAccessKeyCredential")
public class AwsAccessKeyCredential implements AwsCredential {
  String secretKeyRef;
  String accessKey;
  String accessKeyRef;
}
