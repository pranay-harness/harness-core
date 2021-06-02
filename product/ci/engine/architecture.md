# Architecture

This describes the high level architecture of CI lite engine.

# New Lite engine design

CI lite engine starts the grpc server at startup time and waits for ExecuteStep grpc call to execute a step. Workflow execution engine drives the execution of CI stage. WF engine execute the steps by calling ExecuteStep grpc on lite-engine via Delegate agent.
ExecuteStep grpc is an asynchronous api. It starts execution of the step in a separate goroutine and returns immediately after the goroutine has started. It also ensures that multiple calls to ExecuteStep with same id doesn't trigger the job multiple times but only once. After step execution is complete, it updates the status of step via delegate agent.
