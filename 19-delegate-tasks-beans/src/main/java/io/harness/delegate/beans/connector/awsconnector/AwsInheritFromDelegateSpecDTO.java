package io.harness.delegate.beans.connector.awsconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AwsConstants.inheritFromDelegate)
public class AwsInheritFromDelegateSpecDTO implements AwsCredentialSpecDTO {
  @NotNull String delegateSelector;
}
