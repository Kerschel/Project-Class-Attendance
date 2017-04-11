Date.prototype.addDays = function(days){
    var myDate = new Date(this.valueOf());
    myDate.setDate(myDate.getDate() + days);
    return myDate;
}