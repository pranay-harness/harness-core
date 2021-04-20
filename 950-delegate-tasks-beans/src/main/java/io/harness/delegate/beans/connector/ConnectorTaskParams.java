
package io.harness.delegate.beans.connector;

import java.util.Set;
import lombok.Data;
import lombok.experimental.SuperBuilder;
@Data
@SuperBuilder
public class ConnectorTaskParams {
  protected Set<String> delegateSelectors;
}