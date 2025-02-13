package customconnectornode.node

import customconnectornode.discourse.DiscourseApi
import customconnectornode.services.api.IIssueTrackerApi
import customconnectornode.services.node.spi.IIssueTrackerApiFactory
import play.api.Application

class DiscourseApiFactory implements IIssueTrackerApiFactory {
    @Override
    IIssueTrackerApi create(Application application) {
        return new DiscourseApi(application)
    }
}
