<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.interactive-instruments.de/etf/2.0"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" exclude-result-prefixes="xs" version="2.0">

    <xsl:output method="xml" />

    <xsl:param name="serviceUrl" select="'https://localhost/etf'"/>
    <xsl:param name="selection"/>
    <xsl:param name="offset" select="0"/>
    <xsl:param name="limit" select="0"/>

    <xsl:key name="ids" match="/etf:DsResultSet/*//*" use="@id"/>
    <xsl:key name="translationNames"
        match="/etf:DsResultSet/etf:translationTemplateBundles/etf:TranslationTemplateBundle/etf:translationTemplateCollections/etf:LangTranslationTemplateCollection"
        use="@name"/>

    <xsl:attribute-set name="CollectionAttributes">
        <xsl:attribute name="version">1.0</xsl:attribute>
        <xsl:attribute name="xsi:schemaLocation">http://www.interactive-instruments.de/etf/2.0 file:/Users/herrmann/Projects/etf-fbs/etf-bsxds/src/main/resources/schema/service/service.xsd</xsl:attribute>
    </xsl:attribute-set>

    <!-- =============================================================== -->
    <xsl:template match="/etf:DsResultSet">
        <xsl:element name="EtfItemCollection" use-attribute-sets="CollectionAttributes">

            <xsl:variable name="subSet" select="*[./*[1]/local-name() = $selection]"/>
            <xsl:attribute name="returnedItems" select="count($subSet/*)"/>
            <xsl:if test="count($subSet/*) eq 0">
                <xsl:message terminate="yes">ERROR: empty collection for selection "<xsl:value-of select="$selection"/>"</xsl:message>
            </xsl:if>
            <xsl:element name="etf:ref">
                <xsl:attribute name="href" select="concat($serviceUrl, '/collection?offset=', $offset, '&amp;limit=', $limit)"/>
            </xsl:element>
            <xsl:choose>
                <xsl:when test="number($limit) gt 0">
                    <xsl:attribute name="position" select="format-number( $offset div $limit, '#')"/>
                    <xsl:element name="etf:previous">
                        <xsl:attribute name="href" select="concat($serviceUrl, '/collection?offset=', ( $offset - $limit ), '&amp;limit=', $limit)"/>
                    </xsl:element>
                    <xsl:element name="etf:next">
                        <xsl:attribute name="href" select="concat($serviceUrl, '/collection?offset=', ($offset + $limit), '&amp;limit=', $limit)"/>
                    </xsl:element>
                </xsl:when>
            </xsl:choose>


            <xsl:apply-templates select="$subSet"/>

            <!-- additional referencedItems -->
            <xsl:element name="referencedItems">
                <xsl:apply-templates select="*[not(./*[1]/local-name() = $selection)]"/>
            </xsl:element>
        </xsl:element>
    </xsl:template>

    <!-- =============================================================== -->
    <!-- filter itemHash and localPath -->
    <xsl:template match="@* | /etf:DsResultSet//node()[not(self::etf:itemHash or self::etf:localPath)]">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()[not(self::etf:itemHash or self::etf:localPath)]"/>
        </xsl:copy>
    </xsl:template>

    <!-- =============================================================== -->
    <xsl:template
        match="*/etf:testDriver | */etf:tag | */etf:testObject | */etf:testObjectType | */etf:testTaskResult | */etf:executableTestSuite | */etf:testObjectType | */etf:type | */etf:translationTemplateBundle">
        <xsl:element name="{name()}">
            <xsl:variable name="reference" select="@ref"/>
            <xsl:variable name="type" select="concat(upper-case(substring(local-name(),1,1)), substring(local-name(),2))"/>
            <xsl:choose>
                <xsl:when test="key('ids', $reference)">
                    <xsl:attribute name="xsi:type">loc</xsl:attribute>
                    <xsl:attribute name="ref">
                        <xsl:value-of select="$reference"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="xsi:type">ext</xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="concat($serviceUrl, '/', $type, '/', substring-after($reference, 'EID'))"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <!-- =============================================================== -->
    <xsl:template match="*/etf:translationTemplate">
        <xsl:element name="{name()}"
            xpath-default-namespace="http://www.interactive-instruments.de/etf/2.0">
            <xsl:variable name="reference" select="@ref"/>
            <xsl:choose>
                <xsl:when test="key('translationNames', $reference)">
                    <xsl:attribute name="xsi:type">loc</xsl:attribute>
                    <xsl:attribute name="ref">
                        <xsl:value-of select="$reference"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="xsi:type">ext</xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="concat($serviceUrl, '../', substring-after($reference, 'EID'))"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:variable name="parentMapping">
        <entry key="TestModule">ExecutableTestSuite</entry>
        <entry key="TestCase">TestModule</entry>
        <entry key="TestStep">TestCase</entry>
        <entry key="TestAssertion">TestStep</entry>
        <entry key="TestModuleResult">TestTaskResult</entry>
        <entry key="TestCaseResult">TestModuleResult</entry>
        <entry key="TestStepResult">TestCaseResult</entry>
        <entry key="TestAssertionResult">TestStepResult</entry>
        <entry key="TestObjectType">TestObjectType</entry>
        <entry key="TestTask">TestRun</entry>
        <entry key="TestTaskResult">TestRun</entry>
    </xsl:variable>

    <!-- =============================================================== -->
    <xsl:template match="*/etf:parent">
        <xsl:element name="{name()}"
            xpath-default-namespace="http://www.interactive-instruments.de/etf/2.0">
            <xsl:variable name="reference" select="@ref"/>
            <xsl:variable name="parent" select="../local-name()"/>
            <xsl:variable name="type" select="$parentMapping/*[@key=$parent]"/>
            <xsl:choose>
                <xsl:when test="key('ids', $reference)">
                    <xsl:attribute name="xsi:type">loc</xsl:attribute>
                    <xsl:attribute name="ref">
                        <xsl:value-of select="$reference"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="xsi:type">ext</xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="concat($serviceUrl, '/', $type, '/', substring-after($reference, 'EID'))"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>
    
    
    <xsl:variable name="resultedFromMapping">
        <entry key="TestTaskResult">ExecutableTestSuite</entry>
        <entry key="TestModuleResult">TestModule</entry>
        <entry key="TestCaseResult">TestCase</entry>
        <entry key="TestStepResult">TestStep</entry>
        <entry key="TestAssertionResult">TestAssertion</entry>
    </xsl:variable>
    
    <!-- =============================================================== -->
    <xsl:template match="*/etf:resultedFrom">
        <xsl:element name="{name()}"
            xpath-default-namespace="http://www.interactive-instruments.de/etf/2.0">
            <xsl:variable name="reference" select="@ref"/>
            <xsl:variable name="parent" select="../local-name()"/>
            <xsl:variable name="type" select="$parentMapping/*[@key=$parent]"/>
            <xsl:choose>
                <xsl:when test="key('ids', $reference)">
                    <xsl:attribute name="xsi:type">loc</xsl:attribute>
                    <xsl:attribute name="ref">
                        <xsl:value-of select="$reference"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="xsi:type">ext</xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="concat($serviceUrl, '/', $type, '/', substring-after($reference, 'EID'))"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
