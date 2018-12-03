package software.wings.helpers.ext.docker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class DockerPublicImageTagResponse {
  @Getter
  static class Result {
    @Getter
    static class Image {
      private int size;
      private String architecture;
      private String variant;
      private String features;
      private String os;
      @JsonProperty("os_version") private String osVersion;
      @JsonProperty("os_features") private String osFeatures;
    }

    private String name;
    private String id;
    private int repository;
    private int creator;
    private boolean v2;
    @JsonProperty("image_id") private int imageId;
    @JsonProperty("last_updater") private int lastUpdater;
    @JsonProperty("full_size") private String fullSize;
    @JsonProperty("last_updated") private String lastUpdated;
    private List<Image> images;
  }

  private Integer count;
  private String next;
  private String previous;
  private List<Result> results;
}
