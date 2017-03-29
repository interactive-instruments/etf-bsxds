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

declare function local:get-tags($offset as xs:integer, $limit as xs:integer) {
        <DsResultSet
        xmlns="http://www.interactive-instruments.de/etf/2.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/2.0 https://services.interactive-instruments.de/etf/schema/model/resultSet.xsd">
            <tags>
                {etfxdb:get-all(db:open('etf-ds')/etf:Tag, $offset, $limit, $fields)}
            </tags>
        </DsResultSet>
};

declare function local:get-tag($ids as xs:string*) {
    let $tag := db:open('etf-ds')/etf:Tag[@id = $ids]
    return
        <DsResultSet
        xmlns="http://www.interactive-instruments.de/etf/2.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/2.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/2.0 https://services.interactive-instruments.de/etf/schema/model/resultSet.xsd">
            <tags>
                {$tag}
            </tags>
        </DsResultSet>
};

if ($function = 'byId')
then
    local:get-tag($qids)
else
    local:get-tags($offset, $limit)
