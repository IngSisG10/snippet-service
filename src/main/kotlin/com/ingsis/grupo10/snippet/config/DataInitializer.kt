package com.ingsis.grupo10.snippet.config

import com.ingsis.grupo10.snippet.models.Language
import com.ingsis.grupo10.snippet.models.Tag
import com.ingsis.grupo10.snippet.repository.LanguageRepository
import com.ingsis.grupo10.snippet.repository.TagRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Profile("!test")
class DataInitializer(
    private val tagRepository: TagRepository,
    private val languageRepository: LanguageRepository,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        seedTags()
        seedLanguages()
    }

    private fun seedTags() {
        val tags =
            listOf(
                "validation", // Validación sintáctica al crear/actualizar
                "lint", // Linting de reglas de estilo
                "format", // Formateo de código
                "test_execution", // Ejecución de tests
                "snippet_execution", // Ejecución interactiva (futuro)
            )

        tags.forEach { tagName ->
            if (tagRepository.findByName(tagName) == null) {
                val tag =
                    Tag(
                        id = UUID.randomUUID(),
                        name = tagName,
                    )
                tagRepository.save(tag)
            }
        }
    }

    private fun seedLanguages() {
        val languages = listOf("PrintScript")

        languages.forEach { languageName ->
            if (languageRepository.findByName(languageName) == null) {
                val language =
                    Language(
                        id = UUID.randomUUID(),
                        name = languageName,
                    )
                languageRepository.save(language)
            }
        }
    }
}
