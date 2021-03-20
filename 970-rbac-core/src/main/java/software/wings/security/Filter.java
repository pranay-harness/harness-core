package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@JsonSubTypes({
  @Type(value = GenericEntityFilter.class, name = "GenericEntityFilter")
  , @Type(value = EnvFilter.class, name = "EnvFilter"), @Type(value = WorkflowFilter.class, name = "WorkflowFilter")
})
public abstract class Filter {
  private Set<String> ids;

  public Filter(Set<String> ids) {
    this.ids = ids;
  }
}
