package io.harness.cvng.core.entities.changeSource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HarnessCDChangeSource extends ChangeSource {
  public static class UpdatableCDNGChangeSourceEntity
      extends UpdatableChangeSourceEntity<HarnessCDChangeSource, HarnessCDChangeSource> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<HarnessCDChangeSource> updateOperations, HarnessCDChangeSource harnessCDChangeSource) {
      setCommonOperations(updateOperations, harnessCDChangeSource);
    }
  }
}
