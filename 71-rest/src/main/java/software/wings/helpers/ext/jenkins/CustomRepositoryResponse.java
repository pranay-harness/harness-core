package software.wings.helpers.ext.jenkins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class CustomRepositoryResponse {
  private List<Result> results = new ArrayList<>();

  @Data
  @Builder
  public static class Result {
    String buildNo;
    @Builder.Default Map<String, String> metadata = new HashMap<>();
  }
}
