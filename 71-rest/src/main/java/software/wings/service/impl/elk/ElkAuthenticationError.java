package software.wings.service.impl.elk;

import lombok.Data;

/**
 * Created by rsingh on 8/1/17.
 */
@Data
public class ElkAuthenticationError {
  private String type;
  private String reason;
}
