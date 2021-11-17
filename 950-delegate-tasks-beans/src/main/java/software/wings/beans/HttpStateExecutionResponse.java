package software.wings.beans;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class HttpStateExecutionResponse implements DelegateTaskNotifyResponseData {
	private DelegateMetaInfo delegateMetaInfo;
	private ExecutionStatus executionStatus;
	private String errorMessage;
	private String httpResponseBody;
	private int httpResponseCode;
	private String httpMethod;
	private String httpUrl;
	private String header;
	private boolean timedOut;
}
