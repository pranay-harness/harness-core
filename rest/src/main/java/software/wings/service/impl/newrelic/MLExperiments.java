package software.wings.service.impl.newrelic;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.service.impl.analysis.MLAnalysisType;

@Entity(value = "mlExperiments", noClassnameStored = true)
@Indexes({
  @Index(fields = {
    @Field("ml_analysis_type"), @Field("experimentName")
  }, options = @IndexOptions(unique = true, name = "expUniqueIdx"))
})
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class MLExperiments extends Base {
  private MLAnalysisType ml_analysis_type;
  private String experimentName;
}
