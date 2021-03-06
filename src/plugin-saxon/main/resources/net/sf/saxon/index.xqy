xquery version "3.1";
(:~
 : Saxon extension functions
 : @see http://www.saxonica.com/html/documentation/functions/saxon
 :
 :)
module namespace saxon = "http://saxon.sf.net/";

declare namespace jt = "http://saxon.sf.net/java-type";
declare namespace xsl = "http://www.w3.org/1999/XSL/Transform";

declare namespace a = "http://reecedunn.co.uk/xquery/annotations";
declare namespace o = "http://reecedunn.co.uk/xquery/options";

declare %a:since("saxon/pe", "9.2") %a:since("saxon/ee", "9.2") function saxon:adjust-to-civil-time($in as xs:dateTime, $tz as xs:string) as xs:dateTime? external;
declare %a:since("saxon/pe", "9.8") %a:since("saxon/ee", "9.8") function saxon:array-member($value as item()*) as jt:com.saxonica.functions.extfn.ArrayMemberValue external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:base64Binary-to-octets($input as xs:base64Binary) as xs:integer* external;
declare %a:since("saxon/pe", "8.5") %a:since("saxon/ee", "8.5") function saxon:base64Binary-to-string($input as xs:base64Binary?, $encoding as xs:string) as xs:string external;
declare %a:since("saxon/pe", "9.1") %a:since("saxon/ee", "9.1") function saxon:column-number() as xs:integer external;
declare %a:since("saxon/pe", "9.1") %a:since("saxon/ee", "9.1") function saxon:column-number($node as node()) as xs:integer external;
declare %a:since("saxon/pe", "9.0") %a:since("saxon/ee", "9.0") function saxon:compile-query($query as xs:string) as jt:net.sf.saxon.query.XQueryExpression external;
declare %a:since("saxon/pe", "8.5") %a:since("saxon/ee", "8.5") function saxon:compile-stylesheet($stylesheet as document-node()) as jt.net.sf.saxon.PreparedStylesheet external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:decimal-divide($arg1 as xs:decimal?, $arg2 as xs:decimal, $precision as xs:integer) as xs:decimal? external;
declare %a:since("saxon/pe", "8.6.1") %a:since("saxon/ee", "8.6.1") function saxon:deep-equal($arg1 as item()*, $arg2 as item()*, $collation as xs:string?, $flags as xs:string) as xs:boolean external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:discard-document($doc as document-node()) as document-node() external;
declare %a:since("saxon/pe", "9.8") %a:since("saxon/ee", "9.8") function saxon:doc($href as xs:string, $options as map(*)) as document-node() external;
declare %a:since("saxon/pe", "7.2") %a:since("saxon/ee", "7.2") function saxon:eval($stored-expression as jt:net.sf.saxon.functions.Evaluate-PreparedExpression, $param1 as item()*, $param2 as item()*, $param3 as item()*) as xs:double external;
declare %a:since("saxon/pe", "7.2") %a:since("saxon/ee", "7.2") function saxon:evaluate($xpath as xs:string, $param1 as item()*, $param2 as item()*, $param3 as item()*) as item()* external;
declare %a:since("saxon/pe", "8.2") %a:since("saxon/ee", "8.2") function saxon:evaluate-node($node as node()) as item()* external;
declare %a:since("saxon/pe", "7.2") %a:since("saxon/ee", "7.2") function saxon:expression($string as xs:string) as jt:net.sf.saxon.functions.Evaluate-PreparedExpression external;
declare %a:since("saxon/pe", "7.2") %a:since("saxon/ee", "7.2") function saxon:expression($string as xs:string, $ns as element()) as jt:net.sf.saxon.functions.Evaluate-PreparedExpression external;
declare %a:since("saxon/pe", "9.8") %a:since("saxon/ee", "9.8") function saxon:function-annotations($fn as function(*)) as map(*)* external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:get-pseudo-attribute($att as xs:string) as xs:string? external;
declare %a:since("saxon/pe", "9.9") %a:since("saxon/ee", "9.9") function saxon:group-starting($input as item()*, $condition as function(item()) as xs:boolean) as array(item()*)+ external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:has-same-nodes($arg1 as node()*, $arg2 as node()*) as xs:boolean external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:hexBinary-to-octets($input as xs:hexBinary) as xs:integer* external;
declare %a:since("saxon/pe", "8.5") %a:since("saxon/ee", "8.5") function saxon:hexBinary-to-string($input as xs:hexBinary, $encoding as xs:string) as xs:string external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:highest($input as item()*) as item()* external;
declare %a:restrict-until("$key", "saxon/pe", "9.2", "jt:net.sf.saxon.functions.Evaluate-PreparedExpression")
        %a:restrict-until("$key", "saxon/ee", "9.2", "jt:net.sf.saxon.functions.Evaluate-PreparedExpression")
        %a:restrict-since("$key", "saxon/pe", "9.2", "function(*)")
        %a:restrict-since("$key", "saxon/ee", "9.2", "function(*)")
        %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:highest($input as item()*, $key as (jt:net.sf.saxon.functions.Evaluate-PreparedExpression|function(*))) as item()* external;
