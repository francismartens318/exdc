package customconnectornode.node

import com.exalate.api.domain.node.auth.ITrackerBasicAuthenticationData
import com.exalate.api.domain.node.auth.ITrackerNoAuthenticationData
import com.exalate.api.domain.node.auth.ITrackerOAuthAuthenticationData
import com.exalate.api.exception.UncathegorizedTrackableException // Typo in original class, should be UncategorizedTrackableException
import customconnectornode.notion.NotionApi
import customconnectornode.services.api.IIssueTrackerApi
import customconnectornode.services.api.auth.IIssueTrackerAuthenticationDataVisitor
import customconnectornode.services.api.factories.IIssueTrackerApiFactory
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.Application

import javax.annotation.Nonnull

class NotionApiFactory implements IIssueTrackerApiFactory {

    private static final Logger log = LoggerFactory.getLogger(NotionApiFactory.class)
    public static final String NOTION_API_KEY = "notionApiKey" // Key for storing API key in tracker properties

    @Override
    IIssueTrackerApi createApi(@NotNull @Nonnull Application application, @NotNull @Nonnull IIssueTrackerAuthenticationDataVisitor authenticationData) throws UncathegorizedTrackableException {
        log.debug("Creating NotionApi instance")

        // Extract API key from authentication data
        // This assumes the API key is stored as a property in the tracker configuration
        // TODO: Confirm how API key should be stored and retrieved
        String apiKey = authenticationData.getTrackerProperties().get(NOTION_API_KEY)

        if (apiKey == null || apiKey.isEmpty()) {
            // TODO: Handle missing API key more gracefully (e.g., throw specific exception)
            log.error("Notion API key is missing or empty")
            throw new UncathegorizedTrackableException("Notion API key is missing or empty")
        }

        return new NotionApi(application, apiKey)
    }

    @Override
    String getTrackerTypeName() {
        return "notion" // Or a more descriptive name like "Notion"
    }

    @Override
    Boolean isAuthenticationDataValid(@NotNull @Nonnull IIssueTrackerAuthenticationDataVisitor authenticationData) {
        // TODO: Implement proper validation logic if needed
        // For now, assume valid if API key is present
        String apiKey = authenticationData.getTrackerProperties().get(NOTION_API_KEY)
        return apiKey != null && !apiKey.isEmpty()
    }
}
