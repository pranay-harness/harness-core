package software.wings.helpers.ext.k8s.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = K8sInstanceSyncResponse.class, name = "K8S_INSTANCE_SYNC_RESPONSE") })
public interface K8sTaskResponse {}
