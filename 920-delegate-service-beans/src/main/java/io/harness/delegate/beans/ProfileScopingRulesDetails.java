package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfileScopingRulesDetails {
  String profileId;
  String profileName;
  Set<String> scopingRulesDescriptions;
}
