var idx = lunr(function () {
    this.ref('id');
    this.field('uri');
    this.field('title', {boost: 10});
    this.field('text');
    this.metadataWhitelist = ['position']
    documents.forEach(function (doc) {
        this.add(doc);
    }, this)
})
