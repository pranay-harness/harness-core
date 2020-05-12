package software.wings.helpers.ext.nexus.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(CDC)
@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3AssetResponse {
  private List<Asset> items;
  private String continuationToken;
}
