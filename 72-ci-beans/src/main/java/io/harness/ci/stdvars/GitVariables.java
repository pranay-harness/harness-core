package io.harness.ci.stdvars;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class GitVariables {
  private String tag;
  private String revision;
  private String targetRepo;
  private String sourceRepo;
  private String targetBranch;
  private String sourceBranch;
  private boolean isPullRequest;
  private boolean pullRequestID;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
