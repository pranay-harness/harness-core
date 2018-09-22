package software.wings.helpers.ext.external.comm;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@NoArgsConstructor
public abstract class CollaborationProviderRequest {
  @NotEmpty private CommunicationType communicationType;

  public CollaborationProviderRequest(CommunicationType communicationType) {
    this.communicationType = communicationType;
  }

  public abstract CommunicationType getCommunicationType();

  public abstract List<String> getCriteria();

  public enum CommunicationType { EMAIL }
}
