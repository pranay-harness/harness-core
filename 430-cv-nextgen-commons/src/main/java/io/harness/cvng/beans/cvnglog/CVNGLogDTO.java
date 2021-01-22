package io.harness.cvng.beans.cvnglog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class CVNGLogDTO {
  private String accountId;
  private String traceableId;
  private long createdAt;
  private Instant startTime;
  private Instant endTime;
  private TraceableType traceableType;
  public abstract CVNGLogType getType();
}