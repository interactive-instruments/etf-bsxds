import module namespace etfxdb = "http://interactive_instruments.de/etf/etfxdb";

declare default element namespace "http://www.interactive-instruments.de/etf/2.0";
declare namespace etf = "http://www.interactive-instruments.de/etf/2.0";
declare namespace xs = 'http://www.w3.org/2001/XMLSchema';

declare variable $function external;

declare variable $qids external := "";

declare variable $offset external := 0;
declare variable $limit external := 0;
declare variable $levelOfDetail external := 'SIMPLE';

declare function local:get-testobjecttypes($offset as xs:integer, $limit as xs:integer) {
        <DsResultSet
        xmlns="http://www.interactive-instruments.de/etf/2.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/2.0 http://services.interactive-instruments.de/etf/schema/model/resultSet.xsd">
            <testObjectTypes>
                {etfxdb:get-all(db:open('etf-ds')/etf:TestObjectType, $offset, $limit)}
            </testObjectTypes>
        </DsResultSet>
};

declare function local:get-testobjecttype($ids as xs:string*) {
    let $testObjectTypeDb := db:open('etf-ds')/etf:TestObjectType
    let $testObjectType := $testObjectTypeDb[@id = $ids]
    return
        <DsResultSet
        xmlns="http://www.interactive-instruments.de/etf/2.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/2.0 http://services.interactive-instruments.de/etf/schema/model/resultSet.xsd">
            <testObjectTypes>
                {$testObjectType}
            </testObjectTypes>
        </DsResultSet>
};

if ($function = 'byId')
then
    local:get-testobjecttype($qids)
else
    local:get-testobjecttypes($offset, $limit)
