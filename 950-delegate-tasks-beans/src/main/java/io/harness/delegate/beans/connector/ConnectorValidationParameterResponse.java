package io.harness.delegate.beans.connector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ConnectorValidationParameterResponse {
  ConnectorValidationParams connectorValidationParams;
  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) Boolean isInvalid;

  public void setInvalid(boolean isInvalid) {
    this.isInvalid = isInvalid;
  }

  public boolean isInvalid() {
    return Boolean.TRUE.equals(isInvalid);
  }
}