declare %a:since("saxon/pe", "9.1") %a:since("saxon/ee", "9.1") function saxon:in-summer-time($date as xs:dateTime, $region as xs:string) as xs:boolean external;
declare %a:since("saxon/pe", "8.3") %a:since("saxon/ee", "8.3") %a:until("saxon/pe", "9.4") %a:until("saxon/ee", "9.4") function saxon:index($sequence as item()*, $expression as jt:net.sf.saxon.functions.Evaluate-PreparedExpression, $collation as xs:string) as jt:com.saxonica.expr.IndexedSequence external;
declare %a:since("saxon/pe", "9.5") %a:since("saxon/ee", "9.5") function saxon:index($sequence as item()*, $function as function(item()) as xs:anyAtomicType*) as map(*) external;
declare %a:since("saxon/ee", "9.7") function saxon:is-defaulted($node as node()) as xs:boolean external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:is-whole-number($arg as xs:numeric?) as xs:boolean external;
declare %a:since("saxon/pe", "9.0") %a:since("saxon/ee", "9.0") %a:until("saxon/pe", "9.1") %a:until("saxon/ee", "9.1") function saxon:last-modified() as xs:dateTime? external;
declare %a:restrict-until("$file-or-uri", "saxon/pe", "9.2", "node()")
        %a:restrict-until("$file-or-uri", "saxon/ee", "9.2", "node()")
        %a:restrict-since("$file-or-uri", "saxon/pe", "9.2", "xs:string?")
        %a:restrict-since("$file-or-uri", "saxon/ee", "9.2", "xs:string?")
        %a:since("saxon/pe", "9.0") %a:since("saxon/ee", "9.0") function saxon:last-modified($file-or-uri as (xs:string?|node())) as xs:dateTime? external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:leading($input as item()*) as item() external;
declare %a:restrict-until("$test", "saxon/pe", "9.5", "jt:net.sf.saxon.functions.Evaluate-PreparedExpression")
        %a:restrict-until("$test", "saxon/ee", "9.5", "jt:net.sf.saxon.functions.Evaluate-PreparedExpression")
        %a:restrict-since("$test", "saxon/pe", "9.5", "function(*)")
        %a:restrict-since("$test", "saxon/ee", "9.5", "function(*)")
        %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:leading($input as item()*, $test as (jt:net.sf.saxon.functions.Evaluate-PreparedExpression|function(*))) as item() external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:line-number() as xs:integer external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:line-number($node as node()) as xs:integer external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.1") function saxon:lowest($input as item()*) as item()* external;
