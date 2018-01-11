package software.wings.helpers.ext.mail;

import com.google.common.collect.Lists;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
@Entity(value = "emailQueue", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailData extends Queuable {
  private String accountId;
  @Builder.Default private List<String> to = Lists.newArrayList();
  @Builder.Default private List<String> cc = Lists.newArrayList();
  private String subject;
  private String body;
  private String templateName;
  private Object templateModel;
  @Builder.Default private boolean hasHtml = true;
  @Builder.Default private boolean system;
  private String appId;
  private String workflowExecutionId;
}
