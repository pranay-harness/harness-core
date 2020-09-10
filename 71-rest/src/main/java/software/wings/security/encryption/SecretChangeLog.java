package software.wings.security.encryption;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 11/01/17.
 */
@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "secretChangeLogs", noClassnameStored = true)
@HarnessEntity(exportable = false)
@CdIndex(name = "acctEncryptedDataIdx", fields = { @Field("accountId")
                                                   , @Field("encryptedDataId") })
@FieldNameConstants(innerTypeName = "SecretChangeLogKeys")
public class SecretChangeLog extends Base implements AccountAccess {
  @NotEmpty private String accountId;

  @NotEmpty private String encryptedDataId;

  @NotNull private EmbeddedUser user;

  @NotEmpty private String description;

  // Secret change log could be retrieved from external system such as Vault (secret versions metadata)
  // This flag is used to denote if this log entry is originated from external system.
  private boolean external;
}
