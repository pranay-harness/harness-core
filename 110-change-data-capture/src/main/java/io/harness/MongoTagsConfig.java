package io.harness;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CE)
@Value
@Builder
public class MongoTagsConfig {
  @JsonProperty(defaultValue = "none") @Builder.Default @NotEmpty private String tagKey = "none";
  @JsonProperty(defaultValue = "none") @Builder.Default @NotEmpty private String tagValue = "none";
}
