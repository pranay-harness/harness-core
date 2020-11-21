package io.harness.k8s.model.statssummary;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PodStats {
  @SerializedName("podRef") private PodRef podRef;
  @SerializedName("volume") private List<Volume> volumeList = new ArrayList<>();
}
