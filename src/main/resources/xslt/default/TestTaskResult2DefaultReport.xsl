<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:etf="http://www.interactive-instruments.de/etf/2.0" xmlns:x="http://www.interactive-instruments.de/etf/2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" exclude-result-prefixes="x xs etf xsi" version="2.0">
	<xsl:import href="jsAndCss.xsl"/>
	<xsl:import href="UtilityTemplates.xsl"/>
	<xsl:param name="language">en</xsl:param>
	<xsl:param name="serviceUrl" select="'https://localhost/etf'"/>
	<xsl:output method="html" doctype-system="about:legacy-compat" indent="yes" encoding="UTF-8"/>
	<!-- Translation TODO -->
	<xsl:key name="translation" match="x:lang/x:e" use="@key"/>
	<xsl:variable name="lang" select="document('ui-text.xml')/*/x:lang[@xml:lang=$language]"/>
	<xsl:template match="x:lang">
		<xsl:param name="str"/>
		<xsl:variable name="result" select="key('translation', $str)"/>
		<xsl:choose>
			<xsl:when test="$result">
				<xsl:value-of select="$result"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$str"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- Create lookup tables for faster id lookups -->
	<xsl:key name="testSuiteKey" match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite" use="@id"/>
	<xsl:key name="testModuleKey" match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite/etf:testModules[1]/etf:TestModule" use="@id"/>
	<xsl:key name="testCaseKey" match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite/etf:testModules[1]/etf:TestModule/etf:testCases[1]/etf:TestCase" use="@id"/>
	<xsl:key name="testStepKey" match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite/etf:testModules[1]/etf:TestModule/etf:testCases[1]/etf:TestCase/etf:testSteps[1]/etf:TestStep" use="@id"/>
	<xsl:key name="testAssertionKey" match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite/etf:testModules[1]/etf:TestModule/etf:testCases[1]/etf:TestCase/etf:testSteps[1]/etf:TestStep/etf:testAssertions[1]/etf:TestAssertion" use="@id"/>
	<xsl:key name="testItemTypeKey" match="/etf:DsResultSet/etf:testItemTypes[1]/etf:TestItemType" use="@id"/>
	<xsl:key name="testObjectKey" match="/etf:DsResultSet/etf:testObjects[1]/etf:TestObject" use="@id"/>
	<xsl:key name="testObjectTypeKey" match="/etf:DsResultSet/etf:testObjectTypes[1]/etf:TestObjectType" use="@id"/>
	<xsl:key name="translationKey" match="/etf:DsResultSet/etf:translationTemplateBundles[1]/etf:TranslationTemplateBundle/etf:translationTemplateCollections[1]/etf:LangTranslationTemplateCollection/etf:translationTemplates[1]/etf:TranslationTemplate[@language = $language]" use="@name"/>
	<xsl:key name="testTaskKey" match="/etf:DsResultSet/etf:testRuns[1]/etf:TestRun/etf:testTasks[1]/etf:TestTask" use="@id"/>
	<xsl:key name="testTaskResultKey" match="/etf:DsResultSet/etf:testTaskResults[1]/etf:TestTaskResult" use="@id"/>
	<xsl:key name="attachmentsKey" match="/etf:DsResultSet/etf:testTaskResults[1]/etf:TestTaskResult/etf:attachments[1]/etf:Attachment" use="@id"/>
	<xsl:variable name="testTaskResult" select="/etf:DsResultSet/etf:testTaskResults[1]/etf:TestTaskResult[1]"/>
	<xsl:variable name="ets" select="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite[1]"/>
	<xsl:variable name="testTask" select="key('testTaskKey', $testTaskResult/etf:parent[1]/@ref)"/>
	<xsl:variable name="etsId" select="key('testSuiteKey', $testTask/etf:resultedFrom[1]/@ref)"/>
	<xsl:variable name="testObject" select="key('testObjectKey', $testTask/etf:testObject[1]/@ref)"/>
	<xsl:variable name="statisticAttachment" select="$testTaskResult/etf:attachments[1]/etf:Attachment[@type = 'StatisticalReport']"/>
	<!-- Test Report -->
	<!-- ########################################################################################## -->
	<xsl:template match="/etf:DsResultSet">
		<html>
			<head>
				<meta http-equiv="X-UA-Compatible" content="IE=Edge"/>
				<title>
					<xsl:value-of select="$lang/x:e[@key = 'Title']"/>
				</title>
				<!-- Include Styles and Javascript functions -->
				<xsl:call-template name="jsfdeclAndCss"/>
			</head>
			<body>
				<div data-role="header">
					<h1><xsl:value-of select="./etf:testRuns[1]/etf:TestRun[1]/etf:label/text()"/><br/><xsl:value-of select="./etf:executableTestSuites[1]/etf:ExecutableTestSuite[1]/etf:label/text()"/></h1>
					<a href="{$serviceUrl}/results" data-ajax="false" data-icon="back" data-iconpos="notext"/>
				</div>
				<div data-role="content">
					<div class="ui-grid-b">
						<div class="ui-block-a">
							<xsl:call-template name="reportInfo"/>
						</div>
						<div class="ui-block-b">
							<xsl:call-template name="statistics"/>
						</div>
						<div class="ui-block-c">
							<xsl:call-template name="controls"/>
						</div>
					</div>
					<xsl:apply-templates select="$testTask/etf:ArgumentList[1]"/>
					<!-- Test object -->
					<xsl:apply-templates select="$testObject"/>
					<!-- Additional statistics provided by the test project -->
					<xsl:apply-templates select="$statisticAttachment"/>
					<!-- Test Suite Results -->
					<xsl:apply-templates select="$testTaskResult"/>
				</div>
				<xsl:call-template name="footer"/>
			</body>
		</html>
	</xsl:template>
	<!-- General report information-->
	<!-- ########################################################################################## -->
	<xsl:template name="reportInfo">
		<div id="rprtInfo">
			<table>
				<tr class="ReportDetail">
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'PublicationLocation']"/>
					</td>
					<td>
						<a href="{$serviceUrl}/TestTaskResult/{$testTaskResult/@id}.html?lang={$language}" data-ajax="false">
							<xsl:value-of select="$lang/x:e[@key = 'PublicationLocationLink']"/>
						</a>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'Status']"/>
					</td>
					<td>
						<xsl:value-of select="$lang/x:e[@key = $testTaskResult/etf:status]"/>
					</td>
				</tr>
				<tr class="DoNotShowInSimpleView">
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'Started']"/>
					</td>
					<td>
						<xsl:call-template name="formatDate">
							<xsl:with-param name="DateTime" select="$testTaskResult/etf:startTimestamp"/>
						</xsl:call-template>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
					</td>
					<td>
						<xsl:call-template name="formatDuration">
							<xsl:with-param name="ms" select="$testTaskResult/etf:duration[1]/text()"/>
						</xsl:call-template>
					</td>
				</tr>
			</table>
		</div>
	</xsl:template>
	<!-- Short Statistics -->
	<!-- ########################################################################################## -->
	<xsl:template name="statistics">
		<div id="rprtStatistics">
			<table id="my-table">
				<thead>
					<tr>
						<th/>
						<th>
							<xsl:value-of select="$lang/x:e[@key = 'Count']"/>
						</th>
						<th>
							<xsl:value-of select="$lang/x:e[@key = 'Skipped']"/>
						</th>
						<th>
							<xsl:value-of select="$lang/x:e[@key = 'Failed']"/>
						</th>
						<th>
							<xsl:value-of select="$lang/x:e[@key = 'Warning']"/>
						</th>
						<th>
							<xsl:value-of select="$lang/x:e[@key = 'Manual']"/>
						</th>
					</tr>
				</thead>
				<tbody>
					<!-- TEST SUITE STATS-->
					<xsl:if test="$ets[etf:label ne 'IGNORE']">
						<xsl:variable name="results" select="$testTaskResult"/>
						<tr>
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'TestSuites']"/>
							</td>
							<td>
								<xsl:value-of select="count($results)"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='SKIPPED'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='FAILED'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='WARNING'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='PASSED_MANUAL'])"/>
							</td>
						</tr>
					</xsl:if>
					<!-- TEST MODULE STATS-->
					<xsl:if test="$ets/etf:testModules[1]/etf:TestModule[etf:label ne 'IGNORE']">
						<xsl:variable name="results" select="$testTaskResult/etf:testModuleResults[1]/etf:TestModuleResult"/>
						<tr>
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'TestModules']"/>
							</td>
							<td>
								<xsl:value-of select="count($results)"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='SKIPPED'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='FAILED'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='WARNING'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='PASSED_MANUAL'])"/>
							</td>
						</tr>
					</xsl:if>
					<!-- TEST CASES STATS-->
					<xsl:if test="$ets/etf:testModules[1]/etf:TestModule/etf:testCases[1]/etf:TestCase[etf:label ne 'IGNORE']">
						<xsl:variable name="results" select="$testTaskResult/etf:testModuleResults[1]/etf:TestModuleResult/etf:testCaseResults[1]/etf:TestCaseResult"/>
						<tr>
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'TestCases']"/>
							</td>
							<td>
								<xsl:value-of select="count($results)"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='SKIPPED'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='FAILED'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='WARNING'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='PASSED_MANUAL'])"/>
							</td>
						</tr>
					</xsl:if>
					<!-- TEST STEPS STATS-->
					<xsl:if test="$ets/etf:testModule[1]/etf:TestModule/etf:testCases[1]/etf:TestCase/etf:testSteps[1]/etf:TestStep[etf:label ne 'IGNORE']">
						<xsl:variable name="results" select="$testTaskResult/etf:testModuleResults[1]/etf:TestModuleResult/etf:testCaseResults[1]/etf:TestCaseResult/etf:testStepResults[1]/etf:TestStepResult"/>
						<tr>
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'TestSteps']"/>
							</td>
							<td>
								<xsl:value-of select="count($results)"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='SKIPPED'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='FAILED'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='WARNING'])"/>
							</td>
							<td>
								<xsl:value-of select="count($results[etf:status='PASSED_MANUAL'])"/>
							</td>
						</tr>
					</xsl:if>
					<!-- TEST ASSERTIONS STATS-->
					<xsl:variable name="results" select="$testTaskResult/etf:testModuleResults[1]/etf:TestModuleResult/etf:testCaseResults[1]/etf:TestCaseResult/etf:testStepResults[1]/etf:TestStepResult/etf:testAssertionResults[1]/etf:TestAssertionResult"/>
					<tr>
						<td>
							<xsl:value-of select="$lang/x:e[@key = 'TestAssertions']"/>
						</td>
						<td>
							<xsl:value-of select="count($results)"/>
						</td>
						<td>
							<xsl:value-of select="count($results[etf:status='SKIPPED'])"/>
						</td>
						<td>
							<xsl:value-of select="count($results[etf:status='FAILED'])"/>
						</td>
						<td>
							<xsl:value-of select="count($results[etf:status='WARNING'])"/>
						</td>
						<td>
							<xsl:value-of select="count($results[etf:status='PASSED_MANUAL'])"/>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</xsl:template>
	<!-- Properties used in test run -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:ArgumentList">
		<div id="rprtParameters" data-role="collapsible" data-collapsed-icon="info" class="ReportDetail">
			<h3>
				<xsl:value-of select="$lang/x:e[@key = 'Parameters']"/>
			</h3>
			<table>
				<xsl:for-each select="etf:arguments/etf:argument">
					<xsl:if test="normalize-space(./text())">
						<tr>
							<td>
								<xsl:value-of select="./@name"/>
							</td>
							<td>
								<xsl:value-of select="./text()"/>
							</td>
						</tr>
					</xsl:if>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>
	<!-- TestObject -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:TestObject">
		<div id="rprtTestobject" data-role="collapsible" data-collapsed-icon="info" class="DoNotShowInSimpleView"><xsl:variable name="TestObject" select="."/><h3><xsl:value-of select="$lang/x:e[@key = 'TestObject']"/>: <xsl:value-of select="$TestObject/etf:label"/></h3><xsl:if test="$TestObject/etf:description and normalize-space($TestObject/etf:description/text()) ne ''"><xsl:value-of select="$TestObject/etf:description/text()" disable-output-escaping="yes"/></xsl:if><br/><br/><xsl:value-of select="$lang/x:e[@key = 'TestObjectTypes']"/>:
			   <ul><xsl:for-each select="$TestObject/etf:testObjectTypes/etf:testObjectType"><xsl:variable name="TestObjectType" select="key('testObjectTypeKey', ./@ref)"/><li><xsl:value-of select="$TestObjectType/etf:label/text()"/><xsl:if test="$TestObjectType/etf:description and normalize-space($TestObjectType/etf:description/text()) ne ''"><br/><xsl:value-of select="$TestObjectType/etf:description/text()" disable-output-escaping="yes"/></xsl:if></li></xsl:for-each></ul></div>
	</xsl:template>
	<!-- StatisticalReport -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:Attachment[@type = 'StatisticalReport']">
		<xsl:variable name="stat" select="document(./etf:referencedData/@href)/etf:StatisticalReportTable[etf:type/@ref='EID8bb8f162-1082-434f-bd06-23d6507634b8']"/>
		<xsl:if test="$stat">
			<div id="rprtStatReport" data-role="collapsible" data-collapsed-icon="info" class="DoNotShowInSimpleView"><xsl:variable name="StatisticalReport" select="."/>
				<h3><xsl:value-of select="./etf:label"/></h3>
				<table>
					<tr>
						<th><xsl:value-of select="$lang/x:e[@key = 'FeatureType']"/></th>
						<th><xsl:value-of select="$lang/x:e[@key = 'FeatureCount']"/></th>
					</tr>
					<xsl:for-each select="$stat/etf:entries/etf:entry">
						<tr>
							<td><xsl:value-of select="normalize-space(substring-before(text(),';'))"/></td>
							<td><xsl:value-of select="normalize-space(substring-after(text(),';'))"/></td>
						</tr>
					</xsl:for-each>
				</table>
			</div>
		</xsl:if>
	</xsl:template>
	<!-- Test suite result information -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:TestTaskResult">
		<xsl:variable name="TestSuite" select="key('testSuiteKey', ./etf:resultedFrom/@ref)"/>
		<xsl:variable name="resultItem" select="."/>
		<!-- Order by TestSuites -->
		<div class="TestSuite" data-role="collapsible" data-theme="e" data-content-theme="e">
			<xsl:attribute name="data-theme">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">e</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">e</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="data-content-theme">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">e</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">e</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<h2>
				<xsl:value-of select="$TestSuite/etf:label"/>
				<div class="ui-li-count">
					<xsl:variable name="FailedCount">
						<xsl:choose>
							<xsl:when test="$TestSuite/etf:testModules/etf:TestModule[etf:label ne 'IGNORE']">
								<xsl:value-of select="count(./etf:testModuleResults[1]/etf:TestModuleResult[etf:status = 'FAILED'])"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="count(./etf:testModuleResults[1]/etf:TestModuleResult/etf:testCaseResults[1]/etf:TestCaseResult[etf:status = 'FAILED'])"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:variable name="Count">
						<xsl:choose>
							<xsl:when test="$TestSuite/etf:testModules/etf:TestModule[etf:label ne 'IGNORE']">
								<xsl:value-of select="count(./etf:testModuleResults[1]/etf:TestModuleResult)"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="count(./etf:testModuleResults[1]/etf:TestModuleResult/etf:testCaseResults[1]/etf:TestCaseResult)"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:if test="$FailedCount &gt; 0"><xsl:value-of select="$lang/x:e[@key = 'FAILED']"/>: <xsl:value-of select="$FailedCount"/> / </xsl:if>
					<xsl:value-of select="$Count"/>
				</div>
			</h2>
			<xsl:if test="$TestSuite/etf:description and normalize-space($TestSuite/etf:description/text()) ne ''">
				<xsl:value-of select="$TestSuite/etf:description/text()" disable-output-escaping="yes"/>
			</xsl:if>
			<br/>
			<br/>
			<!-- General data about test result and test case -->
			<table>
				<tr>
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'Status']"/>
					</td>
					<td>
						<xsl:value-of select="$lang/x:e[@key = $resultItem/etf:status]"/>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
					</td>
					<td>
						<xsl:call-template name="formatDuration">
							<xsl:with-param name="ms" select="./etf:duration[1]/text()"/>
						</xsl:call-template>
					</td>
				</tr>
				<tr class="ReportDetail">
					<td><xsl:value-of select="$lang/x:e[@key = 'TestSuite']"/> ID</td>
					<td>
						<xsl:value-of select="$TestSuite/@id"/>
					</td>
				</tr>
				<xsl:call-template name="itemData">
					<xsl:with-param name="Node" select="$TestSuite"/>
				</xsl:call-template>
			</table>
			<br/>
			<!-- TestModule result information -->
			<xsl:apply-templates select="./etf:testModuleResults[1]/etf:TestModuleResult"/>
		</div>
	</xsl:template>
	<!-- Test module result information -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:TestModuleResult">
		<xsl:variable name="TestModule" select="key('testModuleKey', ./etf:resultedFrom/@ref)"/>
		<xsl:variable name="resultItem" select="."/>
		<xsl:choose>
			<xsl:when test="$TestModule/etf:label[1]/text() = 'IGNORE'">
				<div class="TestModulePlaceHolder">
					<xsl:apply-templates select="./etf:testCaseResults[1]/etf:TestCaseResult"/>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<!-- Order by TestModules -->
				<div class="TestModule" data-role="collapsible" data-theme="e" data-content-theme="e">
					<xsl:attribute name="data-theme">
						<xsl:choose>
							<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">j</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'WARNING'">j</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">j</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">j</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">e</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">e</xsl:when>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="data-content-theme">
						<xsl:choose>
							<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">g</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'WARNING'">g</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">g</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">g</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">e</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">e</xsl:when>
						</xsl:choose>
					</xsl:attribute>
					<xsl:variable name="FailedTestCaseCount" select="count(./etf:testCaseResults[1]/etf:TestCaseResult[etf:status = 'FAILED'])"/>
					<h2>
						<xsl:value-of select="$TestModule/etf:label"/>
						<div class="ui-li-count">
							<xsl:if test="$FailedTestCaseCount &gt; 0"><xsl:value-of select="$lang/x:e[@key = 'FAILED']"/>: <xsl:value-of select="$FailedTestCaseCount"/> / </xsl:if>
							<xsl:value-of select="count(./etf:testCaseResults[1]/etf:TestCaseResult)"/>
						</div>
					</h2>
					<xsl:if test="$TestModule/etf:description and normalize-space($TestModule/etf:description/text()) ne ''">
						<xsl:value-of select="$TestModule/etf:description/text()" disable-output-escaping="yes"/>
					</xsl:if>
					<br/>
					<br/>
					<table>
						<tr>
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'Status']"/>
							</td>
							<td>
								<xsl:value-of select="$lang/x:e[@key = $resultItem/etf:status[1]]"/>
							</td>
						</tr>
						<tr>
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
							</td>
							<td>
								<xsl:call-template name="formatDuration">
									<xsl:with-param name="ms" select="./etf:duration[1]/text()"/>
								</xsl:call-template>
							</td>
						</tr>
						<tr class="ReportDetail">
							<td><xsl:value-of select="$lang/x:e[@key = 'TestModule']"/> ID</td>
							<td>
								<xsl:value-of select="$TestModule/@id"/>
							</td>
						</tr>
					</table>
					<br/>
					<!-- TestCase result information -->
					<xsl:apply-templates select="./etf:testCaseResults[1]/etf:TestCaseResult"/>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- Test case result information -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:TestCaseResult">
		<xsl:variable name="TestCase" select="key('testCaseKey', ./etf:resultedFrom/@ref)"/>
		<xsl:variable name="resultItem" select="."/>
		<div data-role="collapsible" data-inset="false" data-mini="true">
			<xsl:attribute name="data-theme">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">f</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">f</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="data-content-theme">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">f</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">f</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="class">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">TestCase SuccessfulTestCase</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">TestCase ManualTestCase</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">TestCase FailedTestCase</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">TestCase SuccessfulTestCase</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">TestCase SuccessfulTestCase</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">TestCase FailedTestCase</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">TestCase SuccessfulTestCase DoNotShowInSimpleView</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">TestCase SuccessfulTestCase DoNotShowInSimpleView</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<h3>
				<xsl:variable name="label">
					<xsl:call-template name="string-replace">
						<xsl:with-param name="text" select="$TestCase/etf:label"/>
						<xsl:with-param name="replace" select="'(disabled)'"/>
						<xsl:with-param name="with" select="''"/>
					</xsl:call-template>
				</xsl:variable>
				<a name="{$TestCase/@id}"/>
				<xsl:value-of select="$label"/>
				<div class="ui-li-count">
					<xsl:variable name="FailedCount">
						<xsl:choose>
							<xsl:when test="$TestCase/etf:testSteps/etf:TestStep[etf:label ne 'IGNORE']">
								<xsl:value-of select="count(./etf:testStepResults[1]/etf:TestStepResult[etf:status = 'FAILED'])"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="count(./etf:testStepResults[1]/etf:TestStepResult/etf:testAssertionResults[1]/etf:TestAssertionResult[etf:status = 'FAILED'])"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:variable name="Count">
						<xsl:choose>
							<xsl:when test="$TestCase/etf:testSteps/etf:TestStep[etf:label ne 'IGNORE']">
								<xsl:value-of select="count(./etf:testStepResults[1]/etf:TestStepResult)"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="count(./etf:testStepResults[1]/etf:TestStepResult/etf:testAssertionResults[1]/etf:TestAssertionResult)"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:if test="$FailedCount &gt; 0"><xsl:value-of select="$lang/x:e[@key = 'FAILED']"/>: <xsl:value-of select="$FailedCount"/> / </xsl:if>
					<xsl:value-of select="$Count"/>
				</div>
			</h3>
			<xsl:if test="$TestCase/etf:description and normalize-space($TestCase/etf:description/text()) ne ''">
				<xsl:value-of select="$TestCase/etf:description/text()" disable-output-escaping="yes"/>
			</xsl:if>
			<br/>
			<br/>
			<!-- General data about test result and test case -->
			<table>
				<tr>
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'Status']"/>
					</td>
					<td>
						<xsl:value-of select="$lang/x:e[@key = $resultItem/etf:status]"/>
					</td>
				</tr>
				<tr>
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
					</td>
					<td>
						<xsl:call-template name="formatDuration">
							<xsl:with-param name="ms" select="./etf:duration[1]/text()"/>
						</xsl:call-template>
					</td>
				</tr>
				<xsl:for-each select="$TestCase/etf:dependencies/etf:testCase/@ref">
					<xsl:variable name="DepTestCase" select="key('testCaseKey', .)"/>
					<tr class="DoNotShowInSimpleView">
						<td>
							<xsl:value-of select="$lang/x:e[@key = 'Dependency']"/>
						</td>
						<td>
							<xsl:value-of select="$lang/x:e[@key = 'TestCase']"/>
							<a href="#{$DepTestCase/@id}">
								<xsl:value-of select="$DepTestCase/etf:label/text()"/>
							</a>
						</td>
					</tr>
				</xsl:for-each>
				<tr class="ReportDetail">
					<td><xsl:value-of select="$lang/x:e[@key = 'TestCase']"/> ID</td>
					<td>
						<xsl:value-of select="$TestCase/@id"/>
					</td>
				</tr>
				<xsl:call-template name="itemData">
					<xsl:with-param name="Node" select="$TestCase"/>
				</xsl:call-template>
			</table>
			<br/>
			<!--Add test step results and information about the teststeps -->
			<xsl:apply-templates select="./etf:testStepResults[1]/etf:TestStepResult"/>
		</div>
	</xsl:template>
	<!-- Test Step Results -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:TestStepResult">
		<!-- Information from referenced Test Step -->
		<xsl:variable name="TestStep" select="key('testStepKey', ./etf:resultedFrom/@ref)"/>
		<xsl:variable name="resultItem" select="."/>
		<xsl:choose>
			<xsl:when test="not($TestStep) or $TestStep/etf:label[1]/text() = 'IGNORE'">
				<div class="TestStepPlaceHolder">
					<xsl:apply-templates select="./etf:testAssertionResults[1]/etf:TestAssertionResult"/>
				</div>
			</xsl:when>
			<xsl:otherwise>
				<div class="TestStep" data-role="collapsible" data-theme="g" data-content-theme="g" data-mini="true">
					<xsl:attribute name="data-theme">
						<xsl:choose>
							<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">j</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'WARNING'">j</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">j</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">i</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'"/>
							<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">k</xsl:when>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="data-content-theme">
						<xsl:choose>
							<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">g</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'WARNING'">g</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">g</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">i</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'"/>
							<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'"/>
						</xsl:choose>
					</xsl:attribute>
					<xsl:attribute name="class">
						<xsl:choose>
							<xsl:when test="./etf:status[1]/text() = 'PASSED'">TestStep SuccessfulTestStep</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">TestStep ManualTestStep</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'FAILED'">TestStep FailedTestStep</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'WARNING'">TestStep SuccessfulTestStep</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">TestStep SuccessfulTestStep</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">TestStep FailedTestStep</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">TestStep SuccessfulTestStep DoNotShowInSimpleView</xsl:when>
							<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">TestStep SuccessfulTestStep DoNotShowInSimpleView</xsl:when>
						</xsl:choose>
					</xsl:attribute>
					<xsl:variable name="FailedAssertionCount" select="count(./etf:testAssertionResults[1]/etf:TestAssertionResult[etf:status[1]/text() = 'FAILED'])"/>
					<h4>
						<xsl:variable name="label">
							<xsl:call-template name="string-replace">
								<xsl:with-param name="text" select="$TestStep/etf:label[1]/text()"/>
								<xsl:with-param name="replace" select="'(disabled)'"/>
								<xsl:with-param name="with" select="''"/>
							</xsl:call-template>
						</xsl:variable>
						<xsl:value-of select="$label"/>
						<div class="ui-li-count">
							<xsl:if test="$FailedAssertionCount &gt; 0"><xsl:value-of select="$lang/x:e[@key = 'FAILED']"/>: <xsl:value-of select="$FailedAssertionCount"/> / </xsl:if>
							<xsl:value-of select="count(./etf:testAssertionResults[1]/etf:TestAssertionResult)"/>
						</div>
					</h4>
					<xsl:if test="$TestStep/etf:description and normalize-space($TestStep/etf:description/text()) ne ''">
						<xsl:value-of select="$TestStep/etf:description/text()" disable-output-escaping="yes"/>
					</xsl:if>
					<br/>
					<br/>
					<table>
						<tr>
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'Status']"/>
							</td>
							<td>
								<xsl:value-of select="$lang/x:e[@key = $resultItem/etf:status]"/>
							</td>
						</tr>
						<tr class="DoNotShowInSimpleView">
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'Started']"/>
							</td>
							<td>
								<xsl:call-template name="formatDate">
									<xsl:with-param name="DateTime" select="./etf:startTimestamp[1]/text()"/>
								</xsl:call-template>
							</td>
						</tr>
						<tr>
							<td>
								<xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
							</td>
							<td>
								<xsl:call-template name="formatDuration">
									<xsl:with-param name="ms" select="./etf:duration[1]/text()"/>
								</xsl:call-template>
							</td>
						</tr>
						<tr class="ReportDetail">
							<td><xsl:value-of select="$lang/x:e[@key = 'TestStep']"/> ID</td>
							<td>
								<xsl:value-of select="$TestStep/@id"/>
							</td>
						</tr>
					</table>
					<br/>
					<!-- Execution Statement -->
					<xsl:if test="$TestStep/etf:statementForExecution[1]">
						<div class="Request">
							<xsl:if test="$FailedAssertionCount = 0">
								<xsl:attribute name="DoNotShowInSimpleView"/>
							</xsl:if>
							<xsl:apply-templates select="$TestStep/etf:statementForExecution[1]"/>
						</div>
					</xsl:if>
					<!-- Response -->
					<div class="Response DoNotShowInSimpleView">
						<!-- TODO -->
						<xsl:apply-templates select="./etf:attachment[1]"/>
					</div>
					<!-- Test step failure messages-->
					<xsl:if test="./etf:messages/*">
						<!-- TODO -->
						<xsl:apply-templates select="./etf:messages[1]"/>
					</xsl:if>
					<!-- Get Assertion results -->
					<xsl:if test="./etf:testAssertionResults[1]/etf:TestAssertionResult">
						<div class="AssertionsContainer">
							<h4><xsl:value-of select="$lang/x:e[@key = 'TestAssertions']"/>:</h4>
							<xsl:apply-templates select="./etf:testAssertionResults[1]/etf:TestAssertionResult"/>
						</div>
					</xsl:if>
				</div>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- Messages -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:statementForExecution">
		<div class="ExecutionStatement">
			<xsl:variable name="id" select="generate-id(.)"/>
			<label for="{$id}">
				<xsl:value-of select="$lang/x:e[@key = 'ExecutionStatement']"/>
			</label>
			<textarea id="{$id}.executionStatement" data-mini="true">
				<xsl:value-of select="text()"/>
			</textarea>
		</div>
	</xsl:template>
	<!-- Assertion results -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:TestAssertionResult">
		<xsl:variable name="resultItem" select="."/>
		<xsl:variable name="TestAssertion" select="key('testAssertionKey', ./etf:resultedFrom/@ref)"/>
		<div data-role="collapsible" data-mini="true">
			<!-- Assertion Styling: Set attributes do indicate the status -->
			<xsl:attribute name="data-theme">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">j</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">f</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">f</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="data-content-theme">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">h</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">g</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">f</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">f</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="data-collapsed-icon">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">check</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">eye</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">alert</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">info</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">info</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">info</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">forbidden</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">forbidden</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="class">
				<xsl:choose>
					<xsl:when test="./etf:status[1]/text() = 'PASSED'">Assertion SuccessfulAssertion</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'PASSED_MANUAL'">Assertion ManualAssertion</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'FAILED'">Assertion FailedAssertion</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'WARNING'">Assertion SuccessfulAssertion</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'INFORMATION'">Assertion SuccessfulAssertion</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'SKIPPED'">Assertion FailedAssertion</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'NOT_APPLICABLE'">Assertion SuccessfulAssertion DoNotShowInSimpleView</xsl:when>
					<xsl:when test="./etf:status[1]/text() = 'UNDEFINED'">Assertion SuccessfulAssertion DoNotShowInSimpleView</xsl:when>
				</xsl:choose>
			</xsl:attribute>
			<xsl:variable name="id" select="./@id"/>
			<!-- Information from referenced Assertion -->
			<h5>
				<xsl:value-of select="$TestAssertion/etf:label"/>
			</h5>
			<xsl:if test="$TestAssertion/etf:description and normalize-space($TestAssertion/etf:description/text()) ne ''">
				<xsl:value-of select="$TestAssertion/etf:description/text()" disable-output-escaping="yes"/>
			</xsl:if>
			<br/>
			<br/>
			<table>
				<tr>
					<td>
						<xsl:value-of select="$lang/x:e[@key = 'Status']"/>
					</td>
					<td>
						<xsl:value-of select="$lang/x:e[@key = $resultItem/etf:status]"/>
					</td>
				</tr>
				<xsl:if test="./etf:duration[1]/text()">
					<tr>
						<td>
							<xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
						</td>
						<td>
							<xsl:call-template name="formatDuration">
								<xsl:with-param name="ms" select="./etf:duration[1]/text()"/>
							</xsl:call-template>
						</td>
					</tr>
				</xsl:if>
				<tr class="ReportDetail">
					<td><xsl:value-of select="$lang/x:e[@key = 'TestAssertion']"/> ID</td>
					<td>
						<xsl:value-of select="$TestAssertion/@id"/>
					</td>
				</tr>
			</table>
			<br/>
			<!-- TODO TOKEN REPLACEMENT -->
			<xsl:if test="$TestAssertion/etf:expression and normalize-space($TestAssertion/etf:expression) != ('','''PASSED''')">
				<div class="ReportDetail Expression">
					<label for="{$id}.expression"><xsl:value-of select="$lang/x:e[@key = 'Expression']"/>:</label>
					<textarea id="{$id}.expression" class="Expression" data-mini="true">
						<xsl:value-of select="$TestAssertion/etf:expression"/>
					</textarea>
				</div>
			</xsl:if>
			<xsl:if test="$TestAssertion/etf:expectedResult and normalize-space($TestAssertion/etf:expectedResult) != ('','NOT_APPLICABLE')">
				<div class="ReportDetail ExpectedResult">
					<label for="{$id}.expectedResult"><xsl:value-of select="$lang/x:e[@key = 'ExpectedResult']"/>:</label>
					<textarea id="{$id}.expectedResult" class="ExpectedResult" data-mini="true">
						<xsl:value-of select="$TestAssertion/etf:expectedResult"/>
					</textarea>
				</div>
			</xsl:if>
			<xsl:if test="etf:messages[1]/*">
				<xsl:apply-templates select="etf:messages[1]"/>
			</xsl:if>
		</div>
	</xsl:template>
	<!-- Item data information without label -->
	<!-- ########################################################################################## -->
	<xsl:template name="itemData">
		<xsl:param name="Node"/>
		<xsl:if test="$Node/etf:startTimestamp/text()">
			<tr class="DoNotShowInSimpleView">
				<td>
					<xsl:value-of select="$lang/x:e[@key = 'Started']"/>
				</td>
				<td>
					<xsl:call-template name="formatDate">
						<xsl:with-param name="DateTime" select="$Node/etf:startTimestamp"/>
					</xsl:call-template>
				</td>
			</tr>
		</xsl:if>
		<xsl:if test="$Node/etf:duration/text()">
			<tr>
				<td>
					<xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
				</td>
				<td>
					<xsl:call-template name="formatDuration">
						<xsl:with-param name="ms" select="$Node/etf:duration[1]/text()"/>
					</xsl:call-template>
				</td>
			</tr>
		</xsl:if>
		<!-- Version Data -->
		<xsl:if test="$Node/etf:author/text()">
			<tr class="ReportDetail">
				<td>
					<xsl:value-of select="$lang/x:e[@key = 'Author']"/>
				</td>
				<td>
					<xsl:value-of select="$Node/etf:author"/>
				</td>
			</tr>
		</xsl:if>
		<xsl:if test="$Node/etf:creationDate/text()">
			<tr class="ReportDetail">
				<td>
					<xsl:value-of select="$lang/x:e[@key = 'DateCreated']"/>
				</td>
				<td>
					<xsl:call-template name="formatDate">
						<xsl:with-param name="DateTime" select="$Node/etf:creationDate"/>
					</xsl:call-template>
				</td>
			</tr>
		</xsl:if>
		<xsl:if test="$Node/etf:version/text()">
			<tr class="ReportDetail">
				<td>
					<xsl:value-of select="$lang/x:e[@key = 'Version']"/>
				</td>
				<td>
					<xsl:value-of select="$Node/etf:version"/>
				</td>
			</tr>
		</xsl:if>
		<xsl:if test="$Node/etf:lastEditor/text()">
			<tr class="ReportDetail">
				<td>
					<xsl:value-of select="$lang/x:e[@key = 'LastEditor']"/>
				</td>
				<td>
					<xsl:value-of select="$Node/etf:lastEditor"/>
				</td>
			</tr>
		</xsl:if>
		<xsl:if test="$Node/etf:lastUpdateDate/text()">
			<tr class="ReportDetail">
				<td>
					<xsl:value-of select="$lang/x:e[@key = 'LastUpdated']"/>
				</td>
				<td>
					<xsl:call-template name="formatDate">
						<xsl:with-param name="DateTime" select="$Node/etf:lastUpdateDate"/>
					</xsl:call-template>
				</td>
			</tr>
		</xsl:if>
		<xsl:if test="$Node/etf:itemHash/text()">
			<tr class="ReportDetail">
				<td>
					<xsl:value-of select="$lang/x:e[@key = 'Hash']"/>
				</td>
				<td>
					<xsl:value-of select="$Node/etf:itemHash"/>
				</td>
			</tr>
		</xsl:if>
	</xsl:template>
	<!-- Messages -->
	<!-- ########################################################################################## -->
	<xsl:template match="etf:messages">
		<div class="FailureMessage">
			<xsl:if test="./etf:message">
				<xsl:variable name="id" select="generate-id(.)"/>
				<label for="{$id}">
					<xsl:value-of select="$lang/x:e[@key = 'Messages']"/>
				</label>
				<textarea id="{$id}.failureMessages" data-mini="true">
					<xsl:for-each select="./etf:message">
						<xsl:call-template name="translateMessage">
							<xsl:with-param name="templateId" select="./@ref"/>
							<xsl:with-param name="translationArguments" select="./etf:translationArguments"/>
						</xsl:call-template>
					</xsl:for-each>
				</textarea>
			</xsl:if>
		</div>
	</xsl:template>
	<xsl:template name="translateMessage">
		<xsl:param name="templateId" as="xs:string"/>
		<xsl:param name="translationArguments" as="node()"/>
		<xsl:variable name="template" select="key('translationKey', $templateId)"/>
		<xsl:variable name="str" select="$template[1]/text()"/>
		<xsl:if test="not(normalize-space($str))">
			<xsl:message terminate="yes">ERROR: Translation template for ID <xsl:value-of select="$templateId"/> not found</xsl:message>
		</xsl:if>
		<xsl:value-of select="                 concat(                 etf:replace-multi-tokens($str, $translationArguments[1]/etf:argument/@token, $translationArguments[1]/etf:argument/text()), '&#13;&#10;')"/>
	</xsl:template>
	<xsl:function name="etf:replace-multi-tokens" as="xs:string?">
		<xsl:param name="arg" as="xs:string?"/>
		<xsl:param name="changeFrom" as="xs:string*"/>
		<xsl:param name="changeTo" as="xs:string*"/>
		<xsl:sequence select="                 if (count($changeFrom) &gt; 0)                 then                     etf:replace-multi-tokens(                     replace($arg, concat('\{', $changeFrom[1], '\}'),                     etf:if-absent($changeTo[1], '')),                     $changeFrom[position() &gt; 1],                     $changeTo[position() &gt; 1])                 else                     $arg                 "/>
	</xsl:function>
	<xsl:function name="etf:if-absent" as="item()*">
		<xsl:param name="arg" as="item()*"/>
		<xsl:param name="value" as="item()*"/>
		<xsl:sequence select="                 if (exists($arg))                 then                     $arg                 else                     $value                 "/>
	</xsl:function>
	<!-- ChangeFailureMessage -->
	<!-- ########################################################################################## -->
	<xsl:template name="ChangeFailureMessage">
		<xsl:param name="FailureMessageText"/>
		<!--
   By default the whole expression of a XQuery will be written out in error messages.
   In this template function the error message will be "beautified".
   To use this functionality the xquery shall be in the following form:
   <result>AssertionFailures:
   {
     XQUERY Functions
     if( xyz=FAILURE ) then return 'failure message foo bar... '
     else ''
   </result>
  -->
		<xsl:variable name="AssertionFailuresText">AssertionFailures:</xsl:variable>
		<xsl:variable name="BeautifiedMessage">
			<xsl:call-template name="substring-after-last">
				<xsl:with-param name="input" select="$FailureMessageText"/>
				<xsl:with-param name="substr" select="$AssertionFailuresText"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="BeautifiedXqueryOutput" select="substring-before($BeautifiedMessage, '&lt;/result')"/>
		<xsl:choose>
			<xsl:when test="normalize-space($BeautifiedXqueryOutput)">
				<xsl:value-of select="$BeautifiedXqueryOutput"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$FailureMessageText"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- Footer -->
	<!-- ########################################################################################## -->
	<xsl:template name="footer">
		<div data-role="footer">
			<h1>Report generated by ETF</h1>
		</div>
		<xsl:call-template name="footerScripts"/>
	</xsl:template>
</xsl:stylesheet>
