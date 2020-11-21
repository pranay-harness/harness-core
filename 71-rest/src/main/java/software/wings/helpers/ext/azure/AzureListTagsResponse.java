package software.wings.helpers.ext.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureListTagsResponse {
  private List<TagDetails> value;
}
