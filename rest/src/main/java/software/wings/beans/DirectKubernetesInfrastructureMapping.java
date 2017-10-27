package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encrypted;

/**
 * Created by brett on 2/27/17
 */
@JsonTypeName("DIRECT_KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
public class DirectKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  @Attributes(title = "Master URL", required = true) @NotEmpty private String masterUrl;
  @Attributes(title = "User Name", required = true) @NotEmpty private String username;
  @Attributes(title = "Password", required = true) @NotEmpty @Encrypted private char[] password;
  @Attributes(title = "Namespace") @NotEmpty private String namespace;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public DirectKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.DIRECT_KUBERNETES.name());
  }

  @Override
  @Attributes(title = "Display Name")
  public String getClusterName() {
    return super.getClusterName();
  }
  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDisplayName() {
    return getClusterName() + " (Direct Kubernetes)";
  }

  @SchemaIgnore
  public KubernetesConfig createKubernetesConfig() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder()
                                            .masterUrl(masterUrl)
                                            .username(username)
                                            .password(password)
                                            .namespace(isNotEmpty(namespace) ? namespace : "default")
                                            .build();
    kubernetesConfig.setDecrypted(true);
    return kubernetesConfig;
  }
}
