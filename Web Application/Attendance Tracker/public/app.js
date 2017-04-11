var app = angular.module('myapp',['ngRoute', 'firebase', 'chart.js', 'ngSweetAlert', 'ui.bootstrap']);

var config = {
    apiKey: "AIzaSyBM703Urv9dRC7rNNQ-nxAcONNri_ehGB0",
    authDomain: "classattend.firebaseapp.com",
    databaseURL: "https://classattend.firebaseio.com",
    storageBucket: "classattend.appspot.com",
    messagingSenderId: "194623765186"
};
firebase.initializeApp(config);