package software.wings.service.impl.newrelic;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.service.impl.analysis.MLAnalysisType;

@Indexes({
  @Index(name = "expUniqueIdx", fields = {
    @Field("ml_analysis_type"), @Field("experimentName")
  }, options = @IndexOptions(unique = true))
})
@Data
@FieldNameConstants(innerTypeName = "MLExperimentsKeys")
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity(value = "mlExperiments", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class MLExperiments extends Base {
  private MLAnalysisType ml_analysis_type;
  private String experimentName;
  private boolean is24x7;
}
