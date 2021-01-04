package io.harness.k8s.model.statssummary;

import com.google.gson.annotations.SerializedName;
import io.kubernetes.client.common.KubernetesType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PodStatsList implements KubernetesType {
  @SerializedName("kind") private String kind;
  @SerializedName("apiVersion") private String apiVersion;

  @SerializedName("pods")
  private List<PodStats> items =
      new ArrayList(); // initialized because better to have array of size 0 then having a null
}
