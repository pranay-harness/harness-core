package io.harness.delegate.beans.connector.awskmsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

//import static com.amazonaws.auth.STSSessionCredentialsProvider.DEFAULT_DURATION_SECONDS;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsKmsConstants.ASSUME_IAM_ROLE)
@ApiModel("AwsKmsCredentialSpecAssumeSTS")
public class AwsKmsCredentialSpecAssumeSTSDTO implements AwsKmsCredentialSpecDTO {
  @NotNull
  @Size(min = 1)
  private Set<String> delegateSelectors;
  @ApiModelProperty(dataType = "string")
  @NotNull
  private String roleArn;
  @ApiModelProperty(dataType = "string")
  @NotNull
  private String externalName;
  //@Builder.Default
  //private int assumeStsRoleDuration = DEFAULT_DURATION_SECONDS;
}
