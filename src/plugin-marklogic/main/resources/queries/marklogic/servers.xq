(:
 : Copyright (C) 2019, 2021 Reece H. Dunn
 :
 : Licensed under the Apache License, Version 2.0 (the "License");
 : you may not use this file except in compliance with the License.
 : You may obtain a copy of the License at
 :
 : http://www.apache.org/licenses/LICENSE-2.0
 :
 : Unless required by applicable law or agreed to in writing, software
 : distributed under the License is distributed on an "AS IS" BASIS,
 : WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 : See the License for the specific language governing permissions and
 : limitations under the License.
 :)
xquery version "1.0-ml";
declare namespace o = "http://reecedunn.co.uk/xquery/options";
declare option o:implementation "marklogic/6.0";

(: Return the servers on the MarkLogic server. :)

("(none)", "(none)"),

let $databases := map:map()
let $_ :=
    for $id in xdmp:servers()
    let $server := xdmp:server-name($id)
    let $database := xdmp:server-database($id) ! xdmp:database-name(.)
    where exists($database)
    return map:put($databases, $database, (map:get($databases, $database), $server))

for $id in xdmp:databases()
let $database := xdmp:database-name($id)
let $servers := map:get($databases, $database)
return (
    for $server in $servers return ($server, $database),
    ("(none)", $database)
)
