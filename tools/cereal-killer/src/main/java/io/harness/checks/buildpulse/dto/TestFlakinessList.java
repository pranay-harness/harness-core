
package io.harness.checks.buildpulse.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestFlakinessList {
  @SerializedName("count") @Expose int count;
  @SerializedName("tests") @Expose List<TestFlakiness> tests;
}
