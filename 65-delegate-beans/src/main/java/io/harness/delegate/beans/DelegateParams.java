package io.harness.delegate.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
  String status;
  String version;
  String delegateType;
  String delegateRandomToken;
  String sequenceNum;
  long lastHeartBeat;

  boolean sampleDelegate;
  boolean keepAlivePacket;
  boolean polllingModeEnabled;

  List<String> currentlyExecutingDelegateTasks;
}
