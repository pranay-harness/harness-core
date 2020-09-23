package cistore

import (
	"fmt"
	ciclient "github.com/wings-software/portal/product/log-service/client/ci"
	"gopkg.in/alecthomas/kingpin.v2"
)

type downloadLinkCommand struct {
	accountID string
	orgID     string
	projectID string
	buildID   string
	stageID   string
	stepID    string

	server string
	token  string
}

func (c *downloadLinkCommand) run(*kingpin.ParseContext) error {
	client := ciclient.NewHTTPClient(c.server, c.token, false)
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", c.accountID, c.orgID, c.projectID, c.buildID, c.stageID, c.stepID)
	link, err := client.DownloadLink(nocontext, key)
	if err != nil {
		return err
	}
	println(link.Value)
	return nil
}

func registerDownloadLink(app *kingpin.CmdClause) {
	c := new(downloadLinkCommand)

	cmd := app.Command("download-link", "generate a download link").
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
