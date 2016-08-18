<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.interactive-instruments.de/etf/2.0"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:etf="http://www.interactive-instruments.de/etf/2.0" xmlns:x="ii.exclude"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" exclude-result-prefixes="x xs"
    version="2.0">

    <xsl:import href="lang/current.xsl"/>
    <xsl:import href="jsAndCss.xsl"/>
    <xsl:import href="UtilityTemplates.xsl"/>

    <xsl:param name="language">en</xsl:param>
    <xsl:param name="serviceUrl" select="'https://localhost/etf'"/>

    <xsl:output method="html" indent="yes" encoding="UTF-8"/>

    <!-- Translation TODO -->
    <xsl:key name="translation" match="x:lang/x:e" use="@key"/>
    <xsl:variable name="lang" select="document('lang/current.xsl')/*/x:lang"/>
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

    <!-- Property Level of details -->
    <x:propertyVisibility>
        <!-- possible values: 
   -not mapped -> show always
   -ReportDetail
   -DoNotShowInSimpleView
   -->
        <x:e key="DetailedDescription">DoNotShowInSimpleView</x:e>
        <x:e key="Reference">ReportDetail</x:e>
        <x:e key="SpecificationReference">DoNotShowInSimpleView</x:e>
    </x:propertyVisibility>
    <xsl:key name="propertyVisibility" match="x:propertyVisibility/x:e" use="@key"/>


    <!-- Create lookup tables for faster id lookups -->
    <xsl:key name="etsKey"
        match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite" use="@id"/>
    <xsl:key name="testModuleKey"
        match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite/etf:testModules[1]/etf:TestModule"
        use="@id"/>
    <xsl:key name="testCaseKey"
        match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite/etf:testModules[1]/etf:TestModule/etf:testCases[1]/etf:TestCase"
        use="@id"/>
    <xsl:key name="testStepKey"
        match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite/etf:testModules[1]/etf:TestModule/etf:testCases[1]/etf:TestCase/etf:testSteps[1]/etf:TestStep"
        use="@id"/>
    <xsl:key name="testAssertionKey"
        match="/etf:DsResultSet/etf:executableTestSuites[1]/etf:ExecutableTestSuite/etf:testModules[1]/etf:TestModule/etf:testCases[1]/etf:TestCase/etf:testSteps[1]/etf:TestStep/etf:testAssertions[1]/etf:TestAssertion"
        use="@id"/>
    <xsl:key name="testItemTypeKey" match="/etf:DsResultSet/etf:testItemTypes[1]/etf:TestItemType"
        use="@id"/>
    <xsl:key name="testObjectKey" match="/etf:DsResultSet/etf:testObjects[1]/etf:TestObject"
        use="@id"/>
    <xsl:key name="testObjectTypeKey" match="/etf:DsResultSet/etf:testObjectTypes[1]/etf:TestObjectType"
        use="@id"/>
    <xsl:key name="translationKey"
        match="/etf:DsResultSet/etf:translationTemplateBundles[1]/etf:TranslationTemplateBundle/etf:translationTemplateCollections[1]/etf:LangTranslationTemplateCollection/etf:translationTemplates[1]/etf:TranslationTemplate[@language = $language]"
        use="@name"/>
    <xsl:key name="testTaskKey"
        match="/etf:DsResultSet/etf:testRuns[1]/etf:TestRun/etf:testTasks[1]/etf:TestTask" use="@id"/>
    <xsl:key name="testTaskResultKey"
        match="/etf:DsResultSet/etf:testTaskResults[1]/etf:TestTaskResult" use="@id"/>
    <xsl:key name="attachmentsKey"
        match="/etf:DsResultSet/etf:testTaskResults[1]/etf:TestTaskResult/etf:attachments[1]/etf:Attachment"
        use="@id"/>

    <xsl:variable name="testTaskResult"
        select="/etf:DsResultSet/etf:testTaskResults[1]/etf:TestTaskResult[1]"/>
    <xsl:variable name="testTask" select="key('testTaskKey', $testTaskResult/etf:parent[1]/@ref)"/>
    <xsl:variable name="ets" select="key('etsKey', $testTask/etf:executableTestSuite[1]/@ref)"/>
    <xsl:variable name="testObject" select="key('testObjectKey', $testTask/etf:testObject[1]/@ref)"/>
    <xsl:variable name="statisticAttachment"
        select="$testTaskResult/etf:attachments[1]/etf:Attachment[@type = 'StatisticalReport']"/>

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
                    <h1>
                        <xsl:value-of select="./etf:testRuns[1]/etf:TestRun/etf:label/text()"/>
                    </h1>
                    <a href="{$serviceUrl}/results" data-ajax="false" data-icon="back"
                        data-iconpos="notext"/>
                </div>
                <div data-role="content">
                    <div class="ui-grid-a">
                        <div class="ui-block-a">
                            <xsl:call-template name="reportInfo"/>
                        </div>
                        <div class="ui-block-b">
                            <xsl:call-template name="controls"/>
                        </div>
                    </div>

                    <xsl:apply-templates select="$testTask/etf:ArgumentList[1]"/>

                    <!-- Test object -->
                    <xsl:apply-templates select="$testObject"/>

                    <!-- Additional statistics provided by the test project -->
                    <xsl:apply-templates select="$statisticAttachment"/>

                    <!-- Test Suite Results -->
                    <xsl:apply-templates
                        select="$testTaskResult/etf:testModuleResults[1]/etf:TestModuleResult"/>
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
                        <a
                            href="{$serviceUrl}/TestTaskResult/{$testTaskResult/@id}.html?lang={$language}"
                            data-ajax="false">
                            <xsl:value-of select="$testTaskResult/@id"/>
                        </a>
                    </td>
                </tr>

                <tr>
                    <td>
                        <xsl:value-of select="$lang/x:e[@key = 'Started']"/>
                    </td>
                    <td>
                        <xsl:call-template name="formatDate">
                            <xsl:with-param name="DateTime"
                                select="$testTaskResult/etf:startTimestamp"/>
                        </xsl:call-template>
                    </td>
                </tr>
                <tr class="DoNotShowInSimpleView">
                    <td>
                        <xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
                    </td>
                    <td>
                        <xsl:value-of select="$testTaskResult/etf:duration"/> ms </td>
                </tr>
                <tr class="ReportDetail">
                    <td>
                        <xsl:value-of select="$lang/x:e[@key = 'Status']"/>
                    </td>
                    <td>
                        <xsl:value-of select="$testTaskResult/etf:status"/>
                    </td>
                </tr>
            </table>
        </div>
    </xsl:template>


    <!-- Properties used in test run -->
    <!-- ########################################################################################## -->
    <xsl:template match="etf:ArgumentList">
        <div id="rprtParameters" data-role="collapsible" data-collapsed-icon="info"
            class="ReportDetail">
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
        <div id="rprtTestobject" data-role="collapsible" data-collapsed-icon="info"
            class="ReportDetail">
            <h3>
                <xsl:value-of select="$lang/x:e[@key = 'TestObject']"/>
            </h3>
            <table>
                <tr>
                    <td>
                        <xsl:value-of select="$lang/x:e[@key = 'Label']"/>
                    </td>
                    <td>
                        <xsl:value-of select="etf:label"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <xsl:value-of select="$lang/x:e[@key = 'Description']"/>
                    </td>
                    <td>
                        <xsl:value-of select="etf:description"/>
                    </td>
                </tr>
            </table>
        </div>
    </xsl:template>

    <!-- Test suite result information -->
    <!-- ########################################################################################## -->
    <xsl:template match="etf:TestModuleResult">

        <xsl:variable name="TestModule" select="key('testModuleKey', ./etf:resultedFrom/@ref)"/>

        <xsl:choose>
            <xsl:when test="$TestModule/etf:label[1]/text() = 'IGNORE'">
                <div class="TestModulePlaceHolder">
                    <xsl:apply-templates select="./etf:testCaseResults[1]/etf:TestCaseResult"/>
                </div>
            </xsl:when>
            <xsl:otherwise>
                <!-- Order by TestModules -->
                <div class="TestModule" data-role="collapsible" data-theme="e"
                    data-content-theme="e">
                    <xsl:variable name="FailedTestCaseCount"
                        select="count(./etf:testCaseResults[1]/etf:TestCaseResult[etf:status = 'FAILED'])"/>
                    <h2>
                        <xsl:value-of select="$TestModule/etf:label"/>
                        <div class="ui-li-count">
                            <xsl:if test="$FailedTestCaseCount > 0"> Failed: <xsl:value-of
                                    select="$FailedTestCaseCount"/> / </xsl:if>
                            <xsl:value-of
                                select="count(./etf:testCaseResults[1]/etf:TestCaseResult)"/>
                        </div>
                    </h2>
                    <!-- TestCase result information -->
                    <xsl:apply-templates select="./etf:testCaseResults[1]/etf:TestCaseResult"/>
                </div>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>

    <!-- Test case result information -->
    <!-- ########################################################################################## -->
    <xsl:template match="etf:TestCaseResult">

        <div data-role="collapsible" data-theme="f" data-content-theme="f" data-inset="false"
            data-mini="true">

            <xsl:choose>
                <xsl:when test="./etf:status = 'FAILED'">
                    <xsl:attribute name="data-collapsed-icon">alert</xsl:attribute>
                    <!--xsl:attribute name="data-theme">i</xsl:attribute-->
                    <xsl:attribute name="class">TestCase FailedTestCase</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="class">TestCase SuccessfulTestCase</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>

            <xsl:variable name="TestCase" select="key('testCaseKey', ./etf:resultedFrom/@ref)"/>
            <xsl:variable name="TestStepCount"
                select="count(./etf:testStepResults[1]/etf:TestStepResult)"/>
            <xsl:variable name="FailedTestStepCount"
                select="count(./etf:testStepResults[1]/etf:TestStepResult[etf:status = 'FAILED'])"/>

            <h3>
                <xsl:variable name="label">
                    <xsl:call-template name="string-replace">
                        <xsl:with-param name="text" select="$TestCase/etf:label"/>
                        <xsl:with-param name="replace" select="'(disabled)'"/>
                        <xsl:with-param name="with" select="''"/>
                    </xsl:call-template>
                </xsl:variable>

                <xsl:value-of select="$label"/>


                <div class="ui-li-count">
                    <xsl:if test="$FailedTestStepCount > 0"> Failed: <xsl:value-of
                            select="$FailedTestStepCount"/> / </xsl:if>
                    <xsl:value-of select="$TestStepCount"/>
                </div>
            </h3>

            <!-- General data about test result and test case -->
            <table>
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

            <!--Add test step results and information about the teststeps -->
            <xsl:apply-templates select="./etf:testStepResults[1]/etf:TestStepResult"/>

        </div>

    </xsl:template>

    <!-- Test Step Results -->
    <!-- ########################################################################################## -->
    <xsl:template match="etf:TestStepResult">

        <!-- Information from referenced Test Step -->
        <xsl:variable name="TestStep" select="key('testStepKey', ./etf:resultedFrom/@ref)"/>

        <xsl:choose>
            <xsl:when test="not($TestStep) or $TestStep/etf:label[1]/text() = 'IGNORE'">
                <div class="TestStepPlaceHolder">
                    <xsl:apply-templates
                        select="./etf:testAssertionResults[1]/etf:TestAssertionResult"/>
                </div>
            </xsl:when>
            <xsl:otherwise>
                <div class="TestStep" data-role="collapsible" data-theme="g" data-content-theme="g"
                    data-mini="true">

                    <xsl:choose>
                        <xsl:when test="./etf:status[1]/text() = 'FAILED'">
                            <xsl:attribute name="data-collapsed-icon">alert</xsl:attribute>
                            <xsl:attribute name="data-theme">i</xsl:attribute>
                            <!--xsl:attribute name="data-content-theme">i</xsl:attribute-->
                            <xsl:attribute name="class">TestStep FailedTestStep</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="./etf:status[1]/text() = 'OK'">
                            <xsl:attribute name="class">TestStep SuccessfulTestStep</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="./etf:status[1]/text() = 'WARNING'">
                            <xsl:attribute name="data-theme">j</xsl:attribute>
                            <xsl:attribute name="class">TestStep WarningInTestStep</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="./etf:status[1]/text() = 'SKIPPED'">
                            <xsl:attribute name="data-theme">j</xsl:attribute>
                            <xsl:attribute name="class">TestStep SkippedTestStep</xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <!-- unknown status -->
                            <xsl:attribute name="class">TestStep</xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>

                    <xsl:variable name="FailedAssertionCount"
                        select="count(./etf:testAssertionResults[1]/etf:TestAssertionResult[etf:status[1]/text() = 'FAILED'])"/>
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
                            <xsl:if test="$FailedAssertionCount > 0"> Failed: <xsl:value-of
                                    select="$FailedAssertionCount"/> / </xsl:if>
                            <xsl:value-of
                                select="count(./etf:testAssertionResults[1]/etf:TestAssertionResult)"
                            />
                        </div>
                    </h4>

                    <table>
                        <tr class="ReportDetail">
                            <td><xsl:value-of select="$lang/x:e[@key = 'TestStep']"/> ID</td>
                            <td>
                                <xsl:value-of select="$TestStep/@id"/>
                            </td>
                        </tr>
                        <xsl:if test="$TestStep/etf:description[1]/text()">
                            <tr>
                                <td><xsl:value-of select="$lang/x:e[@key = 'Description']"/>: </td>
                                <td>
                                    <xsl:value-of select="$TestStep/etf:description[1]/text()"/>
                                </td>
                            </tr>
                        </xsl:if>
                        <tr>
                            <td><xsl:value-of select="$lang/x:e[@key = 'Duration']"/>: </td>
                            <td>
                                <xsl:value-of select="./etf:duration[1]/text()"/> ms </td>
                        </tr>
                        <tr class="DoNotShowInSimpleView">
                            <td><xsl:value-of select="$lang/x:e[@key = 'Started']"/>: </td>
                            <td>
                                <xsl:call-template name="formatDate">
                                    <xsl:with-param name="DateTime"
                                        select="./etf:startTimestamp[1]/text()"/>
                                </xsl:call-template>
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
                            <xsl:apply-templates
                                select="./etf:testAssertionResults[1]/etf:TestAssertionResult"/>
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

        <div data-role="collapsible" data-mini="true">
            <!-- Assertion Styling: Set attributes do indicate the status -->
            <xsl:attribute name="data-theme">
                <xsl:choose>
                    <xsl:when test="./etf:status[1]/text() = 'OK'">h</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'WARNING'">j</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'SKIPPED'"/>
                    <xsl:otherwise>k</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="data-content-theme">
                <xsl:choose>
                    <xsl:when test="./etf:status[1]/text() = 'OK'">h</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'FAILED'">i</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'WARNING'">g</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'SKIPPED'"/>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="data-collapsed-icon">
                <xsl:choose>
                    <xsl:when test="./etf:status[1]/text() = 'OK'">check</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'FAILED'">alert</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'WARNING'">alert</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'SKIPPED'">forbidden</xsl:when>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="./etf:status[1]/text() = 'OK'">Assertion
                        SuccessfulAssertion</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'FAILED'">Assertion
                        FailedAssertion</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'WARNING'">Assertion
                        WarningAssertion</xsl:when>
                    <xsl:when test="./etf:status[1]/text() = 'SKIPPED'">Assertion SkippedAssertion
                        DoNotShowInSimpleView</xsl:when>
                </xsl:choose>
            </xsl:attribute>

            <xsl:variable name="id" select="./@id"/>

            <!-- Information from referenced Assertion -->
            <xsl:variable name="TestAssertion"
                select="key('testAssertionKey', ./etf:resultedFrom/@ref)"/>
            <h5>
                <xsl:value-of select="$TestAssertion/etf:label"/>
            </h5>

            <table>
                <tr class="ReportDetail">
                    <td><xsl:value-of select="$lang/x:e[@key = 'TestAssertion']"/> ID</td>
                    <td>
                        <xsl:value-of select="$TestAssertion/@id"/>
                    </td>
                </tr>
                <xsl:if test="./etf:duration[1]/text()">
                    <tr>
                        <td>
                            <xsl:value-of select="$lang/x:e[@key = 'Duration']"/>
                        </td>
                        <td>
                            <xsl:value-of select="./etf:duration"/> ms </td>
                    </tr>
                </xsl:if>
                <xsl:if test="./etf:description[1]/text()">
                    <tr>
                        <td>
                            <xsl:value-of select="$lang/x:e[@key = 'Description']"/>
                        </td>
                        <td>
                            <xsl:value-of select="./etf:description"/>
                        </td>
                    </tr>
                </xsl:if>
                <tr>
                    <td>
                        <xsl:value-of select="$lang/x:e[@key = 'AssertionType']"/>
                    </td>
                    <td>
                        <xsl:value-of select="$TestAssertion/etf:testItemType"/>
                    </td>
                </tr>
            </table>

            <!-- TODO TOKEN REPLACEMENT -->

            <div class="ReportDetail Expression">
                <label for="{$id}.expression"><xsl:value-of select="$lang/x:e[@key = 'Expression']"
                    />:</label>
                <textarea id="{$id}.expression" class="Expression" data-mini="true">
                    <xsl:value-of select="$TestAssertion/etf:expression"/>
                </textarea>
            </div>

            <xsl:if test="$TestAssertion/etf:expectedResult/text()">
                <div class="ReportDetail ExpectedResult">
                    <label for="{$id}.expectedResult"><xsl:value-of
                            select="$lang/x:e[@key = 'ExpectedResult']"/>:</label>
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

    <!-- Output short description or description from etf:description element or etf:properties -->
    <!-- ########################################################################################## -->
    <xsl:template name="descriptionItem">
        <xsl:param name="Node"/>
        <xsl:variable name="descriptionText" select="$Node/etf:description/text()"/>
        <xsl:if test="normalize-space($descriptionText) != ''">
            <tr>
                <td>
                    <xsl:value-of select="$lang/x:e[@key = 'Description']"/>
                </td>
                <td>
                    <xsl:value-of select="$descriptionText"/>
                </td>
            </tr>
        </xsl:if>

    </xsl:template>

    <!-- Item data information without label -->
    <!-- ########################################################################################## -->
    <xsl:template name="itemData">
        <xsl:param name="Node"/>

        <xsl:if test="$Node/etf:startTimestamp/text()">
            <tr class="ReportDetail">
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
                    <xsl:value-of select="$Node/etf:duration"/>
                </td>
            </tr>
        </xsl:if>

        <xsl:call-template name="descriptionItem">
            <xsl:with-param name="Node" select="$Node"/>
        </xsl:call-template>

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
            <tr class="DoNotShowInSimpleView">
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
            <tr class="DoNotShowInSimpleView">
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
                            <xsl:with-param name="translationArguments"
                                select="./etf:translationArguments"/>
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
            <xsl:message terminate="yes">ERROR: Translation template for ID <xsl:value-of
                    select="$templateId"/> not found</xsl:message>
        </xsl:if>

        <xsl:value-of
            select="
                concat(
                etf:replace-multi-tokens($str, $translationArguments[1]/etf:argument/@token, $translationArguments[1]/etf:argument/text()), '&#13;&#10;')"/>

    </xsl:template>


    <xsl:function name="etf:replace-multi-tokens" as="xs:string?">
        <xsl:param name="arg" as="xs:string?"/>
        <xsl:param name="changeFrom" as="xs:string*"/>
        <xsl:param name="changeTo" as="xs:string*"/>
        <xsl:sequence
            select="
                if (count($changeFrom) > 0)
                then
                    etf:replace-multi-tokens(
                    replace($arg, concat('\{', $changeFrom[1], '\}'),
                    etf:if-absent($changeTo[1], '')),
                    $changeFrom[position() > 1],
                    $changeTo[position() > 1])
                else
                    $arg
                "
        />
    </xsl:function>

    <xsl:function name="etf:if-absent" as="item()*">
        <xsl:param name="arg" as="item()*"/>
        <xsl:param name="value" as="item()*"/>
        <xsl:sequence
            select="
                if (exists($arg))
                then
                    $arg
                else
                    $value
                "
        />
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
        <xsl:variable name="BeautifiedXqueryOutput"
            select="substring-before($BeautifiedMessage, '&lt;/result')"/>

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
            <h1>ETF © 2013-2015 interactive instruments</h1>
        </div>
        <xsl:call-template name="footerScripts"/>
    </xsl:template>

</xsl:stylesheet>
