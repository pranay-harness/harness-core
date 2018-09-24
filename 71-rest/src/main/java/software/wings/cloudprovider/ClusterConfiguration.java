package software.wings.cloudprovider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by anubhaw on 12/29/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClusterConfiguration {
  private Integer size;
  private String name;
}
