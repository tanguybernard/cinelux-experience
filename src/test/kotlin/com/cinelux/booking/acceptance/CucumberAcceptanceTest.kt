package com.cinelux.booking.acceptance

import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/booking")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty, html:target/cucumber-reports/acceptance.html")
@ConfigurationParameter(key = "cucumber.glue", value = "com.cinelux.booking.acceptance.steps")
@ConfigurationParameter(key = "cucumber.filter.tags", value = "not @wip")
class CucumberAcceptanceTest
