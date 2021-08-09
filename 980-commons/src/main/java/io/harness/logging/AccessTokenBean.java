package io.harness.logging;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessTokenBean {
  private String projectId;
  private String tokenValue;
  private long expirationTimeMillis;
}
