package software.wings.helpers.ext.jenkins.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.offbytwo.jenkins.model.JobWithDetails;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JobWithExtendedDetails extends JobWithDetails {
  @JsonProperty("property") List<JobProperty> properties;
}
