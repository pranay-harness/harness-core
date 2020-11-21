package io.harness.ng.core.invites.ext.mail;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "emailQueue2", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class EmailData extends Queuable {
  private String accountId;
  @Builder.Default private List<String> to = new ArrayList<>();
  @Builder.Default private List<String> cc = new ArrayList<>();
  private String subject;
  private String body;
  private String templateName;
  private Object templateModel;
  @Builder.Default private boolean hasHtml = true;
  private boolean system;
  private String appId;
  private String workflowExecutionId;
}
