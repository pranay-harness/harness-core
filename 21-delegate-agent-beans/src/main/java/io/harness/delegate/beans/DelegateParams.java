package io.harness.delegate.beans;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DelegateParams {
  String delegateId;
  String accountId;
  String ip;
  String hostName;
  String delegateName;
  String delegateGroupName;
  String delegateProfileId;
  String description;
  String version;
  String delegateType;
  String delegateRandomToken;
  String sequenceNum;
  String location;
  long lastHeartBeat;

  boolean sampleDelegate;
  boolean keepAlivePacket;
  boolean polllingModeEnabled;
  boolean proxy;
  boolean ceEnabled;

  List<String> currentlyExecutingDelegateTasks;
}
