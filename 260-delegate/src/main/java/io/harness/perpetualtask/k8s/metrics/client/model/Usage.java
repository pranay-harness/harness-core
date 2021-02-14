package io.harness.perpetualtask.k8s.metrics.client.model;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode
@TargetModule(Module._420_DELEGATE_AGENT)
public class Usage implements Serializable {
  @SerializedName("cpu") String cpu;
  @SerializedName("memory") String memory;
}
