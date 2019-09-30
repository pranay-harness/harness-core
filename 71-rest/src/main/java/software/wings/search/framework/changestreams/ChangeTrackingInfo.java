package software.wings.search.framework.changestreams;

import io.harness.persistence.PersistentEntity;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * The change tracking info that has to
 * be provided to open a changestream.
 *
 * @author utkarsh
 */

@Value
@AllArgsConstructor
public class ChangeTrackingInfo<T extends PersistentEntity> {
  private Class<T> morphiaClass;
  private ChangeSubscriber<T> changeSubscriber;
  private String resumeToken;
}
