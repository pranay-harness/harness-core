package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("criteriaSpecWrapperDTO")
public class CriteriaSpecWrapperDTO {
  @NotNull CriteriaSpecType type;

  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  @NotNull
  CriteriaSpecDTO criteriaSpecDTO;

  public static CriteriaSpecWrapperDTO fromCriteriaSpecWrapper(
      CriteriaSpecWrapper criteriaSpecWrapper, boolean skipEmpty) {
    if (criteriaSpecWrapper == null) {
      return null;
    }

    CriteriaSpec criteriaSpec = criteriaSpecWrapper.getCriteriaSpec();
    if (criteriaSpec == null) {
      throw new InvalidRequestException("Criteria Spec can't be null");
    }

    return CriteriaSpecWrapperDTO.builder()
        .type(criteriaSpecWrapper.getType())
        .criteriaSpecDTO(criteriaSpec.toCriteriaSpecDTO(skipEmpty))
        .build();
  }
}
