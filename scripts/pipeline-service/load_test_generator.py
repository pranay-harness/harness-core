from locust import HttpUser, task, between
import os



class QuickstartUser(HttpUser):
    wait_time = between(1, 2)

    @task(3)
    def run_pipeline(self):
        self.client.post("/api/pipeline/execute/{pipelineId}?accountIdentifier={accountId}&projectIdentifier={projectId}&orgIdentifier={orgId}".format(pipelineId=os.environ.get("PIPELINE_ID"), accountId=os.environ.get("ACCOUNT_ID"), orgId=os.environ.get("ORG_ID"), projectId=os.environ.get("PROJECT_ID")), {})
