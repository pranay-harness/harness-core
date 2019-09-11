package software.wings.search.framework.changestreams;

import io.harness.persistence.PersistentEntity;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.function.Consumer;

/**
 * The change tracking info that has to
 * be provided to open a changestream.
 *
 * @author utkarsh
 */

@Value
@AllArgsConstructor
public class ChangeTrackingInfo {
  private Class<? extends PersistentEntity> morphiaClass;
  private Consumer<ChangeEvent> changeEventConsumer;
  private String resumeToken;
}
