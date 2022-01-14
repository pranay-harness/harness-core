package software.wings.integration.dl;

import io.harness.annotation.HarnessEntity;

import software.wings.beans.Base;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "!!!testDummies", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Dummy extends Base {
  private List<DummyItem> dummies;
  private String name;
}
