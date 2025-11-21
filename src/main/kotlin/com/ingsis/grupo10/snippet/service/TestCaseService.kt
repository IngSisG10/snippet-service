package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.models.Test
import com.ingsis.grupo10.snippet.repository.TestRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TestCaseService(
    private val testCaseRepository: TestRepository,
) {
    fun getTestCases(): List<Test> = testCaseRepository.findAll()

    fun postTestCase(testCase: Test): Test = testCaseRepository.save(testCase)

    fun removeTestCase(id: UUID) = testCaseRepository.deleteById(id)

    // fun testSnippet(testCase: Test)
}
