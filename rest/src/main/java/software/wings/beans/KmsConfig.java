package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encrypted;
import software.wings.security.EncryptionType;
import software.wings.service.intfc.security.EncryptionConfig;

/**
 * Created by rsingh on 9/29/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Indexes({
  @Index(fields = { @Field("name"), @Field("accountId") }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
@Entity(value = "kmsConfig", noClassnameStored = true)
public class KmsConfig extends Base implements EncryptionConfig {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "AWS Access Key", required = true) @Encrypted private String accessKey;

  @Attributes(title = "AWS Secret Key", required = true) @Encrypted private String secretKey;

  @Attributes(title = "AWS key ARN", required = true) @Encrypted private String kmsArn;

  private boolean isDefault = true;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore @Transient private int numOfEncryptedValue;

  @SchemaIgnore @Transient private EncryptionType encryptionType;
}
