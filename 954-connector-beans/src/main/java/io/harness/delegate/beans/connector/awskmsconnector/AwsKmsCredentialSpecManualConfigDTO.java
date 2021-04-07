package io.harness.delegate.beans.connector.awskmsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsKmsConstants.MANUAL_CONFIG)
@ApiModel("AwsKmsCredentialSpecManualConfig")
public class AwsKmsCredentialSpecManualConfigDTO implements AwsKmsCredentialSpecDTO {
  @ApiModelProperty(dataType = "string")
  @NotNull
  private String accessKey;
  @ApiModelProperty(dataType = "string")
  @NotNull
  private String secretKey;
}
