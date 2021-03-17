package software.wings.helpers.ext.k8s.response;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = K8sInstanceSyncResponse.class, name = "K8S_INSTANCE_SYNC_RESPONSE") })
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public interface K8sTaskResponse {}
