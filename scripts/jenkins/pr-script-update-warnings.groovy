import jenkins.model.Jenkins

import javax.xml.transform.stream.StreamSource
import java.util.regex.Matcher
import java.util.regex.Pattern

def updateJob(name, amount) {
    job = Jenkins.instance.getItemByFullName(name)
    config = job.getConfigFile().file.text

    def Pattern pattern = Pattern.compile("<threshold>(\\d+)</threshold>")
    Matcher matcher = pattern.matcher(config)
    matcher.find()

    current = Integer.parseInt(matcher.group(1))
    next = current - amount

    config = config.replace("<threshold>" + current + "</threshold>", "<threshold>" + next + "</threshold>")

    InputStream is = new ByteArrayInputStream(config.getBytes());
    job.updateByXml(new StreamSource(is));
    job.save();
}

updateJob("pr-portal-warnings", 2)
updateJob("pr-portal-checkstyle-to-be", 5)