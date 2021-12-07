package io.harness.cvng.servicelevelobjective.beans.slotargetspec;

import io.harness.cvng.servicelevelobjective.beans.SLOTargetType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalenderSLOTargetSpec extends SLOTargetSpec {
  @JsonProperty("type") CalenderType type;
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  CalenderSpec spec;
  public enum CalenderType {
    @JsonProperty("Weekly") WEEKLY,
    @JsonProperty("Monthly") MONTHLY,
    @JsonProperty("Quarterly") QUARTERLY
  }
  @SuperBuilder
  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = WeeklyCalendarSpec.class, name = "Weekly")
    , @JsonSubTypes.Type(value = MonthlyCalenderSpec.class, name = "Monthly"),
        @JsonSubTypes.Type(value = QuarterlyCalenderSpec.class, name = "Quarterly"),
  })
  public abstract static class CalenderSpec {
    @JsonIgnore public abstract CalenderType getType();
  }
  @SuperBuilder
  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class WeeklyCalendarSpec extends CalenderSpec {
    public enum DayOfWeek {
      @JsonProperty("Mon") MONDAY,
      @JsonProperty("Tue") TUESDAY,
      @JsonProperty("Wed") WEDNESDAY,
      @JsonProperty("Thu") THURSDAY,
      @JsonProperty("Fri") FRIDAY,
      @JsonProperty("Sat") SATURDAY,
      @JsonProperty("Sun") SUNDAY;
    }
    @NotNull private DayOfWeek dayOfWeek;
    @Override
    public CalenderType getType() {
      return CalenderType.WEEKLY;
    }
  }
  @SuperBuilder
  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MonthlyCalenderSpec extends CalenderSpec {
    @NotNull private int dayOfMonth;
    @Override
    public CalenderType getType() {
      return CalenderType.MONTHLY;
    }
  }
  @SuperBuilder
  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class QuarterlyCalenderSpec extends CalenderSpec {
    @Override
    public CalenderType getType() {
      return CalenderType.QUARTERLY;
    }
  }

  @Override
  public SLOTargetType getType() {
    return SLOTargetType.CALENDER;
  }
}
