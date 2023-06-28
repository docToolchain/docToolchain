var documents = [
<% published_content.eachWithIndex { content, i ->
def text = content.body
if (content.body.contains("<!-- endtoc -->")) {
    text = content.body.split("(?ms)<!-- endtoc -->", 2)[1]
}
text = content.body
                .replaceAll("<[^>]*>", " ")
                .replaceAll("[\r\n \t]", " ")
                .replaceAll(" +", " ")
                .replaceAll('[\\\\]', '\\\\\\\\')
                .replaceAll('"', '\\\\"')
%>
{
    "id": $i,
    "uri": "${content.uri}",
    "menu": "${content.menu}",
    "title": "${content.title}",
    "text": "${text}"
},
<% } %>
];
