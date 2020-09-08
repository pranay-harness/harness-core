package io.harness.k8s.model.statssummary;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class PodStatsList {
  @SerializedName("pods")
  private List<PodStats> items =
      new ArrayList(); // initialized because better to have array of size 0 then having a null
}
