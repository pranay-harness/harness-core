package software.wings.service.impl.newrelic;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.service.impl.analysis.MLAnalysisType;

@CdUniqueIndex(name = "expUniqueIdx", fields = { @Field("ml_analysis_type")
                                                 , @Field("experimentName") })
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
