package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.security.EncryptionType;
import software.wings.service.intfc.security.EncryptionConfig;

/**
 * Created by rsingh on 11/02/17.
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Indexes({
  @Index(fields = { @Field("name"), @Field("accountId") }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "vaultConfig", noClassnameStored = true)
@ToString(exclude = {"authToken"})
public class VaultConfig extends Base implements EncryptionConfig {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "Vault Url", required = true) private String vaultUrl;

  @Attributes(title = "Auth token", required = true) private String authToken;

  @Attributes(title = "Renew token interval", required = true) private int renewIntervalHours;

  private boolean isDefault = true;

  private long renewedAt;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private EncryptionType encryptionType;

  @SchemaIgnore @Transient private String encryptedBy;
}
