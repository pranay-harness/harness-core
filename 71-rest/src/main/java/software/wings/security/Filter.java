package software.wings.security;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author rktummala on 02/08/18
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
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
