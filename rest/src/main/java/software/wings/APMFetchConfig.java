package software.wings;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class APMFetchConfig {
  String url;
  String body;
}
