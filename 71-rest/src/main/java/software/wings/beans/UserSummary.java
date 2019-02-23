package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode
public class UserSummary {
  @NotEmpty private String name;
  @Email private String email;
  private List<String> accountIds = new ArrayList<>();
  private boolean emailVerified;
}
