package io.harness.changehandlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.changestreamsframework.ChangeEvent;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CI)
@Slf4j
public class PlanExecutionSummaryChangeDataHandler extends AbstractChangeDataHandler {
  @Override
  public Map<String, String> getColumnValueMapping(ChangeEvent<?> changeEvent, String[] fields) {
    if (changeEvent == null) {
      return null;
    }
    Map<String, String> columnValueMapping = new HashMap<>();
    DBObject dbObject = changeEvent.getFullDocument();

    columnValueMapping.put("id", changeEvent.getUuid());

    if (dbObject == null) {
      return columnValueMapping;
    }

    if (dbObject.get("accountId") != null) {
      columnValueMapping.put("accountId", dbObject.get("accountId").toString());
    }
    if (dbObject.get("orgIdentifier") != null) {
      columnValueMapping.put("orgIdentifier", dbObject.get("orgIdentifier").toString());
    }
    if (dbObject.get("projectIdentifier") != null) {
      columnValueMapping.put("projectIdentifier", dbObject.get("projectIdentifier").toString());
    }
    if (dbObject.get("pipelineIdentifier") != null) {
      columnValueMapping.put("pipelineIdentifier", dbObject.get("pipelineIdentifier").toString());
    }
    if (dbObject.get("name") != null) {
      columnValueMapping.put("name", dbObject.get("name").toString());
    }
    if (dbObject.get("status") != null) {
      columnValueMapping.put("status", dbObject.get("status").toString());
    }

    // if moduleInfo is not null
    if (dbObject.get("moduleInfo") != null) {
      if (((BasicDBObject) dbObject.get("moduleInfo")).get("ci") != null) {
        columnValueMapping.put("moduleInfo_type", "CI");
        DBObject ciObject = (DBObject) (((BasicDBObject) dbObject.get("moduleInfo")).get("ci"));
        DBObject ciExecutionInfo = (DBObject) ciObject.get("ciExecutionInfoDTO");

        if (ciObject.get("repoName") != null) {
          columnValueMapping.put("moduleInfo_repository", ciObject.get("repoName").toString());
        }

        if (ciObject.get("branch") != null) {
          columnValueMapping.put("moduleinfo_branch_name", ciObject.get("branch").toString());
        }

        if (ciExecutionInfo != null) {
          DBObject branch = (DBObject) (ciExecutionInfo.get("branch"));

          HashMap firstCommit = null;
          String commits = "commits";
          if (branch != null && branch.get(commits) != null && ((List) branch.get(commits)).size() > 0) {
            firstCommit = (HashMap) ((List) branch.get(commits)).get(0);
            if (firstCommit != null) {
              if (firstCommit.get("id") != null) {
                columnValueMapping.put("moduleInfo_branch_commit_id", firstCommit.get("id").toString());
              }
              if (firstCommit.get("message") != null) {
                columnValueMapping.put("moduleInfo_branch_commit_message", firstCommit.get("message").toString());
              }
            }
          } else if (ciExecutionInfo.get("pullRequest") != null) {
            DBObject pullRequestObject = (DBObject) ciExecutionInfo.get("pullRequest");
            if (pullRequestObject.get(commits) != null && ((List) pullRequestObject.get(commits)).size() > 0) {
              firstCommit = (HashMap) ((List) pullRequestObject.get(commits)).get(0);
              if (firstCommit != null) {
                if (firstCommit.get("id") != null) {
                  columnValueMapping.put("moduleInfo_branch_commit_id", firstCommit.get("id").toString());
                }
                if (firstCommit.get("message") != null) {
                  columnValueMapping.put("moduleInfo_branch_commit_message", firstCommit.get("message").toString());
                }
              }
            }
          }
          DBObject author = (DBObject) (ciExecutionInfo.get("author"));
          if (author != null) {
            columnValueMapping.put("moduleInfo_author_id", author.get("id").toString());
            columnValueMapping.put("author_name", author.get("name").toString());
            columnValueMapping.put("author_avatar", author.get("avatar").toString());
          }
          if (ciExecutionInfo.get("event") != null) {
            columnValueMapping.put("moduleInfo_event", ciExecutionInfo.get("event").toString());
          }
        }
      } else {
        return null;
      }
    } else {
      // no information mention related to moduleInfo
      return null;
    }
    columnValueMapping.put("startTs", String.valueOf(Long.parseLong(dbObject.get("startTs").toString())));
    if (dbObject.get("endTs") != null) {
      columnValueMapping.put("endTs", String.valueOf(Long.parseLong(dbObject.get("endTs").toString())));
    }

    return columnValueMapping;
  }
}
