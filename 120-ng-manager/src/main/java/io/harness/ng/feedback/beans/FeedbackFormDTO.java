package io.harness.ng.feedback.beans;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@OwnedBy(GTM)
@Schema(name = "FeedbackForm", description = "This is the view of feedback form defined in Harness")
public class FeedbackFormDTO {
  String accountId;
  String email;
  ModuleType moduleType;
  Integer score;
  String suggestion;
}
