xquery version "1.0";
(:~
 : eXist default static context
 :
 : @see http://
 :)

declare default element namespace "";
declare default function namespace "http://www.w3.org/2005/xpath-functions"; (: = 'fn:' :)

(: XQuery 1.0 :)
declare namespace xml = "http://www.w3.org/XML/1998/namespace";
declare namespace xs = "http://www.w3.org/2001/XMLSchema";
declare namespace xsi = "http://www.w3.org/2001/XMLSchema-instance";
declare namespace fn = "http://www.w3.org/2005/xpath-functions";
declare namespace local = "http://www.w3.org/2005/xquery-local-functions";

(: XQuery 3.0 :)
declare namespace math = "http://www.w3.org/2005/xpath-functions/math";

(: XQuery 3.1 :)
declare namespace map = "http://www.w3.org/2005/xpath-functions/map";
declare namespace array = "http://www.w3.org/2005/xpath-functions/array";

(: eXist :)
declare namespace compression = "http://exist-db.org/xquery/compression";
declare namespace console = "http://exist-db.org/xquery/console";
declare namespace contentextraction = "http://exist-db.org/xquery/contentextraction";
declare namespace counter = "http://exist-db.org/xquery/counter";

()