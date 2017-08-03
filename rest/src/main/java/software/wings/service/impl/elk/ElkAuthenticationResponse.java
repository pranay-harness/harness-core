package software.wings.service.impl.elk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * Created by rsingh on 8/1/17.
 */
@Data
public class ElkAuthenticationResponse {
  private String username;
  private List<String> roles;
  private String full_name;
  private String email;
  private boolean enabled;

  @JsonIgnore private Object metadata;
  private ElkAuthenticationError error;
}
