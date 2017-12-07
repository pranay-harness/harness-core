package software.wings.service.impl.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Data
public class TimeSeriesMLDataSummary {
  private List<List<Double>> data;
  private List<List<Double>> weights;
  private String weights_type;
  private String data_type;
  private List<String> host_names;
}