declare %a:restrict-until("$key", "saxon/pe", "9.2", "jt:net.sf.saxon.functions.Evaluate-PreparedExpression")
        %a:restrict-until("$key", "saxon/ee", "9.2", "jt:net.sf.saxon.functions.Evaluate-PreparedExpression")
        %a:restrict-since("$key", "saxon/pe", "9.2", "function(*)")
        %a:restrict-since("$key", "saxon/ee", "9.2", "function(*)")
        %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.1") function saxon:lowest($input as item()*, $key as (jt:net.sf.saxon.functions.Evaluate-PreparedExpression|function(*))) as item()* external;
declare %a:since("saxon/pe", "9.9") %a:since("saxon/ee", "9.9") function saxon:map-search($input as item()*, $key as xs:anyAtomicType) as map(*)* external;
declare %a:since("saxon/pe", "9.9") %a:since("saxon/ee", "9.9") function saxon:map-search($input as item()*, $key as xs:anyAtomicType, $condition as function(item()) as xs:boolean) as map(*)* external;
declare %a:since("saxon/pe", "9.8") %a:since("saxon/ee", "9.8") function saxon:message-count($errCode as xs:QName?) as xs:integer external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") %a:deprecated("saxon/pe", "9.2") %a:deprecated("saxon/ee", "9.2") function saxon:namespace-node($prefix as xs:string, $uri as xs:string) as namespace-node() external;
declare %a:since("saxon/pe", "9.9") %a:since("saxon/ee", "9.9") function saxon:object-map($object as jt:java.lang.Object) as map(xs:string, function(*)) external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:octets-to-base64Binary($octets as xs:integer*) as xs:base64Binary external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:octets-to-hexBinary($octets as xs:integer*) as xs:hexBinary external;
declare %a:since("saxon/pe", "7.1") %a:since("saxon/ee", "7.1") %a:deprecated("saxon/pe", "9.6", "fn:parse-xml") %a:deprecated("saxon/ee", "9.6", "fn:parse-xml") function saxon:parse($xml as xs:string) as document-node() external;
declare %a:since("saxon/pe", "9.2") %a:since("saxon/ee", "9.2") function saxon:parse-html($html as xs:string) as document-node() external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") %a:deprecated("saxon/pe", "9.6", "fn:parse-xml") %a:deprecated("saxon/ee", "9.6", "fn:path") function saxon:path() as xs:string external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") %a:deprecated("saxon/pe", "9.6", "fn:parse-xml") %a:deprecated("saxon/ee", "9.6", "fn:path") function saxon:path($node as node()) as xs:string external;
declare %a:since("saxon/pe", "9.9") %a:since("saxon/ee", "9.9") function saxon:pedigree($in as function(*)) as tuple(container: function(*), key: anyAtomicType?, index: xs:integer?)? external;
declare %a:since("saxon/pe", "9.1") %a:since("saxon/ee", "9.1") function saxon:print-stack() as xs:string external;
declare %a:since("saxon/pe", "9.1") %a:since("saxon/ee", "9.1") function saxon:query($name as jt:net.sf.saxon.query.XQueryExpression?) as item()* external;
declare %a:since("saxon/pe", "9.1") %a:since("saxon/ee", "9.1") function saxon:query($name as jt:net.sf.saxon.query.XQueryExpression?, $context-item as item()?) as item()* external;
declare %a:since("saxon/pe", "9.1") %a:since("saxon/ee", "9.1") function saxon:query($name as jt:net.sf.saxon.query.XQueryExpression?, $context-item as item()?, $params as node()*) as item()* external;
declare %a:since("saxon/pe", "9.7") %a:since("saxon/ee", "9.7") function saxon:read-binary-resource($uri as xs:string) as xs:base64Binary external;
declare %a:since("saxon/ee", "9.5") function saxon:schema() as function(*)? external;
declare %a:since("saxon/ee", "9.5") function saxon:schema($kind as xs:string, $name as xs:QName) as function(*)? external;
declare %a:since("saxon/pe", "9.5") %a:since("saxon/ee", "9.5") function saxon:send-mail($emailConfig as map(*), $subject as xs:string, $content as item()) as xs:boolean external;
declare %a:since("saxon/pe", "9.5") %a:since("saxon/ee", "9.5") function saxon:send-mail($emailConfig as map(*), $subject as xs:string, $content as item(), $attachment as item()*) as xs:boolean external;
declare %a:since("saxon/pe", "7.1") %a:since("saxon/ee", "7.1") %a:deprecated("saxon/pe", "9.6", "fn:serialize") %a:deprecated("saxon/ee", "9.6", "fn:serialize") function saxon:serialize($node as node(), $format as xs:string) as xs:string external;
declare %a:since("saxon/pe", "7.1") %a:since("saxon/ee", "7.1") %a:deprecated("saxon/pe", "9.6", "fn:serialize") %a:deprecated("saxon/ee", "9.6", "fn:serialize") function saxon:serialize($node as node(), $format as element(xsl:output)) as xs:string external;
declare %a:since("saxon/pe", "8.9") %a:since("saxon/ee", "8.9") %a:deprecated("saxon/pe", "9.6", "fn:sort") %a:deprecated("saxon/ee", "9.6", "fn:sort") function saxon:sort($seq as item()*) as item()* external;
declare %a:since("saxon/pe", "7.1") %a:since("saxon/ee", "7.1") %a:deprecated("saxon/pe", "9.6", "fn:sort") %a:deprecated("saxon/ee", "9.6", "fn:sort") function saxon:sort($seq as item()*, $sort-key as function(item()) as xs:anyAtomicType) as item()* external;
declare %a:since("saxon/ee", "8.0") function saxon:stream($input as item()*) as item()* external;
declare %a:since("saxon/pe", "8.4") %a:since("saxon/ee", "8.4") function saxon:string-to-base64Binary($in as xs:string?, $encoding as xs:string) as xs:base64Binary? external;
declare %a:since("saxon/pe", "8.4") %a:since("saxon/ee", "8.4") function saxon:string-to-hexBinary($in as xs:string?, $encoding as xs:string) as xs:hexBinary? external;
declare %a:since("saxon/pe", "8.1") %a:since("saxon/ee", "8.1") function saxon:string-to-utf8($name as xs:string) as xs:integer* external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:system-id() as xs:string external;
declare %a:since("saxon/pe", "9.8") %a:since("saxon/ee", "9.8") function saxon:timestamp() as xs:dateTimeStamp external;
declare %a:since("saxon/pe", "8.5") %a:since("saxon/ee", "8.5") function saxon:transform($stylesheet as jt:net.sf.saxon.PreparedStylesheet, $source as node()) as document-node() external;
declare %a:since("saxon/pe", "8.5") %a:since("saxon/ee", "8.5") function saxon:transform($stylesheet as jt:net.sf.saxon.PreparedStylesheet, $source as node(), $params as item()*) as document-node() external;
declare %a:since("saxon/pe", "9.5") %a:since("saxon/ee", "9.5") function saxon:type($item as item()?) as function(*)? external;
declare %a:since("saxon/pe", "8.0") %a:since("saxon/ee", "8.0") function saxon:type-annotation($item as item()?) as xs:QName? external;
declare %a:since("saxon/pe", "9.1") %a:since("saxon/ee", "9.1") function saxon:unparsed-entities($doc as document-node()) as xs:string* external;
declare %a:since("saxon/ee", "9.5") function saxon:validate($node as node()?) as map(xs:string, item()*)? external;
declare %a:since("saxon/ee", "9.5") function saxon:validate($node as node()?, $options as map(xs:string, item()*)) as map(xs:string, item()*)? external;
declare %a:since("saxon/ee", "9.5") function saxon:validate($node as node()?, $options as map(xs:string, item()*), $params as map(xs:string, item()*)) as map(xs:string, item()*)? external;
declare %a:since("saxon/pe", "9.9") %a:since("saxon/ee", "9.9") function saxon:with-pedigree($in as function(*)) as function(*) external;