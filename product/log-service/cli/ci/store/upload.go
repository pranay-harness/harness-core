package cistore

import (
	"fmt"
	"os"

	ciclient "github.com/wings-software/portal/product/log-service/client/ci"
	"gopkg.in/alecthomas/kingpin.v2"
)

type uploadCommand struct {
	accountID string
	orgID     string
	projectID string
	buildID   string
	stageID   string
	stepID    string
	path      string

	server string
	token  string
}

func (c *uploadCommand) run(*kingpin.ParseContext) error {
	f, err := os.Open(c.path)
	if err != nil {
		return err
	}
	defer f.Close()
	client := ciclient.NewHTTPClient(c.server, c.token, false)
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", c.accountID, c.orgID, c.projectID, c.buildID, c.stageID, c.stepID)
	return client.Upload(nocontext, key, f)
}

func registerUpload(app *kingpin.CmdClause) {
	c := new(uploadCommand)

	cmd := app.Command("upload", "upload logs").
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

	cmd.Arg("path", "file path").
		StringVar(&c.path)

	cmd.Flag("server", "server endpoint").
		Default("http://localhost:8080").
		StringVar(&c.server)

	cmd.Flag("token", "server token").
		StringVar(&c.token)
}
