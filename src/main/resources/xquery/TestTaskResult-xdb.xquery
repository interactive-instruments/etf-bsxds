import module namespace etfxdb = "http://interactive_instruments.de/etf/etfxdb";

declare default element namespace "http://www.interactive-instruments.de/etf/2.0";
declare namespace etf = "http://www.interactive-instruments.de/etf/2.0";
declare namespace xs = 'http://www.w3.org/2001/XMLSchema';

declare variable $function external;

declare variable $qids external := "";

declare variable $offset external := 0;
declare variable $limit external := 0;
declare variable $levelOfDetail external := 'SIMPLE';

declare function local:get-testTaskResults($offset as xs:integer, $limit as xs:integer) {
    <DsResultSet
    xmlns="http://www.interactive-instruments.de/etf/2.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
    xsi:schemaLocation="http://www.interactive-instruments.de/etf/2.0 file:/../src/main/resources/schema/model/resultSet.xsd">
        <testTaskResults>
            {etfxdb:get-all(db:open('etf-ds')/etf:TestTaskResult, $levelOfDetail, $offset, $limit)}
        </testTaskResults>
    </DsResultSet>
};


declare function local:get-testTaskResult($ids as xs:string*) {
    let $testObjectTypesDb := db:open('etf-ds')/etf:TestObjectType
    let $testObjectsDb := db:open('etf-ds')/etf:TestObject
    let $executableTestSuiteDb := db:open('etf-ds')/etf:ExecutableTestSuite
    let $translationTemplateBundleDb := db:open('etf-ds')/etf:TranslationTemplateBundle
    let $testTaskResultsDb := db:open('etf-ds')/etf:TestTaskResult

    let $testTaskResult := $testTaskResultsDb[@id = $ids]
    let $executableTestSuite := etfxdb:get-executableTestSuites($executableTestSuiteDb, $levelOfDetail, $testTaskResult)

    return
        <DsResultSet
        xmlns="http://www.interactive-instruments.de/etf/2.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/2.0 file:/../src/main/resources/schema/model/resultSet.xsd">
            <executableTestSuites>
                {$executableTestSuite}
            </executableTestSuites>
            <testObjects>
                {etfxdb:get-testObjects($testObjectsDb, $levelOfDetail, $testTaskResult)}
            </testObjects>
            <testObjectTypes>
                {etfxdb:get-testObjectTypes($testObjectTypesDb, $levelOfDetail, $executableTestSuite/etf:supportedTestObjectTypes)}
                {etfxdb:get-testObjectTypes($testObjectTypesDb, $levelOfDetail, $executableTestSuite/etf:consumableResultObjectTypes)}
            </testObjectTypes>
            <translationTemplateBundles>
                {etfxdb:get-translationTemplateBundles($translationTemplateBundleDb, $levelOfDetail, $executableTestSuite)}
            </translationTemplateBundles>
            <testTaskResults>
                {$testTaskResult}
            </testTaskResults>
        </DsResultSet>
};

if ($function = 'byId')
then
    local:get-testTaskResult($qids)
else
    local:get-testTaskResults($offset, $limit)
