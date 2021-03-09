package io.harness.cdng.artifact.resources.ecr.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcrBuildDetailsDTO {
    String tag;
    String buildUrl;
    Map<String, String> metadata;
    Map<String, String> labels;
    String imagePath;
}
