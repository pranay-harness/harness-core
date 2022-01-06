/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.log;

import static software.wings.sm.states.ElkAnalysisState.DEFAULT_TIME_FIELD;

import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.elk.ElkDataCollectionInfoV2;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElkCVConfiguration extends LogsCVConfiguration {
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
  public boolean isCVTaskBasedCollectionEnabled() {
    return true;
  }

  @Override
  public CVConfiguration deepCopy() {
    ElkCVConfiguration clonedConfig = new ElkCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setQuery(this.getQuery());
    clonedConfig.setHostnameField(this.getHostnameField());
    clonedConfig.setIndex(this.getIndex());
    clonedConfig.setMessageField(this.getMessageField());
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
                                                          .queryType(ElkQueryType.TERM)
                                                          .build();
    fillDataCollectionInfoWithCommonFields(elkDataCollectionInfoV2);
    return elkDataCollectionInfoV2;
  }
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class ElkCVConfigurationYaml extends LogsCVConfigurationYaml {
    private String index;
    private String hostnameField;
    private String messageField;
    private String timestampField;
    private String timestampFormat;
  }
}
