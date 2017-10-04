package software.wings.yaml.gitSync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

/**
 * Created by bsollish on 10/03/17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "gitSyncWebhook", noClassnameStored = true)
@Indexes(
    @Index(fields = { @Field("webhook")
                      , @Field("entityId") }, options = @IndexOptions(name = "gitSyncWebhookIdx")))
public class GitSyncWebhook extends Base {
  private String accountId;
  private String webhookToken;
  private String entityId;
}