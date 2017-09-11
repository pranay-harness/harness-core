package software.wings.service.impl.instance.sync.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author rktummala on 09/05/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class CloudInstanceFilter extends InstanceFilter {
  protected List<String> hostNames;
}
