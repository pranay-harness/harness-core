package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.stencils.EnumData;

import java.util.Optional;

@JsonTypeName("AWS_AWS_LAMBDA")
public class AwsLambdaInfraStructureMapping extends InfrastructureMapping {
  /**
   * Instantiates a new Infrastructure mapping.
   */
  public AwsLambdaInfraStructureMapping() {
    super(InfrastructureMappingType.AWS_AWS_LAMBDA.name());
  }

  @Attributes(title = "Region", required = true)
  @NotEmpty
  @EnumData(enumDataProvider = AwsInfrastructureMapping.AwsRegionDataProvider.class)
  private String region;

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDisplayName() {
    return String.format("%s (AWS/Lambda) %s",
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        this.getRegion());
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
