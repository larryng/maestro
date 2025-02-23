package maestro.cli.report

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import maestro.cli.model.FlowStatus
import maestro.cli.model.TestExecutionSummary
import okio.Sink
import okio.buffer

class JUnitTestSuiteReporter(
    private val mapper: ObjectMapper,
    private val testSuiteName: String?
) : TestSuiteReporter {

    override fun report(
        summary: TestExecutionSummary,
        out: Sink
    ) {
        mapper
            .writerWithDefaultPrettyPrinter()
            .writeValue(
                out.buffer().outputStream(),
                TestSuites(
                    suites = summary
                        .suites
                        .map { suite ->
                            TestSuite(
                                name = testSuiteName ?: "Test Suite",
                                device = summary.deviceName,
                                failures = suite.flows.count { it.status == FlowStatus.ERROR },
                                tests = suite.flows.size,
                                testCases = suite.flows
                                    .map { flow ->
                                        TestCase(
                                            id = flow.fileName ?: flow.name,
                                            name = flow.name,
                                            classname = flow.fileName ?: flow.name,
                                            failure = flow.failure?.let { failure ->
                                                Failure(
                                                    message = failure.message,
                                                )
                                            }
                                        )
                                    }
                            )
                        }
                )
            )
    }

    @JacksonXmlRootElement(localName = "testsuites")
    private data class TestSuites(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("testsuite")
        val suites: List<TestSuite>,
    )

    @JacksonXmlRootElement(localName = "testsuite")
    private data class TestSuite(
        @JacksonXmlProperty(isAttribute = true) val name: String,
        @JacksonXmlProperty(isAttribute = true) val device: String?,
        @JacksonXmlProperty(isAttribute = true) val tests: Int,
        @JacksonXmlProperty(isAttribute = true) val failures: Int,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("testcase")
        val testCases: List<TestCase>,
    )

    private data class TestCase(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String,
        @JacksonXmlProperty(isAttribute = true) val classname: String,
        val failure: Failure? = null,
    )

    private data class Failure(
        @JacksonXmlText val message: String,
    )

    companion object {

        fun xml(testSuiteName: String? = null) = JUnitTestSuiteReporter(
            mapper = XmlMapper().apply {
                registerModule(KotlinModule.Builder().build())
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
                configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
            },
            testSuiteName = testSuiteName
        )

    }

}
