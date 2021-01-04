package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Asset {
  private String downloadUrl;
  private String path;
  private String id;
  private String repository;
  private String format;
  private Checksum checksum;

  @lombok.Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Checksum {
    private String sha1;
    private String sha512;
  }
}
