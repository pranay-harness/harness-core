package software.wings.security.encryption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 11/01/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "secretChangeLogs", noClassnameStored = true)
public class SecretChangeLog extends Base {
  @NotEmpty @Indexed private String accountId;

  @NotEmpty @Indexed private String encryptedDataId;

  @NotNull private EmbeddedUser user;

  @NotEmpty private String description;
}
