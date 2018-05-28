package software.wings.beans;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.api.LoadBalancerConfig;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.jersey.JsonViews;
import software.wings.settings.UsageRestrictions;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.yaml.setting.LoadBalancerProviderYaml;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@JsonTypeName("ELB")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "secretKey")
public class ElasticLoadBalancerConfig extends LoadBalancerConfig implements Encryptable {
  @Attributes(title = "Region", required = true)
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private Regions region;

  @NotEmpty @Attributes(title = "Elastic Load Balancer Name", required = true) private String loadBalancerName;

  @Attributes(title = "AWS account access key", required = true) @NotEmpty private String accessKey;

  @Attributes(title = "AWS account secret key", required = true) @Encrypted private char[] secretKey;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedSecretKey;

  /**
   * Instantiates a new Elastic load balancer config.
   */
  public ElasticLoadBalancerConfig() {
    super(SettingVariableTypes.ELB.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends LoadBalancerProviderYaml {
    private String region;
    private String loadBalancerName;
    private String accessKey;
    private String secretKey;

    @Builder
    public Yaml(String type, String harnessApiVersion, String region, String loadBalancerName, String accessKey,
        String secretKey, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.region = region;
      this.loadBalancerName = loadBalancerName;
      this.accessKey = accessKey;
      this.secretKey = secretKey;
    }
  }
}
