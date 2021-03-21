package io.harness.connector.entities.embedded.ceawsconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

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
@OwnedBy(DX)
public class S3BucketDetails {
  String s3BucketName;
  String s3Prefix;
  String region;
}
