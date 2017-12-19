package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

/**
 * Created by anubhaw on 12/19/17.
 */
@JsonTypeName("AWS_AMI")
@Data
@Builder
@AllArgsConstructor
public class AwsAmiInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Region")
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private String region;

  @Attributes(title = "Auto Scaling group") private String autoScalingGroupName;

  public AwsAmiInfrastructureMapping() {
    super(InfrastructureMappingType.AWS_AMI.name());
  }

  @Override
  public String getDefaultName() {
    return null;
  }

  public String getHostConnectionAttrs() {
    return null;
  }
}
