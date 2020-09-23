package cistream

import (
	"fmt"
	ciclient "github.com/wings-software/portal/product/log-service/client/ci"
	"github.com/wings-software/portal/product/log-service/stream"

	"gopkg.in/alecthomas/kingpin.v2"
)

type pushCommand struct {
	accountID string
	orgID     string
	projectID string
	buildID   string
	stageID   string
	stepID    string
	line      *stream.Line

	server string
	token  string
}

func (c *pushCommand) run(*kingpin.ParseContext) error {
	client := ciclient.NewHTTPClient(c.server, c.token, false)
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", c.accountID, c.orgID, c.projectID, c.buildID, c.stageID, c.stepID)
	return client.Write(nocontext, key, []*stream.Line{c.line})
}

func registerPush(app *kingpin.CmdClause) {
	c := new(pushCommand)
	c.line = new(stream.Line)

	cmd := app.Command("push", "push log lines").
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

	cmd.Arg("message", "line message").
		StringVar(&c.line.Message)

	cmd.Flag("line", "line number").
		Default("0").
		IntVar(&c.line.Number)

	cmd.Flag("timestamp", "line timestamp").
		Int64Var(&c.line.Timestamp)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8080").
		StringVar(&c.server)

	cmd.Flag("token", "server token").
		StringVar(&c.token)
}
