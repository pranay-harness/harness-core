package io.harness.delegate.beans.argo.response;

import io.harness.argo.beans.ManifestDiff;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;
import lombok.Data;

import java.util.List;

@Data
public class ManifestDiffResponse implements ArgoTaskResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private CommandExecutionStatus executionStatus;
  private String errorMessage;
  private List<ManifestDiff> manifestDiffList;

  ManifestDiffResponse(DelegateMetaInfo delegateMetaInfo, CommandExecutionStatus executionStatus, String errorMessage, List<ManifestDiff> manifestDiffList) {
    this.delegateMetaInfo = delegateMetaInfo;
    this.executionStatus = executionStatus;
    this.errorMessage = errorMessage;
    this.manifestDiffList = manifestDiffList;
  }

  public static ManifestDiffResponseBuilder builder() {
    return new ManifestDiffResponseBuilder();
  }

  public static class ManifestDiffResponseBuilder {
    private DelegateMetaInfo delegateMetaInfo;
    private CommandExecutionStatus executionStatus;
    private String errorMessage;
    private List<ManifestDiff> manifestDiffList;

    ManifestDiffResponseBuilder() {
    }

    public ManifestDiffResponseBuilder delegateMetaInfo(DelegateMetaInfo delegateMetaInfo) {
      this.delegateMetaInfo = delegateMetaInfo;
      return this;
    }

    public ManifestDiffResponseBuilder executionStatus(CommandExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
      return this;
    }

    public ManifestDiffResponseBuilder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public ManifestDiffResponseBuilder manifestDiffList(List<ManifestDiff> manifestDiffList) {
      this.manifestDiffList = manifestDiffList;
      return this;
    }

    public ManifestDiffResponse build() {
      return new ManifestDiffResponse(delegateMetaInfo, executionStatus, errorMessage, manifestDiffList);
    }

    public String toString() {
      return "ManifestDiffResponse.ManifestDiffResponseBuilder(delegateMetaInfo=" + this.delegateMetaInfo + ", executionStatus=" + this.executionStatus + ", errorMessage=" + this.errorMessage + ", manifestDiffList=" + this.manifestDiffList + ")";
    }
  }
}
