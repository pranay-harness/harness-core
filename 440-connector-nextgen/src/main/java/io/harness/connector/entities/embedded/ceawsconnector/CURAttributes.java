package io.harness.connector.entities.embedded.ceawsconnector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CURAttributesKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.ceawsconnector.CURAttributes")
public class CURAttributes {
  String reportName;
  S3BucketDetails s3BucketDetails;
}
