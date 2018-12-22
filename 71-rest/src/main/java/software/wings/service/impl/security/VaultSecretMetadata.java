package software.wings.service.impl.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * @author marklu on 2018-12-07
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class VaultSecretMetadata {
  @JsonProperty("current_version") private int currentVersion;
  @JsonProperty("updated_time") String updatedTime;
  private Map<Integer, VersionMetadata> versions;

  public static class VersionMetadata {
    @JsonProperty("created_time") String createdTime;
    @JsonProperty("deletion_time") String deletionTime;
    boolean destroyed;
  }
}
