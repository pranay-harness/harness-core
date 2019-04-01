package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Data
@NoArgsConstructor
@Entity(value = "harnessApiKeys", noClassnameStored = true)
public class HarnessApiKey implements PersistentEntity, UuidAware {
  @Id private String uuid;
  @NotEmpty private byte[] encryptedKey;
  @Indexed @NotEmpty private ClientType clientType;

  @Builder
  private HarnessApiKey(String uuid, byte[] encryptedKey, ClientType clientType) {
    this.uuid = uuid;
    this.encryptedKey = encryptedKey;
    this.clientType = clientType;
  }

  @Override
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  public enum AuthType { API_KEY_HEADER, AUTH_HEADER }

  public enum ClientType {
    SALESFORCE(AuthType.API_KEY_HEADER),
    PROMETHEUS(AuthType.AUTH_HEADER),
    INTERNAL(AuthType.API_KEY_HEADER);

    private final AuthType authType;

    ClientType(AuthType authType) {
      this.authType = authType;
    }

    public AuthType getAuthType() {
      return authType;
    }

    public static boolean isValid(String clientType) {
      if (isEmpty(clientType)) {
        return false;
      }
      if (SALESFORCE.name().equalsIgnoreCase(clientType) || PROMETHEUS.name().equalsIgnoreCase(clientType)
          || INTERNAL.name().equalsIgnoreCase(clientType)) {
        return true;
      }
      return false;
    }
  }
}
