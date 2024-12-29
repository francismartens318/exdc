package customconnectornode.discourse

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.exalate.api.domain.IIssueKey
import com.exalate.basic.domain.BasicIssueKey
import com.exalate.replication.services.issuetracker.GroovyHttpClient
import customconnectornode.domain.PageRequest
import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory
import play.api.Application
import play.api.inject.Injector
import spock.lang.Specification
import spock.lang.Subject

class DiscourseApiSpec extends Specification {

    @Subject
    DiscourseApi discourseApi

    Application application
    GroovyHttpClient httpClient
    Injector injector


    static {
        // Set root logger to ERROR level
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.setLevel(Level.ERROR)

        // Enable DEBUG level for customconnectornode package
        Logger appLogger = (Logger) LoggerFactory.getLogger("customconnectornode")
        appLogger.setLevel(Level.DEBUG)
    }

    def setupSpec() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./ccnode")
                .load()

        System.setProperty("TRACKER_URL", dotenv.get("TRACKER_URL"))
        System.setProperty("TRACKER_API_KEY", dotenv.get("TRACKER_API_KEY"))
        System.setProperty("TRACKER_USER", dotenv.get("TRACKER_USER"))
        System.setProperty("TRACKER_PASSWORD", dotenv.get("TRACKER_PASSWORD"))
    }

    def setup() {
        application = Mock(Application)
        injector = Mock(Injector)

        // Use interface mocking with as()
        httpClient = Mock (GroovyHttpClient) {
            setBasicAuth(_, _) >> null
        } as GroovyHttpClient

        application.injector() >> injector
        injector.instanceOf(GroovyHttpClient) >> httpClient

        discourseApi = new DiscourseApi(application)
    }

    def "searchEntityTypes returns correct entity types"() {
        given:
        def query = "test"
        def pageRequest = new PageRequest(0, 10)

        when:
        def result = discourseApi.searchEntityTypes(query, pageRequest)

        then:
        result.results.size() == 1
        result.results[0].name == "topic"

    }

    def "readEntity returns hub issue for valid topic with a number of comments"() {
        given:
        def entityKey = new BasicIssueKey("7", "7", "topic")


        when:
        def result = discourseApi.readEntity(entityKey)

        then:

        result != null
        result.key == "7"
        result.summary == "Test Topic to check the test cases"
        result.description == "<p>This topic is to validate the test case</p>"
        result.created.toString() == "Mon Dec 23 12:28:09 CET 2024"


        result.customFields.size() == 1

        // check category custom field
        result.customFields["category"].value?.name == "Category"
        result.customFields["category"].value.id == "1"
        result.customFields["category"].value.uid == "4"


    }

    def "doesEntityExist returns correct boolean"() {
        given:
        def entityKey = new BasicIssueKey("123", "123", "topic")

        when:
        def result = discourseApi.doesEntityExist(entityKey)

        then:
        !result // Since readEntity returns null in our implementation
    }

    def "writeEntity returns EntityWriteResult with provided entity"() {
        given:
        def entityKey = Mock(IIssueKey)
        def entityBeforeScript = Mock(IHubIssueReplica)
        def entityAfterScript = Mock(IHubIssueReplica)
        def traces = []
        def blobMetadataList = []

        when:
        def result = discourseApi.writeEntity(entityKey, entityBeforeScript, entityAfterScript, traces, blobMetadataList)

        then:
        result != null
        result.entity == entityAfterScript
        result.traces == traces
    }

    def "search returns empty page response"() {
        given:
        def query = "test"
        def since = new Date()
        def entityKeyContext = Mock(EntityKeyContext)
        def pageRequest = new PageRequest(0, 10)

        when:
        def result = discourseApi.search(query, since, entityKeyContext, pageRequest)

        then:
        result != null
        result.items.isEmpty()
        result.hasMore
    }

    def "deleteEntity throws IssueTrackerException"() {
        given:
        def entityKey = Mock(IIssueKey)

        when:
        discourseApi.deleteEntity(entityKey)

        then:
        thrown(IssueTrackerException)
    }

    def "uploadFile throws IssueTrackerException"() {
        when:
        discourseApi.uploadFile("test.txt", Mock(Source), null, null)

        then:
        thrown(IssueTrackerException)
    }
}