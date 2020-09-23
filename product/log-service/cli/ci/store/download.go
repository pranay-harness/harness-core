package cistore

import (
	"fmt"
	"io"
	"os"

	ciclient "github.com/wings-software/portal/product/log-service/client/ci"
	"gopkg.in/alecthomas/kingpin.v2"
)

type downloadCommand struct {
	accountID string
	orgID     string
	projectID string
	buildID   string
	stageID   string
	stepID    string

	server string
	token  string
}

func (c *downloadCommand) run(*kingpin.ParseContext) error {
	cli := ciclient.NewHTTPClient(c.server, c.token, false)
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", c.accountID, c.orgID, c.projectID, c.buildID, c.stageID, c.stepID)
	out, err := cli.Download(nocontext, key)
	if out != nil {
		defer out.Close()
	}
	if err != nil {
		return err
	}
	_, err = io.Copy(os.Stdout, out)
	return err
}

func registerDownload(app *kingpin.CmdClause) {
	c := new(downloadCommand)

	cmd := app.Command("download", "download logs").
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
