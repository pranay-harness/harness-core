package software.wings.search.framework;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Elasticsearch Configuration for search
 *
 * @author utkarsh
 */
@Value
@Builder
public class ElasticsearchConfig {
  @JsonProperty(defaultValue = "http://localhost:9200")
  @Builder.Default
  @NotEmpty
  private String uri = "http://localhost:9200";

  @JsonProperty(defaultValue = "_default") @Builder.Default @NotEmpty private String indexSuffix = "_default";
  @JsonProperty(defaultValue = "tag") @Builder.Default @NotEmpty private String mongoTagKey = "tag";
  @JsonProperty(defaultValue = "tagValue") @Builder.Default @NotEmpty private String mongoTagValue = "tagValue";

  private byte[] encryptedUri;
}
