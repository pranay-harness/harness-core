package software.wings.verification.log;

import static software.wings.sm.states.ElkAnalysisState.DEFAULT_TIME_FIELD;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.FeatureName;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.elk.ElkDataCollectionInfoV2;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ElkCVConfiguration extends LogsCVConfiguration {
  @Attributes(required = true, title = "Query Type") @DefaultValue("TERM") private ElkQueryType queryType;

  @Attributes(title = "Elastic search indices to search", required = true) @DefaultValue("_all") protected String index;

  @Attributes(required = true, title = "Host Name Field") @DefaultValue("hostname") protected String hostnameField;

  @Attributes(required = true, title = "Message Field") @DefaultValue("message") protected String messageField;

  @Attributes(required = true, title = "Timestamp Field")
  @DefaultValue(DEFAULT_TIME_FIELD)
  protected String timestampField;

  @Attributes(required = true, title = "Timestamp format")
  @DefaultValue("yyyy-MM-dd'T'HH:mm:ss.SSSX")
  private String timestampFormat;

  @Override
  public boolean isCVTaskBasedCollectionFeatureFlagged() {
    return true;
  }

  @Override
  public FeatureName getCVTaskBasedCollectionFeatureFlag() {
    return FeatureName.ELK_24_7_CV_TASK;
  }

  @Override
  public CVConfiguration deepCopy() {
    ElkCVConfiguration clonedConfig = new ElkCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setQuery(this.getQuery());
    clonedConfig.setHostnameField(this.getHostnameField());
    clonedConfig.setIndex(this.getIndex());
    clonedConfig.setMessageField(this.getMessageField());
    clonedConfig.setQueryType(this.getQueryType());
    clonedConfig.setTimestampField(this.getTimestampField());
    clonedConfig.setTimestampFormat(this.getTimestampFormat());
    return clonedConfig;
  }

  @Override
  public DataCollectionInfoV2 toDataCollectionInfo() {
    ElkDataCollectionInfoV2 elkDataCollectionInfoV2 = ElkDataCollectionInfoV2.builder()
                                                          .query(this.getQuery())
                                                          .indices(this.getIndex())
                                                          .hostnameField(this.getHostnameField())
                                                          .messageField(this.getMessageField())
                                                          .timestampField(this.getTimestampField())
                                                          .timestampFieldFormat(this.getTimestampFormat())
                                                          .queryType(this.getQueryType())
                                                          .build();
    fillDataCollectionInfoWithCommonFields(elkDataCollectionInfoV2);
    return elkDataCollectionInfoV2;
  }
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class ElkCVConfigurationYaml extends LogsCVConfigurationYaml {
    private String queryType;
    private String index;
    private String hostnameField;
    private String messageField;
    private String timestampField;
    private String timestampFormat;
  }
}
