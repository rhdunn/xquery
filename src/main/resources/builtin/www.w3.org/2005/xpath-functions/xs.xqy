xquery version "3.1";

(:~
 : XPath and XQuery Functions and Operators: Constructor Functions
 :
 : @see https://www.w3.org/TR/2007/REC-xpath-functions-20070123/
 : @see https://www.w3.org/TR/2010/REC-xpath-functions-20101214/
 : @see https://www.w3.org/TR/2014/REC-xpath-functions-30-20140408/
 : @see https://www.w3.org/TR/2016/CR-xpath-functions-31-20170321/
 :)
module  namespace xs = "http://www.w3.org/2001/XMLSchema";
declare namespace a  = "http://reecedunn.co.uk/xquery/annotations";

(: XSD 1.0 => XQuery 1.0
 : XSD 1.1 => XQuery 3.0+
 :)

declare %a:since("XSD", "1.0") function xs:ENTITY($arg as xs:anyAtomicType?) as xs:ENTITY? external;
declare %a:since("XSD", "1.0") function xs:ID($arg as xs:anyAtomicType?) as xs:ID? external;
declare %a:since("XSD", "1.0") function xs:IDREF($arg as xs:anyAtomicType?) as xs:IDREF? external;
declare %a:since("XSD", "1.0") function xs:NCName($arg as xs:anyAtomicType?) as xs:NCName? external;
declare %a:since("XSD", "1.0") function xs:NMTOKEN($arg as xs:anyAtomicType?) as xs:NMTOKEN? external;
declare %a:since("XSD", "1.0") function xs:Name($arg as xs:anyAtomicType?) as xs:Name? external;
(: NOTE: $arg is required in XQuery 1.0 :)
declare %a:since("XSD", "1.0") function xs:QName($arg as xs:anyAtomicType?) as xs:QName? external;

declare %a:since("XSD", "1.0") function xs:anyURI($arg as xs:anyAtomicType?) as xs:anyURI? external;
declare %a:since("XSD", "1.0") function xs:base64Binary($arg as xs:anyAtomicType?) as xs:base64Binary? external;
declare %a:since("XSD", "1.0") function xs:boolean($arg as xs:anyAtomicType?) as xs:boolean? external;
declare %a:since("XSD", "1.0") function xs:byte($arg as xs:anyAtomicType?) as xs:byte? external;
declare %a:since("XSD", "1.0") function xs:date($arg as xs:anyAtomicType?) as xs:date? external;
declare %a:since("XSD", "1.0") function xs:dateTime($arg as xs:anyAtomicType?) as xs:dateTime? external;
declare %a:since("XSD", "1.0") function xs:dayTimeDuration($arg as xs:anyAtomicType?) as xs:dayTimeDuration? external;
declare %a:since("XSD", "1.0") function xs:decimal($arg as xs:anyAtomicType?) as xs:decimal? external;
declare %a:since("XSD", "1.0") function xs:double($arg as xs:anyAtomicType?) as xs:double? external;
declare %a:since("XSD", "1.0") function xs:duration($arg as xs:anyAtomicType?) as xs:duration? external;
declare %a:since("XSD", "1.0") function xs:float($arg as xs:anyAtomicType?) as xs:float? external;
declare %a:since("XSD", "1.0") function xs:gDay($arg as xs:anyAtomicType?) as xs:gDay? external;
declare %a:since("XSD", "1.0") function xs:gMonth($arg as xs:anyAtomicType?) as xs:gMonth? external;
declare %a:since("XSD", "1.0") function xs:gMonthDay($arg as xs:anyAtomicType?) as xs:gMonthDay? external;
declare %a:since("XSD", "1.0") function xs:gYear($arg as xs:anyAtomicType?) as xs:gYear? external;
declare %a:since("XSD", "1.0") function xs:gYearMonth($arg as xs:anyAtomicType?) as xs:gYearMonth? external;
declare %a:since("XSD", "1.0") function xs:hexBinary($arg as xs:anyAtomicType?) as xs:hexBinary? external;
declare %a:since("XSD", "1.0") function xs:int($arg as xs:anyAtomicType?) as xs:int? external;
declare %a:since("XSD", "1.0") function xs:integer($arg as xs:anyAtomicType?) as xs:integer? external;
declare %a:since("XSD", "1.0") function xs:language($arg as xs:anyAtomicType?) as xs:language? external;
declare %a:since("XSD", "1.0") function xs:long($arg as xs:anyAtomicType?) as xs:long? external;
declare %a:since("XSD", "1.0") function xs:negativeInteger($arg as xs:anyAtomicType?) as xs:negativeInteger? external;
declare %a:since("XSD", "1.0") function xs:nonNegativeInteger($arg as xs:anyAtomicType?) as xs:nonNegativeInteger? external;
declare %a:since("XSD", "1.0") function xs:nonPositiveInteger($arg as xs:anyAtomicType?) as xs:nonPositiveInteger? external;
declare %a:since("XSD", "1.0") function xs:normalizedString($arg as xs:anyAtomicType?) as xs:normalizedString? external;
declare %a:since("XSD", "1.0") function xs:positiveInteger($arg as xs:anyAtomicType?) as xs:positiveInteger? external;
declare %a:since("XSD", "1.0") function xs:short($arg as xs:anyAtomicType?) as xs:short? external;
declare %a:since("XSD", "1.0") function xs:string($arg as xs:anyAtomicType?) as xs:string? external;
declare %a:since("XSD", "1.0") function xs:time($arg as xs:anyAtomicType?) as xs:time? external;
declare %a:since("XSD", "1.0") function xs:token($arg as xs:anyAtomicType?) as xs:token? external;
declare %a:since("XSD", "1.0") function xs:unsignedByte($arg as xs:anyAtomicType?) as xs:unsignedByte? external;
declare %a:since("XSD", "1.0") function xs:unsignedInt($arg as xs:anyAtomicType?) as xs:unsignedInt? external;
declare %a:since("XSD", "1.0") function xs:unsignedLong($arg as xs:anyAtomicType?) as xs:unsignedLong? external;
declare %a:since("XSD", "1.0") function xs:unsignedShort($arg as xs:anyAtomicType?) as xs:unsignedShort? external;
declare %a:since("XSD", "1.0") function xs:untypedAtomic($arg as xs:anyAtomicType?) as xs:untypedAtomic? external;
declare %a:since("XSD", "1.0") function xs:yearMonthDuration($arg as xs:anyAtomicType?) as xs:yearMonthDuration? external;
declare %a:since("XSD", "1.1") function xs:dateTimeStamp($arg as xs:anyAtomicType?) as xs:dateTimeStamp? external;
