app.factory('Auth', ['$firebaseAuth', function($firebaseAuth){
    var factory = {
    	state: $firebaseAuth
    };
    factory.getState = function(){
    	return $firebaseAuth()
    }

    return factory;
}]);
app.factory('GeneratePDF', function(){
    var doc;
    var currentPosition = 20;
    const INDENT_VALUE = 20;
    var factory = {};
    factory.init = function(orientation){
        doc = new jsPDF();
    }
    factory.addText = function(text){
        doc.text(text, INDENT_VALUE, currentPosition);
        currentPosition = currentPosition + 10;
    }
    factory.addTable = function(columns, rows){
        myFontSize = 10;
        doc.autoTable(columns, rows,
        {startY: currentPosition}, {
            styles: {
                fontSize: 20,
                lineColor: 200,
                textColor: 20
            },
            margin: {top: 45}
        });
        currentPosition = doc.autoTable.previous.finalY + 10;
    }
    factory.addPage = function(){
        doc.addPage();
        currentPosition = 20;
    }
    factory.addLineBreak = function(){
        currentPosition = currentPosition + 10;
    }
    factory.generatePDF = function(){
        doc.output('datauri');
    }
    factory.downloadPDF = function(){
        doc.save('Attendance.pdf');
    }
    return factory;
})