package software.wings.search.framework.changestreams;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * The change tracking info that has to
 * be provided to open a changestream.
 *
 * @author utkarsh
 */

@OwnedBy(PL)
@Value
@AllArgsConstructor
public class ChangeTrackingInfo<T extends PersistentEntity> {
  private Class<T> morphiaClass;
  private ChangeSubscriber<T> changeSubscriber;
  private String resumeToken;
}
