const fs = require('fs');
var assert = require("chai").assert;
var sql = require("../../sql").events;






describe('sql()', function() {
    var store = new Array();

    fs.readdir("../../sql_queries/pd_event", function(files) {
        files.forEach( function(a, b, c) {
            store.push(file);
        })
    });
    var tests = [
        {args: [1, 2],       expected: 3},
        {args: [1, 2, 3],    expected: 6},
        {args: [1, 2, 3, 4], expected: 10}
    ];

    tests.forEach(function(test) {
        it('correctly adds ' + test.args.length + ' args', function() {
        var res = add.apply(null, test.args);
        assert.equal(res, test.expected);
        });
    });
});


