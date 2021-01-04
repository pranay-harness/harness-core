package software.wings.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class UrlConfiguration {
  private String portalUrl;
  private String apiUrl;
  private String delegateMetadataUrl;
  private String watcherMetadataUrl;
}
