package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.dto.log.LintStatus
import com.ingsis.grupo10.snippet.dto.log.LogDto
import com.ingsis.grupo10.snippet.dto.log.TestExecutionResult
import com.ingsis.grupo10.snippet.dto.validation.LintResultDTO
import com.ingsis.grupo10.snippet.dto.validation.ValidationError
import com.ingsis.grupo10.snippet.models.Data
import com.ingsis.grupo10.snippet.models.Log
import com.ingsis.grupo10.snippet.models.Snippet
import com.ingsis.grupo10.snippet.models.Test
import com.ingsis.grupo10.snippet.repository.DataRepository
import com.ingsis.grupo10.snippet.repository.LogRepository
import com.ingsis.grupo10.snippet.repository.TagRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class LogService(
    private val logRepository: LogRepository,
    private val tagRepository: TagRepository,
    private val dataRepository: DataRepository,
) {
    // Resultado de la validación de un snippet
    fun logValidation(
        snippet: Snippet,
        errors: List<ValidationError>,
    ): Log {
        val tag =
            tagRepository.findByName("validation")
                ?: throw IllegalStateException("Tag 'validation' not found. Please seed the database.")

        val log =
            Log(
                id = UUID.randomUUID(),
                tag = tag,
                snippet = snippet,
                test = null,
                date = LocalDateTime.now(),
            )

        val savedLog = logRepository.save(log)

        val status = if (errors.isEmpty()) "valid" else "invalid"
        saveData(savedLog, "status", status)

        errors.forEachIndexed { index, error ->
            saveData(savedLog, "error_${index}_message", error.message)
            saveData(savedLog, "error_${index}_rule", error.rule)
            error.line?.let { saveData(savedLog, "error_${index}_line", it.toString()) }
            error.column?.let { saveData(savedLog, "error_${index}_column", it.toString()) }
        }

        return savedLog
    }

    // Resultado del linting de un snippet
    fun logLinting(
        snippet: Snippet,
        result: LintResultDTO,
    ): Log {
        val tag =
            tagRepository.findByName("lint")
                ?: throw IllegalStateException("Tag 'lint' not found. Please seed the database.")

        val log =
            Log(
                id = UUID.randomUUID(),
                tag = tag,
                snippet = snippet,
                test = null,
                date = LocalDateTime.now(),
            )

        val savedLog = logRepository.save(log)

        val status = if (result.errors.isEmpty()) "valid" else "invalid"
        saveData(savedLog, "status", status)

        result.errors.forEachIndexed { index, error ->
            saveData(savedLog, "error_${index}_message", error.message)
            saveData(savedLog, "error_${index}_type", error.type)
            error.segment?.let { saveData(savedLog, "error_${index}_segment", it.toString()) }
        }

        return savedLog
    }

    // Resultado del formateo de un snippet
    fun logFormatting(
        snippet: Snippet,
        formattedCode: String,
        rulesApplied: String,
    ): Log {
        val tag =
            tagRepository.findByName("format")
                ?: throw IllegalStateException("Tag 'format' not found. Please seed the database.")

        val log =
            Log(
                id = UUID.randomUUID(),
                tag = tag,
                snippet = snippet,
                test = null,
                date = LocalDateTime.now(),
            )

        val savedLog = logRepository.save(log)

        saveData(savedLog, "formatted_code", formattedCode)
        saveData(savedLog, "rules_applied", rulesApplied)
        saveData(savedLog, "status", "success")

        return savedLog
    }

    // Resultado de la ejecución de un test
    fun logTestExecution(
        test: Test,
        actualOutput: String,
        expectedOutput: String,
        passed: Boolean,
        durationMs: Long? = null,
    ): Log {
        val tag =
            tagRepository.findByName("test_execution")
                ?: throw IllegalStateException("Tag 'test_execution' not found. Please seed the database.")

        val log =
            Log(
                id = UUID.randomUUID(),
                tag = tag,
                snippet = test.snippet,
                test = test,
                date = LocalDateTime.now(),
            )

        val savedLog = logRepository.save(log)

        saveData(savedLog, "status", if (passed) "passed" else "failed")
        saveData(savedLog, "actual_output", actualOutput)
        saveData(savedLog, "expected_output", expectedOutput)
        durationMs?.let { saveData(savedLog, "duration_ms", it.toString()) }

        return savedLog
    }

    // Resultado de la ejecución interactiva de un snippet
    fun logSnippetExecution(
        snippet: Snippet,
        output: String,
        inputs: String,
        status: String,
    ): Log {
        val tag =
            tagRepository.findByName("snippet_execution")
                ?: throw IllegalStateException("Tag 'snippet_execution' not found. Please seed the database.")

        val log =
            Log(
                id = UUID.randomUUID(),
                tag = tag,
                snippet = snippet,
                test = null,
                date = LocalDateTime.now(),
            )

        val savedLog = logRepository.save(log)

        saveData(savedLog, "output", output)
        saveData(savedLog, "inputs", inputs)
        saveData(savedLog, "status", status)

        return savedLog
    }

    // Traer el último estado de linting de un snippet
    fun getLatestLintStatus(snippetId: UUID): LintStatus {
        val latestLog =
            logRepository.findFirstBySnippetIdAndTagNameOrderByDateDesc(snippetId, "lint")

        if (latestLog == null) {
            return LintStatus(
                snippetId = snippetId,
                status = "pending",
                lastLintDate = null,
                errors = emptyList(),
            )
        }

        val dataEntries = dataRepository.findByLogId(latestLog.id)
        val dataMap = dataEntries.associate { it.name to it.data }

        val status = dataMap["status"] ?: "pending"
        val errors = parseErrorsFromData(dataMap)

        return LintStatus(
            snippetId = snippetId,
            status = status,
            lastLintDate = latestLog.date,
            errors = errors,
        )
    }

    // Traer la versión formateada de un snippet
    fun getFormattedVersion(snippetId: UUID): String? {
        val latestLog =
            logRepository.findFirstBySnippetIdAndTagNameOrderByDateDesc(snippetId, "format")
                ?: return null

        val dataEntries = dataRepository.findByLogId(latestLog.id)
        return dataEntries.find { it.name == "formatted_code" }?.data
    }

    // Traer el historial de ejecuciones de tests para un test específico
    fun getTestExecutionHistory(testId: UUID): List<TestExecutionResult> {
        val logs = logRepository.findByTestId(testId)

        return logs.map { log ->
            val dataEntries = dataRepository.findByLogId(log.id)
            val dataMap = dataEntries.associate { it.name to it.data }

            TestExecutionResult(
                logId = log.id,
                testId = testId,
                status = dataMap["status"] ?: "unknown",
                actualOutput = dataMap["actual_output"] ?: "",
                expectedOutput = dataMap["expected_output"] ?: "",
                durationMs = dataMap["duration_ms"]?.toLongOrNull(),
                executedAt = log.date,
            )
        }
    }

    // Traer todos los logs de un snippet, opcionalmente filtrados por tag
    fun getSnippetLogs(
        snippetId: UUID,
        tagName: String? = null,
    ): List<LogDto> {
        val logs =
            if (tagName != null) {
                logRepository.findBySnippetIdAndTagName(snippetId, tagName)
            } else {
                logRepository.findBySnippetIdOrderByDateDesc(snippetId)
            }

        return logs.map { log ->
            val dataEntries = dataRepository.findByLogId(log.id)
            val dataMap = dataEntries.associate { it.name to it.data }

            LogDto(
                id = log.id,
                tagName = log.tag.name,
                snippetId = log.snippet?.id,
                testId = log.test?.id,
                date = log.date,
                data = dataMap,
            )
        }
    }

    // Genera un entry de Data
    private fun saveData(
        log: Log,
        name: String,
        value: String,
    ) {
        val data =
            Data(
                id = UUID.randomUUID(),
                log = log,
                name = name,
                data = value,
            )
        dataRepository.save(data)
    }

    // Valida y parsea los errores desde el mapa de datos
    private fun parseErrorsFromData(dataMap: Map<String, String>): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        var index = 0

        while (dataMap.containsKey("error_${index}_message")) {
            val error =
                ValidationError(
                    message = dataMap["error_${index}_message"] ?: "",
                    rule = dataMap["error_${index}_rule"] ?: "unknown",
                    line = dataMap["error_${index}_line"]?.toIntOrNull(),
                    column = dataMap["error_${index}_column"]?.toIntOrNull(),
                )
            errors.add(error)
            index++
        }

        return errors
    }

    fun getSnippetIdsByCompliance(snippetIds: List<UUID>, compliance: String): List<UUID> {
        if (compliance == "all") {
            return snippetIds
        }

        return snippetIds.filter { snippetId ->
            val lintStatus = getLatestLintStatus(snippetId)
            lintStatus.status == compliance
        }
    }
}
