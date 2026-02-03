package com.cinelux.screening.acceptance

import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/screening")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty, html:target/cucumber-reports/screening-acceptance.html")
@ConfigurationParameter(key = "cucumber.glue", value = "com.cinelux.screening.acceptance.steps")
@ConfigurationParameter(key = "cucumber.filter.tags", value = "not @wip")
class CucumberScreeningAcceptanceTest
