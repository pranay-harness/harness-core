package software.wings.search.framework;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class ElasticsearchConfig {
  @JsonProperty(defaultValue = "http://localhost:9200")
  @Builder.Default
  @NotEmpty
  private String uri = "http://localhost:9200";

  @JsonProperty(defaultValue = "_default") @Builder.Default @NotEmpty private String indexSuffix = "_default";
}
