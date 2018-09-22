package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by peeyushaggarwal on 10/6/16.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmbeddedUser {
  private String uuid;
  private String name;
  private String email;
}
