package io.harness.cvng.beans.change;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

  @Override
  @JsonIgnore
  public ChangeSourceType getType() {
    return ChangeSourceType.PAGER_DUTY;
  }
}
