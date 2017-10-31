package software.wings.beans;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.LoadBalancerConfig;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.annotation.Encrypted;
import software.wings.annotation.Encryptable;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@JsonTypeName("ELB")
@Data
@Builder
@ToString(exclude = "secretKey")
public class ElasticLoadBalancerConfig extends LoadBalancerConfig implements Encryptable {
  @Attributes(title = "Region", required = true)
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private Regions region;

  @NotEmpty @Attributes(title = "Elastic Load Balancer Name", required = true) private String loadBalancerName;

  @Attributes(title = "AWS account access key", required = true) @NotEmpty private String accessKey;

  @Attributes(title = "AWS account secret key", required = true) @NotEmpty @Encrypted private char[] secretKey;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedSecretKey;

  /**
   * Instantiates a new Elastic load balancer config.
   */
  public ElasticLoadBalancerConfig() {
    super(SettingVariableTypes.ELB.name());
  }

  public ElasticLoadBalancerConfig(Regions region, String loadBalancerName, String accessKey, char[] secretKey,
      String accountId, String encryptedSecretKey) {
    this();
    this.region = region;
    this.loadBalancerName = loadBalancerName;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.accountId = accountId;
    this.encryptedSecretKey = encryptedSecretKey;
  }
}
