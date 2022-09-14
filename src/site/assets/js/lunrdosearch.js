function preg_quote (str, delimiter) {
    //  discuss at: https://locutus.io/php/preg_quote/
    // original by: booeyOH
    // improved by: Ates Goral (https://magnetiq.com)
    // improved by: Kevin van Zonneveld (https://kvz.io)
    // improved by: Brett Zamir (https://brett-zamir.me)
    // bugfixed by: Onno Marsman (https://twitter.com/onnomarsman)
    //   example 1: preg_quote("$40")
    //   returns 1: '\\$40'
    //   example 2: preg_quote("*RRRING* Hello?")
    //   returns 2: '\\*RRRING\\* Hello\\?'
    //   example 3: preg_quote("\\.+*?[^]$(){}=!<>|:")
    //   returns 3: '\\\\\\.\\+\\*\\?\\[\\^\\]\\$\\(\\)\\{\\}\\=\\!\\<\\>\\|\\:'
  
    return (str + '')
      .replace(new RegExp('[.\\\\+*?\\[\\^\\]$(){}=!<>|:\\' + (delimiter || '') + '-]', 'g'), '\\$&')
  }
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
                        regexp = new RegExp("(" + preg_quote(field) + ")", 'gi')    
                        subtext = subtext.replace(regexp, "<b>$1</b>");
                        output += "<span class='match'>" + subtext + "</span><br />";
                    })
                }
            }
            resultsDiv.innerHTML = output;
        })

    }
}
