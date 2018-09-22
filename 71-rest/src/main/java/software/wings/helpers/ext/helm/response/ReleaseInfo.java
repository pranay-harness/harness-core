package software.wings.helpers.ext.helm.response;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@Builder
public class ReleaseInfo {
  private String name;
  private String revision;
  private String status;
  private String chart;
  private String namespace;
}
