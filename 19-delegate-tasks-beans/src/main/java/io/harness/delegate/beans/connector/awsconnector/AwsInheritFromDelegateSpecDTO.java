package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("AwsInheritFromDelegateSpec")
public class AwsInheritFromDelegateSpecDTO implements AwsCredentialSpecDTO {
  @NotNull String delegateSelector;
}
