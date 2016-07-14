declare default element namespace "http://www.interactive-instruments.de/etf/1.0";
declare namespace etf = "http://www.interactive-instruments.de/etf/1.0";
declare namespace xs = 'http://www.w3.org/2001/XMLSchema';

declare variable $function external;

declare variable $qids external := "";

declare variable $offset external := 0;
declare variable $limit external := 0;
declare variable $replacedItems external := 'HIDE';

declare function local:get-translationtemplatebundles($offset as xs:integer, $limit as xs:integer) {
    let $testRunTemplateDb :=
        for $item in db:open('etf-ds')/etf:TranslationTemplateBundle
        order by $item/etf:label ascending
        return
            $item
    let $testRunTemplates := $testRunTemplateDb[position() > $offset and position() <= $offset + $limit]
    return
        <DataStorageResult
        xmlns="http://www.interactive-instruments.de/etf/1.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/1.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/1.0 file:/../src/main/resources/schema/model/storage.xsd">
            <translationTemplateBundles>
                {$testRunTemplates}
            </translationTemplateBundles>
        </DataStorageResult>
};

declare function local:get-translationtemplatebundle($ids as xs:string*) {
    let $testRunTemplateDb := db:open('etf-ds')/etf:TranslationTemplateBundle
    let $testRunTemplate := $testRunTemplateDb[@id = $ids]
    return
        <DataStorageResult
        xmlns="http://www.interactive-instruments.de/etf/1.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:etf="http://www.interactive-instruments.de/etf/1.0"
        xsi:schemaLocation="http://www.interactive-instruments.de/etf/1.0 file:/../src/main/resources/schema/model/storage.xsd">
            <translationTemplateBundles>
                {$testRunTemplate}
            </translationTemplateBundles>
        </DataStorageResult>
};

if ($function = 'byId')
then
    local:get-translationtemplatebundle($qids)
else
    local:get-translationtemplatebundles($offset, $limit)
