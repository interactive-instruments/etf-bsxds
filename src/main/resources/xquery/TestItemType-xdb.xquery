import module namespace etfxdb = "http://interactive_instruments.de/etf/etfxdb";

declare default element namespace "http://www.interactive-instruments.de/etf/2.0";
declare namespace etf = "http://www.interactive-instruments.de/etf/2.0";
declare namespace xs = 'http://www.w3.org/2001/XMLSchema';

declare variable $function external;

declare variable $qids external := "";

declare variable $offset external := 0;
declare variable $limit external := 0;
declare variable $levelOfDetail external := 'SIMPLE';
declare variable $fields external := '*';

declare function local:get-testitemtypes($offset as xs:integer, $limit as xs:integer) {
        <DsResultSet
        xmlns="http://www.interactive-instruments.de/etf/2.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/2.0 https://services.interactive-instruments.de/etf/schema/model/resultSet.xsd">
            <testItemTypes>
                {etfxdb:get-all(db:open('etf-ds')/etf:TestItemType, $offset, $limit, $fields)}
            </testItemTypes>
        </DsResultSet>
};

declare function local:get-testitemtype($ids as xs:string*) {
    let $testItemType := db:open('etf-ds')/etf:TestItemType[@id = $ids]
    return
        <DsResultSet
        xmlns="http://www.interactive-instruments.de/etf/2.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/2.0 https://services.interactive-instruments.de/etf/schema/model/resultSet.xsd">
            <testItemTypes>
                {$testItemType}
            </testItemTypes>
        </DsResultSet>
};

if ($function = 'byId')
then
    local:get-testitemtype($qids)
else
    local:get-testitemtypes($offset, $limit)
