package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(GcpConstants.inheritFromDelegate)
public class GcpDelegateDetailsDTO implements GcpCredentialSpecDTO {
  String delegateSelector;
}
