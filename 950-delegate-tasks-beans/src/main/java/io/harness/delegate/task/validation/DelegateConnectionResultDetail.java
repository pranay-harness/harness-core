package io.harness.delegate.task.validation;

import java.time.OffsetDateTime;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DelegateConnectionResultDetail {
  private String uuid;
  private long lastUpdatedAt;
  private String accountId;
  private String delegateId;
  private String criteria;
  private boolean validated;
  private long duration;
  private Date validUntil = getValidUntilTime();

  public static Date getValidUntilTime() {
    return Date.from(OffsetDateTime.now().plusDays(30).toInstant());
  }
}
