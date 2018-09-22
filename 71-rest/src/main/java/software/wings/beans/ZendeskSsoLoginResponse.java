package software.wings.beans;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 11/30/17.
 */
@Data
@Builder
public class ZendeskSsoLoginResponse {
  private String redirectUrl;
  private String userId;
}
