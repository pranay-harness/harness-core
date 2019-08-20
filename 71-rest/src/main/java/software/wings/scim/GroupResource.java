package software.wings.scim;

import com.unboundid.scim2.common.BaseScimResource;
import com.unboundid.scim2.common.annotations.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(id = "urn:ietf:params:scim:schemas:core:2.0:Group", name = "Group", description = "Group")
public class GroupResource extends BaseScimResource {
  private String displayName;
  private List<Member> members;
}
