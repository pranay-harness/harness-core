package software.wings.helpers.ext.mail;

import io.harness.annotation.HarnessEntity;
import io.harness.queue.Queuable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;

import java.util.ArrayList;
import java.util.List;

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
  @Builder.Default private List<String> bcc = new ArrayList<>();
  private String subject;
  private String body;
  private String templateName;
  private Object templateModel;
  @Builder.Default private boolean hasHtml = true;
  private boolean system;
  private String appId;
  private String workflowExecutionId;
}
