package io.harness.entity;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class InstanceStats {
  private String accountId;
  private String serviceId;
  private String envId;
  private Timestamp reportedAt;
}
