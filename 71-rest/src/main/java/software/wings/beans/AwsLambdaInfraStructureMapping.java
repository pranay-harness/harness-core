package software.wings.beans;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.exception.InvalidRequestException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.stencils.EnumData;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The type Aws lambda infra structure mapping.
 */
@JsonTypeName("AWS_AWS_LAMBDA")
@Data
@EqualsAndHashCode(callSuper = false)
public class AwsLambdaInfraStructureMapping extends InfrastructureMapping {
  /**
   * Instantiates a new Infrastructure mapping.
   */
  public AwsLambdaInfraStructureMapping() {
    super(InfrastructureMappingType.AWS_AWS_LAMBDA.name());
  }

  @Attributes(title = "Region", required = true)
  @EnumData(enumDataProvider = AwsInfrastructureMapping.AwsRegionDataProvider.class)
  private String region;

  @Attributes(title = "VPC") private String vpcId;
  @Attributes(title = "Subnets") private List<String> subnetIds = new ArrayList<>();
  @Attributes(title = "Security Groups") private List<String> securityGroupIds = new ArrayList<>();
  @Attributes(title = " IAM role") private String role;

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Util.normalize(format("%s (AWS_Lambda) %s",
        Optional.ofNullable(this.getComputeProviderName()).orElse(this.getComputeProviderType().toLowerCase()),
        StringUtils.isEmpty(getProvisionerId()) ? this.getRegion() : ""));
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      switch (entry.getKey()) {
        case "region":
          try {
            setRegion((String) entry.getValue());
          } catch (ClassCastException e) {
            throw new InvalidRequestException("Region should be of String type. Found : " + entry.getValue());
          }
          break;
        case "role":
          try {
            setRole((String) entry.getValue());
          } catch (ClassCastException e) {
            throw new InvalidRequestException("IAM Role should be of String type. Found : " + entry.getValue());
          }
          break;
        case "vpcId":
          try {
            setVpcId((String) entry.getValue());
          } catch (ClassCastException e) {
            throw new InvalidRequestException("Vpc Id should be of String type. Found : " + entry.getValue());
          }
          break;
        case "subnetIds":
          try {
            setSubnetIds(getList(entry.getValue()));
          } catch (ClassCastException e) {
            throw new InvalidRequestException(
                "Subnet Ids should be of List or comma-separated String type. Found : " + entry.getValue());
          }
          break;
        case "securityGroups":
          try {
            setSecurityGroupIds(getList(entry.getValue()));
          } catch (ClassCastException e) {
            throw new InvalidRequestException(
                "Security Groups should be of List or comma-separated String type. Found : " + entry.getValue());
          }
          break;
        default:
          throw new InvalidRequestException("UnSupported Provisioner Mapping " + entry.getKey());
      }
    }
    if (StringUtils.isEmpty(region)) {
      throw new InvalidRequestException("Region Mapping is Required");
    }
    if (StringUtils.isEmpty(role)) {
      throw new InvalidRequestException("Role Mapping is Required");
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends InfrastructureMapping.YamlWithComputeProvider {
    private String region;
    private String vpcId;
    private List<String> subnetIds = new ArrayList<>();
    private List<String> securityGroupIds = new ArrayList<>();
    private String role;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String computeProviderType, String serviceName,
        String infraMappingType, String deploymentType, String computeProviderName, String region, String vpcId,
        List<String> subnetIds, List<String> securityGroupIds, String role) {
      super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
          computeProviderName);
      this.region = region;
      this.vpcId = vpcId;
      this.subnetIds = subnetIds;
      this.securityGroupIds = securityGroupIds;
      this.role = role;
    }
  }
}
