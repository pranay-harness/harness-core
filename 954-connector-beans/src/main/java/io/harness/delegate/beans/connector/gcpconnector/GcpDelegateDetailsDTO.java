package io.harness.delegate.beans.connector.gcpconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(GcpConstants.INHERIT_FROM_DELEGATE)
@ApiModel("GcpDelegateDetails")
@Schema(name = "GcpDelegateDetails", description = "This contains GCP connector delegate details")
public class GcpDelegateDetailsDTO implements GcpCredentialSpecDTO {
  @NotNull @Size(min = 1) Set<String> delegateSelectors;
}
