package io.harness.delegate.beans.connector.awsconnector;

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
@JsonTypeName(AwsConstants.INHERIT_FROM_DELEGATE)
@ApiModel("AwsDelegateDetails")
@Schema(name = "AwsDelegateDetails", description = "This contains AWS delegate details")
public class AwsDelegateDetailsDTO {
  @NotNull @Size(min = 1) Set<String> delegateSelectors;
}
