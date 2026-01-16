package com.cinelux.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Hexagonal Architecture Rules")
class HexagonalArchitectureTest {

    private lateinit var classes: JavaClasses

    @BeforeAll
    fun setUp() {
        classes = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.cinelux")
    }

    @Nested
    @DisplayName("Layer Dependencies")
    inner class LayerDependencies {

        @Test
        @DisplayName("domain should not depend on application layer")
        fun `domain should not depend on application`() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .because("Domain layer must not depend on Application layer (dependency inversion)")
                .check(classes)
        }

        @Test
        @DisplayName("domain should not depend on infrastructure layer")
        fun `domain should not depend on infrastructure`() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("Domain layer must not depend on Infrastructure layer")
                .check(classes)
        }

        @Test
        @DisplayName("application should not depend on infrastructure layer")
        fun `application should not depend on infrastructure`() {
            noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("Application layer must not depend on Infrastructure layer")
                .check(classes)
        }
    }

    @Nested
    @DisplayName("Domain Layer Purity")
    inner class DomainLayerPurity {

        @Test
        @DisplayName("domain should not use Spring Framework")
        fun `domain should not use Spring`() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Domain layer must be framework-agnostic")
                .check(classes)
        }

        @Test
        @DisplayName("domain should not use JPA/Jakarta Persistence")
        fun `domain should not use JPA`() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "javax.persistence.."
                )
                .because("Domain layer must not have persistence framework dependencies")
                .check(classes)
        }

        @Test
        @DisplayName("domain should not use Jackson")
        fun `domain should not use Jackson`() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson..")
                .because("Domain layer must not have serialization framework dependencies")
                .check(classes)
        }

        @Test
        @DisplayName("domain should not use Jakarta/Javax validation")
        fun `domain should not use validation annotations`() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.validation..",
                    "javax.validation.."
                )
                .because("Domain layer should use Kotlin require/check for validation")
                .check(classes)
        }
    }

    @Nested
    @DisplayName("Port Layer Rules")
    inner class PortLayerRules {

        @Test
        @DisplayName("API ports should not use Spring annotations")
        fun `api ports should not use Spring`() {
            noClasses()
                .that().resideInAPackage("..application.port.api..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("API ports (driving) must be framework-agnostic interfaces")
                .check(classes)
        }

        @Test
        @DisplayName("SPI ports should not use Spring annotations")
        fun `spi ports should not use Spring`() {
            noClasses()
                .that().resideInAPackage("..application.port.spi..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("SPI ports (driven) must be framework-agnostic interfaces")
                .check(classes)
        }

        @Test
        @DisplayName("ports should only depend on domain")
        fun `ports should only depend on domain`() {
            noClasses()
                .that().resideInAPackage("..application.port..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("Ports define contracts using domain types only")
                .check(classes)
        }
    }

    @Nested
    @DisplayName("Use Case Rules")
    inner class UseCaseRules {

        @Test
        @DisplayName("use cases should not use Spring annotations")
        fun `use cases should not use Spring`() {
            noClasses()
                .that().resideInAPackage("..application.usecase..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("Use cases must be framework-agnostic, wired via infrastructure config")
                .check(classes)
        }

        @Test
        @DisplayName("use case implementations should depend on API ports")
        fun `use case implementations should depend on api ports`() {
            classes()
                .that().resideInAPackage("..application.usecase..")
                .and().haveSimpleNameEndingWith("UseCaseImpl")
                .should().dependOnClassesThat().resideInAPackage("..application.port.api..")
                .because("Use case implementations must implement their corresponding API port")
                .check(classes)
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    inner class NamingConventions {

        @Test
        @DisplayName("use case interfaces should end with 'UseCase'")
        fun `api port interfaces should end with UseCase`() {
            classes()
                .that().resideInAPackage("..application.port.api..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("UseCase")
                .orShould().haveSimpleNameEndingWith("Result")
                .orShould().haveSimpleNameEndingWith("Command")
                .orShould().haveSimpleNameEndingWith("Query")
                .because("API ports should follow naming convention: *UseCase, *Command, *Query, *Result")
                .check(classes)
        }

        @Test
        @DisplayName("SPI port interfaces should end with 'Repository' or 'Gateway'")
        fun `spi port interfaces should follow naming convention`() {
            classes()
                .that().resideInAPackage("..application.port.spi..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Repository")
                .orShould().haveSimpleNameEndingWith("Gateway")
                .orShould().haveSimpleNameEndingWith("Client")
                .orShould().haveSimpleNameEndingWith("Publisher")
                .because("SPI ports should follow naming convention: *Repository, *Gateway, *Client, *Publisher")
                .check(classes)
        }
    }
}
