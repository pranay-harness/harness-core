package io.harness.virtualstack;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.manage.GlobalContextManager;
import io.harness.serializer.KryoSerializer;

import com.google.protobuf.ByteString;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class VirtualStackUtils {
  public VirtualStackRequest populateRequest(KryoSerializer kryoSerializer) {
    return VirtualStackRequest.newBuilder()
        .setGlobalContext(
            ByteString.copyFrom(kryoSerializer.asDeflatedBytes(GlobalContextManager.obtainGlobalContext())))
        .build();
  }
}
