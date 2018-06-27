package software.wings.cloudprovider;

import com.amazonaws.services.ec2.model.Instance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by brett on 4/6/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfo {
  public enum Status { SUCCESS, FAILURE }

  private String hostName;
  private String ip;
  private String containerId;
  private Instance ec2Instance;
  private Status status;
}
