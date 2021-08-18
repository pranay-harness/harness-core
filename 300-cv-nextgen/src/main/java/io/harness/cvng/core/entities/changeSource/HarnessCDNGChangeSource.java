package io.harness.cvng.core.entities.changeSource;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("HarnessCDNG")
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HarnessCDNGChangeSource extends ChangeSource {
  public static class UpdatableCDNGChangeSourceEntity
      extends UpdatableChangeSourceEntity<HarnessCDNGChangeSource, HarnessCDNGChangeSource> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<HarnessCDNGChangeSource> updateOperations, HarnessCDNGChangeSource harnessCdngChangeSource) {
      setCommonOperations(updateOperations, harnessCdngChangeSource);
    }
  }
}
