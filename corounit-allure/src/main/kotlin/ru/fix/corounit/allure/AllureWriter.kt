package ru.fix.corounit.allure

import io.qameta.allure.AllureResultsWriter
import io.qameta.allure.FileSystemResultsWriter
import io.qameta.allure.model.TestResult
import io.qameta.allure.model.TestResultContainer
import io.qameta.allure.util.PropertiesUtils
import java.io.InputStream
import java.nio.file.Paths

object AllureWriter: AllureResultsWriter{
    private val writer: AllureResultsWriter
    init {
        val properties = PropertiesUtils.loadAllureProperties()
        val path = properties.getProperty("allure.results.directory", "allure-results")
        writer = FileSystemResultsWriter(Paths.get(path))
    }

    override fun write(testResult: TestResult?) {
        writer.write(testResult)
    }

    override fun write(testResultContainer: TestResultContainer?) {
        writer.write(testResultContainer)
    }

    override fun write(source: String?, attachment: InputStream?) {
        writer.write(source, attachment)
    }
}