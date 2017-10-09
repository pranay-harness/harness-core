package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by rsingh on 9/29/17.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(value = "kmsConfig", noClassnameStored = true)
public class KmsConfig extends Base {
  @Attributes(title = "Name", required = true) private String name;

  @Attributes(title = "AWS Access Key", required = true) @Encrypted private String accessKey;

  @Attributes(title = "AWS Secret Key", required = true) @Encrypted private String secretKey;

  @Attributes(title = "AWS key ARN", required = true) @Encrypted private String kmsArn;

  private boolean isDefault = true;

  @SchemaIgnore @NotEmpty private String accountId;
}
