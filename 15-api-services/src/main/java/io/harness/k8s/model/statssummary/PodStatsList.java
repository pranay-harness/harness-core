package io.harness.k8s.model.statssummary;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PodStatsList {
  @SerializedName("pods")
  private List<PodStats> items =
      new ArrayList(); // initialized because better to have array of size 0 then having a null
}
