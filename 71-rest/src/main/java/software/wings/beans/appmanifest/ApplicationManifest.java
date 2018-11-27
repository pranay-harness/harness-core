package software.wings.beans.appmanifest;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.yaml.YamlType;
import software.wings.yaml.BaseEntityYaml;

@Entity("applicationManifests")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class ApplicationManifest extends Base {
  public static final String SERVICE_ID_KEY = "serviceId";

  @NotEmpty @Indexed(options = @IndexOptions(unique = true)) private String serviceId;
  @NonNull StoreType storeType;

  public ApplicationManifest cloneInternal() {
    ApplicationManifest manifest =
        ApplicationManifest.builder().serviceId(this.serviceId).storeType(this.storeType).build();
    manifest.setAppId(this.appId);
    return manifest;
  }

  @Data
  @EqualsAndHashCode(callSuper = false)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String storeType;
    @Builder
    public Yaml(String type, String harnessApiVersion, String storeType) {
      super(YamlType.APPLICATION_MANIFEST.name(), harnessApiVersion);
      this.storeType = storeType;
    }
  }
}
