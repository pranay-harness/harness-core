package software.wings.api;

import io.harness.queue.Queuable;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by rsingh on 10/6/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "kmsTransitionEvent", noClassnameStored = true)
public class KmsTransitionEvent extends Queuable {
  private String entityId;
  private EncryptionType fromEncryptionType;
  private String fromKmsId;
  private EncryptionType toEncryptionType;
  private String toKmsId;
  private String accountId;
}
