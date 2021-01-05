package io.harness.connector.entities.embedded.ceawsconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "S3BucketDetailsKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.ceawsconnector.S3BucketDetails")
public class S3BucketDetails {
  private String s3BucketName;
  private String s3Prefix;
  private String region;
}
