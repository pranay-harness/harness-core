package io.harness.delegate.beans.executioncapability;

// TODO: ALWAYS_TRUE should not be a capability type. In this case, task validation should not even happen.
// But Validation needs to happen at delegate as its part of Handshake between Delegate and manager,
// in order for delegate to acquire a task.
// May be changed later
public enum CapabilityType { HTTP, ALWAYS_TRUE }
