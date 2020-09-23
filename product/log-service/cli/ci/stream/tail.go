package cistream

import (
	"context"
	"fmt"
	"os"
	"os/signal"

	ciclient "github.com/wings-software/portal/product/log-service/client/ci"

	"gopkg.in/alecthomas/kingpin.v2"
)

type tailCommand struct {
	accountID string
	orgID     string
	projectID string
	buildID   string
	stageID   string
	stepID    string

	server string
	token  string
}

func (c *tailCommand) run(*kingpin.ParseContext) error {
	ctx := context.Background()
	ctx, cancel := context.WithCancel(ctx)
	s := make(chan os.Signal, 1)
	signal.Notify(s, os.Interrupt)
	defer func() {
		println("")
		println("closing stream ...")
		signal.Stop(s)
		cancel()
	}()
	go func() {
		select {
		case <-s:
			cancel()
		case <-ctx.Done():
		}
	}()

	client := ciclient.NewHTTPClient(c.server, c.token, false)
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", c.accountID, c.orgID, c.projectID, c.buildID, c.stageID, c.stepID)
	linec, errc := client.Tail(ctx, key)
	fmt.Println(linec)
	fmt.Println(errc)
	for {
		fmt.Println("here .. ")
		select {
		case <-ctx.Done():
			return nil
		case err := <-errc:
			fmt.Println(err)
			if err != context.Canceled {
				return nil
			}
		case line := <-linec:
			fmt.Println(line.Message)
		}
	}
}

func registerTail(app *kingpin.CmdClause) {
	c := new(tailCommand)

	cmd := app.Command("tail", "tail the log stream").
		Action(c.run)

	cmd.Arg("accountID", "project identifier").
		Required().
		StringVar(&c.accountID)

	cmd.Arg("orgID", "org identifier").
		Required().
		StringVar(&c.orgID)

	cmd.Arg("projectID", "project identifier").
		Required().
		StringVar(&c.projectID)

	cmd.Arg("buildID", "build identifier").
		Required().
		StringVar(&c.buildID)

	cmd.Arg("stageID", "stage identifier").
		Required().
		StringVar(&c.stageID)

	cmd.Arg("stepID", "step identifier").
		Required().
		StringVar(&c.stepID)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8080").
		StringVar(&c.server)

	cmd.Flag("token", "server token").
		StringVar(&c.token)
}
