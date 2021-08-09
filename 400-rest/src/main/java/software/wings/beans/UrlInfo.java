package software.wings.beans;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 06/19/19
 */

@Data
@Builder
public class UrlInfo {
  private String title;
  private String url;
}
