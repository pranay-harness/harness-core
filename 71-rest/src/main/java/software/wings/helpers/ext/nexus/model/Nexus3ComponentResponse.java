package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3ComponentResponse {
  private List<Component> items;
  private String continuationToken;

  @lombok.Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Component {
    private String id;
    private String repository;
    private String format;
    private String group;
    private String name;
    private String version;
    private List<Asset> assets;
  }
}
