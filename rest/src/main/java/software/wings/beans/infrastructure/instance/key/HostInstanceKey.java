package software.wings.beans.infrastructure.instance.key;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Indexed;

/**
 * Host based instance key like physical host and cloud instances like ec2 , gcp instance.
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostInstanceKey extends InstanceKey {
  @Indexed private String hostName;
  @Indexed private String infraMappingId;
}
