package io.harness.ng.cdOverview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BranchInfo {
    private String branch;
    private String commit;
    private String commitID;
    private String repoName;
    private AuthorInfo author;
}
