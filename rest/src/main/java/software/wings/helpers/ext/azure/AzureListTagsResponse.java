package software.wings.helpers.ext.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureListTagsResponse {
  private List<TagDetails> value;
}
