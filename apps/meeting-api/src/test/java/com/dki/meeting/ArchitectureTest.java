package com.dki.meeting;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.*;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.dki.meeting", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest
  static final ArchRule coreDoesNotDependOnFrameworksOrAdapters =
      noClasses()
          .that()
          .resideInAnyPackage("..application..", "..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta..",
              "com.fasterxml..",
              "org.slf4j..",
              "java.sql..",
              "javax.sql..",
              "..infra..",
              "..config..",
              "..foundation.http..");

  @ArchTest
  static final ArchRule bookingCoreOwnsItsPorts =
      noClasses()
          .that()
          .resideInAnyPackage("..booking.application..", "..booking.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..identity..", "..room..");

  @ArchTest
  static final ArchRule roomCoreDoesNotKnowOtherFeatures =
      noClasses()
          .that()
          .resideInAnyPackage("..room.application..", "..room.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..identity..", "..booking..");

  @ArchTest
  static final ArchRule identityCoreDoesNotKnowOtherFeatures =
      noClasses()
          .that()
          .resideInAnyPackage("..identity.application..", "..identity.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..room..", "..booking..");

  @ArchTest
  static final ArchRule domainsDoNotDependOnApplications =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..application..");

  @ArchTest
  static final ArchRule controllersDoNotCallApplicationImplementations =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..application.service..", "..application.usecase..");

  @ArchTest
  static final ArchRule inputPortsDoNotKnowImplementations =
      noClasses()
          .that()
          .resideInAPackage("..application.port.in..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..application.service..", "..application.usecase..", "..application.port.out..");

  @ArchTest
  static final ArchRule useCasesExposeOneOperation =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("..application.usecase..")
          .and()
          .arePublic()
          .should()
          .haveName("execute");

  @ArchTest
  static final ArchRule modulesHaveNoCycles =
      slices().matching("com.dki.meeting.(*)..").should().beFreeOfCycles();
}
