package io.harness.perpetualtask.k8s.metrics.client.model;

import com.google.gson.annotations.SerializedName;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class Usage implements Serializable {
  @SerializedName("cpu") String cpu;
  @SerializedName("memory") String memory;
}
