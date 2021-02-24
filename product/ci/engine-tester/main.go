package main

import (
	"encoding/base64"
	"fmt"
	"log"

	"github.com/golang/protobuf/proto"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
)

var (
	callbackToken = "3CkwAIzX9jqgrkh8HhYJ6g"
	taskID        = "FP_i0fPwQb-dVeL2j41mQA"
	accountID     = "kmpySmUISimoRrJL6NL73w"
)

func getNewTIStep(id string, goals string, port uint32) *pb.UnitStep {
	ctx := &pb.StepContext{
		NumRetries: 2,
	}

	return &pb.UnitStep{
		Id:          id,
		DisplayName: fmt.Sprintf("step %s", id),
		Step: &pb.UnitStep_RunTests{
			RunTests: &pb.RunTestsStep{
				Context:              ctx,
				Args:                 "goals",
				BuildTool:            "maven",
				Language:             "java",
				ContainerPort:        port,
				TestAnnotations:      "test",
				Packages:             "packages",
				RunOnlySelectedTests: true,
				Reports:              nil,
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}
}

func pauseResumeTask() {
	//   fmt.Println("Aman -- test")
	// 	tiStep := getNewRunStep("step1", []string{`echo step1; ls; sleep 105`}, nil, uint32(8000))
	// step2 := getNewRunStep("step2", []string{`echo "step2; hello world"`}, nil, uint32(8001))
	// step3 := getNewRunStep("step3", []string{`echo "step3; hello world"`}, nil, uint32(8002))

	// func getNewTIStep(id string, goals string, port uint32) *pb.UnitStep {
	tiStep := getNewTIStep("tistep", "-Dnothing", uint32(8000))

	var steps []*pb.Step
	steps = append(steps,
		&pb.Step{Step: &pb.Step_Unit{Unit: tiStep}},
	)

	execution := &pb.Execution{
		Steps: steps,
	}
	data, err := proto.Marshal(execution)
	if err != nil {
		log.Fatalf("marshaling error: %v", err)
	}

	encoded := base64.StdEncoding.EncodeToString(data)
	fmt.Println(encoded)
}

func main() {
	pauseResumeTask()
	panic("error")
}
