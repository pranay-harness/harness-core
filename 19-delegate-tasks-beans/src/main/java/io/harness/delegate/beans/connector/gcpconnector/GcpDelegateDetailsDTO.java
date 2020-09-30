package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("GcpDelegateDetails")
public class GcpDelegateDetailsDTO implements GcpCredentialSpecDTO {
  String delegateSelector;
}
