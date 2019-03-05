package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

@Entity(value = "delegateSequenceConfig", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("hostName"), @Field("sequenceNum") },

      options = @IndexOptions(unique = true, name = "uniqueDelegateSequenceIdx"))
})
@HarnessExportableEntity
public class DelegateSequenceConfig extends Base {
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String HOST_NAME_KEY = "hostName";
  public static final String SEQUENCE_NUM = "sequenceNum";
  public static final String DELEGATE_TOKEN = "delegateToken";

  @NotEmpty private String accountId;
  @NotEmpty private String hostName;
  @NotEmpty private Integer sequenceNum;
  @NotEmpty private String delegateToken;

  public static final class Builder {
    private String accountId;
    private String hostName;
    private Integer sequenceNum;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String delegateToken;

    private Builder() {}

    public static DelegateSequenceConfig.Builder aDelegateSequenceBuilder() {
      return new DelegateSequenceConfig.Builder();
    }

    public DelegateSequenceConfig.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public DelegateSequenceConfig.Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public DelegateSequenceConfig.Builder withSequenceNum(Integer sequenceNum) {
      this.sequenceNum = sequenceNum;
      return this;
    }

    public DelegateSequenceConfig.Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public DelegateSequenceConfig.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public DelegateSequenceConfig.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public DelegateSequenceConfig.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public DelegateSequenceConfig.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public DelegateSequenceConfig.Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public DelegateSequenceConfig.Builder withDelegateToken(String delegateToken) {
      this.delegateToken = delegateToken;
      return this;
    }

    public DelegateSequenceConfig build() {
      DelegateSequenceConfig delegateSequenceConfig = new DelegateSequenceConfig();
      delegateSequenceConfig.setAccountId(accountId);
      delegateSequenceConfig.setHostName(hostName);
      delegateSequenceConfig.setSequenceNum(sequenceNum);
      delegateSequenceConfig.setUuid(uuid);
      delegateSequenceConfig.setAppId(appId);
      delegateSequenceConfig.setCreatedBy(createdBy);
      delegateSequenceConfig.setCreatedAt(createdAt);
      delegateSequenceConfig.setLastUpdatedBy(lastUpdatedBy);
      delegateSequenceConfig.setLastUpdatedAt(lastUpdatedAt);
      delegateSequenceConfig.setDelegateToken(delegateToken);
      return delegateSequenceConfig;
    }
  }
}
