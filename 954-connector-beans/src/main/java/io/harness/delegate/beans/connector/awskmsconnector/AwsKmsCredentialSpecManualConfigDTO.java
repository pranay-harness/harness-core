package io.harness.delegate.beans.connector.awskmsconnector;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsKmsConstants.MANUAL_CONFIG)
@ApiModel("AwsKmsCredentialSpecManualConfig")
public class AwsKmsCredentialSpecManualConfigDTO implements AwsKmsCredentialSpecDTO {
  @ApiModelProperty(dataType = "string") @NotNull private String accessKey;
  @ApiModelProperty(dataType = "string") @NotNull private String secretKey;
}
