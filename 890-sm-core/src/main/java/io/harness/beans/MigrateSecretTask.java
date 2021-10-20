package io.harness.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;

import javax.validation.constraints.NotNull;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "kmsTransitionEvent2", noClassnameStored = true)
@HarnessEntity(exportable = false)
public final class MigrateSecretTask extends Queuable {
  @NotNull private String accountId;
  @NotNull private String secretId;
  @NotNull private SecretManagerConfig fromConfig;
  @NotNull private SecretManagerConfig toConfig;
}
