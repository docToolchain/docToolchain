function dosearch(element) {
    //only start search if we have more than 3 characters
    if (element.value.length>=3) {
        var resultsDiv = document.querySelector("#results");
        output = "";
        var results = idx.search(element.value);
        if (results.length == 0) {
            results = idx.search(element.value+"*");
        }
        if (results.length == 0) {
            results = idx.search("*"+element.value+"*");
        }
        if (results.length == 0) {
            results = idx.search(element.value+"~1");
        }
        var lastMenu = ""
        var lastTitle = ""
        results.forEach(function (it) {
            var doc = documents[it.ref];
            output += "<h4><a href='" + doc.uri + "#:~:text=" + element.value + "'>" + doc.title + "</a></h4>";
            for( var field in it.matchData.metadata) {
                var matches = it.matchData.metadata[field];
                if (matches['text']) {
                    matches['text']['position'].forEach(function (pos) {
                        var subtext = doc.text.substring(pos[0]-50, pos[0]+pos[1]+50);
                        if (pos[0]>0) {
                            subtext = subtext.replace(new RegExp(/^[^ ]*/, "i"), "...");
                        }
                        subtext = subtext.replace(new RegExp(/[^ ]*$/, "i"), "...");
                        var regexp = new RegExp(field, "gi");
                        subtext = subtext.replace(regexp, "<b>$&</b>");
                        output += "<span class='match'>" + subtext + "</span><br />";
                    })
                }
            }
            resultsDiv.innerHTML = output;
        })

    }
}
