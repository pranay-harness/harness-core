package software.wings.helpers.ext.jenkins;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(_960_API_SERVICES)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class CustomRepositoryResponse {
  @Builder.Default private List<Result> results = new ArrayList<>();

  @Data
  @Builder
  public static class Result {
    String buildNo;
    @Builder.Default Map<String, String> metadata = new HashMap<>();
  }
}
