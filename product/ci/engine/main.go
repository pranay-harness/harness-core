package main

/*
	CI lite engine executes steps of stage provided as an input.
*/
import (
	"fmt"
	"github.com/golang/protobuf/proto"
	"encoding/base64"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	pb2 "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/executor"
	"go.uber.org/zap"
)

const (
	applicationName = "CI-lite-engine"
	deployable      = "ci-lite-engine"
)

type schema struct {
	Input       string `arg:"--input, required" help:"base64 format of stage/step to execute"`
	LogPath     string `arg:"--logpath, required" help:"relative file path to store logs of steps"`
	TmpFilePath string `arg:"--tmppath, required" help:"relative file path to store temporary files"`
}

var args struct {
	Stage *schema `arg:"subcommand:stage"`
	Step  *schema `arg:"subcommand:step"`

	Verbose               bool   `arg:"--verbose" help:"enable verbose logging mode"`
	Deployment            string `arg:"env:DEPLOYMENT" help:"name of the deployment"`
	DeploymentEnvironment string `arg:"env:DEPLOYMENT_ENVIRONMENT" help:"environment of the deployment"`
}

func parseArgs() {
	// set defaults here
	args.DeploymentEnvironment = "prod"
	args.Verbose = false

	arg.MustParse(&args)
}

func init() {
	//TODO: perform any initialization
}

func generate(log *zap.SugaredLogger) string {

	// info0 := &pb2.UploadFile{
	// 	FilePattern: "/step/harness/omg.txt",
	// 	Destination: &pb2.Destination {
	// 		DestinationUrl: "https://harness.jfrog.io/artifactory/pcf",
	// 		Connector: &pb2.Connector {
	// 			Id: "J",
	// 			Auth: pb2.AuthType_BASIC_AUTH,
	// 		},
	// 		LocationType: pb2.LocationType_JFROG,
	// 	},
	// }

	// y := []*pb2.UploadFile{}
	// y = append(y, info0)




	info1 := &pb2.BuildPublishImage{
        DockerFile: "/step/harness/Dockerifle_1",
        Context: "/step/harness",
        Destination: &pb2.Destination {
            DestinationUrl: "us.gcr.io/ci-play/kaniko-build:v1.2",
            LocationType: pb2.LocationType_GCR,
            Connector: &pb2.Connector {
                Id: "G",
                Auth: pb2.AuthType_SECRET_FILE,
            },
        },
    }
    info2 := &pb2.BuildPublishImage{
        DockerFile: "/step/harness/Dockerifle_2",
        Context: "/step/harness",
        Destination: &pb2.Destination {
            DestinationUrl: "vistaarjuneja/xyz:0.1",
            LocationType: pb2.LocationType_DOCKERHUB,
            Connector: &pb2.Connector {
                Id: "D",
                Auth: pb2.AuthType_BASIC_AUTH,
            },
        },
    }
    // info3 := &pb2.BuildPublishImage{
    // 	DockerFile: "/step/harness/Dockerifle_1",
    //     Context: "/step/harness",
    //     Destination: &pb2.Destination {
    //         DestinationUrl: "448640225317.dkr.ecr.us-east-1.amazonaws.com/vistaarjuneja:v0.5",
    //         LocationType: pb2.LocationType_ECR,
    //         Connector: &pb2.Connector {
    //             Id: "E",
    //             Auth: pb2.AuthType_ACCESS_KEY,
    //         },
    //     },
    // }
    x := []*pb2.BuildPublishImage{}
    x = append(x, info2)
    x = append(x, info1)
    // x = append(x, info3)
    x = append(x, info2)
    // x = append(x, info3)
    x = append(x, info1)

    publishArtifactsStep := &pb.Step_PublishArtifacts{
		PublishArtifacts: &pb.PublishArtifactsStep{
			Images: x,
		},
	}


	step0 := &pb.Step{
        Id: "yay",
        DisplayName: "publishing_artifact",
        Step: publishArtifactsStep,
    }

 //    step1 := &pb.Step{
 //        Id: "yay",
 //        DisplayName: "building_publishing",
 //        Step: buildPublishStep,
 //    }

    fmt.Println(step0)

    data, err := proto.Marshal(step0)
    if err != nil {
        log.Fatalf("marshaling error: %v", err)
    }
    encoded := base64.StdEncoding.EncodeToString(data)

    return encoded
}

func main() {
	parseArgs()

	// build initial log
	log := logs.NewBuilder().Verbose(args.Verbose).WithDeployment(args.Deployment).
		WithFields("deployable", deployable,
			"application_name", applicationName).
		MustBuild().Sugar()

	log.Infow("CI lite engine is starting")
	s := &schema{Input: generate(log), LogPath: "/Users/vistaarjuneja/portal/log.txt"}
	args.Step = s
	switch {
	case args.Stage != nil:
		executor.ExecuteStage(args.Stage.Input, args.Stage.LogPath, args.Stage.TmpFilePath, log)
	case args.Step != nil:
		executor.ExecuteStep(args.Step.Input, args.Step.LogPath, args.Step.TmpFilePath, log)
	default:
		log.Fatalw(
			"One of stage or step needs to be specified",
			"args", args,
		)
	}
	log.Infow("CI lite engine completed execution, now exiting")
}
