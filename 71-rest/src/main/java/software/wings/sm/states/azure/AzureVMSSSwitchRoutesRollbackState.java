package software.wings.sm.states.azure;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.sm.StateType;

public class AzureVMSSSwitchRoutesRollbackState extends AzureVMSSSwitchRoutesState {
  public AzureVMSSSwitchRoutesRollbackState(String name) {
    super(name, StateType.AZURE_VMSS_SWITCH_ROUTES_ROLLBACK.name());
  }

  @Override
  @SchemaIgnore
  public boolean isDownsizeOldVMSSS() {
    return super.isDownsizeOldVMSSS();
  }
}
