package io.harness.cvng.beans.change;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagerDutyEventMetaData extends ChangeEventMetadata {
  String eventId;
  String pagerDutyUrl;
  String title;
  String status;
  Instant triggeredAt;
  String urgency;
  String htmlUrl;
  String priority;
  String assignment;
  String assignmentUrl;
  String escalationPolicy;
  String escalationPolicyUrl;

  @Override
  @JsonIgnore
  public ChangeSourceType getType() {
    return ChangeSourceType.PAGER_DUTY;
  }
}
