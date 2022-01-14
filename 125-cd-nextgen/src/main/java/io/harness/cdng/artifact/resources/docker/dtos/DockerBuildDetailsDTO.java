package io.harness.cdng.artifact.resources.docker.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerBuildDetailsDTO {
  String tag;
  String buildUrl;
  Map<String, String> metadata;
  Map<String, String> labels;
  String imagePath;
}
