package io.harness.serializer.kryo;

import com.amazonaws.SdkClientException;
import com.amazonaws.internal.SdkInternalList;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.CapacityReservationSpecificationResponse;
import com.amazonaws.services.ec2.model.CapacityReservationTargetResponse;
import com.amazonaws.services.ec2.model.CpuOptions;
import com.amazonaws.services.ec2.model.EbsInstanceBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.IamInstanceProfile;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import com.amazonaws.services.ec2.model.InstanceIpv6Address;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAssociation;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceAttachment;
import com.amazonaws.services.ec2.model.InstancePrivateIpAddress;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Monitoring;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.ProductCode;
import com.amazonaws.services.ec2.model.StateReason;
import com.amazonaws.services.ec2.model.Tag;
import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;

public class ApiServiceKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(Instance.class, 1001);
    kryo.register(SdkInternalList.class, 1002);
    kryo.register(InstanceBlockDeviceMapping.class, 1003);
    kryo.register(EbsInstanceBlockDevice.class, 1004);
    kryo.register(IamInstanceProfile.class, 1005);
    kryo.register(Monitoring.class, 1006);
    kryo.register(InstanceNetworkInterface.class, 1007);
    kryo.register(InstanceNetworkInterfaceAssociation.class, 1008);
    kryo.register(InstanceNetworkInterfaceAttachment.class, 1009);
    kryo.register(GroupIdentifier.class, 1010);
    kryo.register(InstancePrivateIpAddress.class, 1011);
    kryo.register(Placement.class, 1012);
    kryo.register(InstanceState.class, 1013);
    kryo.register(Tag.class, 1014);
    kryo.register(com.amazonaws.AbortedException.class, 1015);
    kryo.register(StateReason.class, 1016);
    kryo.register(SdkClientException.class, 1017);
    kryo.register(InstanceIpv6Address.class, 1018);
    kryo.register(ProductCode.class, 1019);
    kryo.register(Filter.class, 1020);
    kryo.register(Regions.class, 1021);
    kryo.register(CpuOptions.class, 1022);
    kryo.register(CapacityReservationSpecificationResponse.class, 1023);
    kryo.register(CapacityReservationTargetResponse.class, 1024);
  }
}
