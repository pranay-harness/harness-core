package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3AssetResponse {
  private List<Asset> items;
  private String continuationToken;
}
