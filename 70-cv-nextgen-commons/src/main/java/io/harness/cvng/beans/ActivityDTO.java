package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class ActivityDTO {
  private String accountIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  private String serviceIdentifier;
  private String environmentIdentifier;
  @NotNull private String name;
  private List<VerificationJobRuntimeDetails> verificationJobRuntimeDetails;
  @NotNull private Long activityStartTime;
  private Long activityEndTime;
  private List<String> tags;

  @JsonIgnore public abstract ActivityType getType();

  @Data
  @Builder
  public static class VerificationJobRuntimeDetails {
    String verificationJobIdentifier;
    Map<String, String> runtimeValues;
  }
}
