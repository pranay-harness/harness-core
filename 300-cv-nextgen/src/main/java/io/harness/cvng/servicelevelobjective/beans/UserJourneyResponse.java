package io.harness.cvng.servicelevelobjective.beans;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CV)
@Value
public class UserJourneyResponse {
  @NotNull @JsonProperty("userJourney") private UserJourneyDTO userJourneyDTO;
  private Long createdAt;
  private Long lastModifiedAt;

  @Builder
  public UserJourneyResponse(UserJourneyDTO userJourneyDTO, Long createdAt, Long lastModifiedAt) {
    this.userJourneyDTO = userJourneyDTO;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
  }
}
