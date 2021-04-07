package io.harness.delegate.beans.connector.awskmsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsKmsConstants.ASSUME_IAM_ROLE)
@ApiModel("AwsKmsCredentialSpecAssumeIAM")
public class AwsKmsCredentialSpecAssumeIAMDTO implements AwsKmsCredentialSpecDTO {
  @NotNull @Size(min = 1) private Set<String> delegateSelectors;
}
