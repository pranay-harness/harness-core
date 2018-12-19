package software.wings.beans.approval;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserGroupApprovalParams {
  @Getter @Setter private List<String> userGroups = new ArrayList<>();
  @Getter @Setter private String groupName;
}
