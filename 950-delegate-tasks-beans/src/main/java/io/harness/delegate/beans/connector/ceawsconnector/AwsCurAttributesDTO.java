package io.harness.delegate.beans.connector.ceawsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AwsCurAttributes")
public class AwsCurAttributesDTO {
  @NotNull String reportName;
  @NotNull String s3BucketName;

  @Builder
  public AwsCurAttributesDTO(String reportName, String s3BucketName) {
    this.reportName = reportName;
    this.s3BucketName = s3BucketName;
  }
}
