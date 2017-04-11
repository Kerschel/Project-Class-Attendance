app.config(['$routeProvider', function($routeProvider){
	$routeProvider
		.when("/", {
			templateUrl: 'templates/home.html',
			controller: 'MainCtrl'
		}).when("/courses", {
			templateUrl: 'templates/courses.html',
			controller: 'CoursesCtrl'
		}).when("/reports", {
			templateUrl: 'templates/reports.html',
			controller: 'ReportsCtrl'
		}).when("/students", {
			templateUrl: 'templates/students.html',
			controller: 'StudentsCtrl'
		}).when("/signup", {
			templateUrl: 'templates/signup.html',
			controller: 'SignupCtrl'
		}).when("/login", {
			templateUrl: 'templates/login.html',
			controller: 'LoginCtrl'
		}).when("/allcoursesreport", {
			templateUrl: 'templates/allcoursesreport.html',
			controller: 'allCoursesCtrl'
		}).when("/coursereport", {
			templateUrl: 'templates/courseReport.html',
			controller: 'courseReportCtrl'
		}).when("/studentreport", {
			templateUrl: 'templates/studentReport.html',
			controller: 'studentReportCtrl'
		}).otherwise({
			templateUrl: 'templates/home.html',
			controller: 'MainCtrl'
		});
}])
.run(function($rootScope, $location, Auth){
	$rootScope.location = $location;
	$rootScope.navCheck = function(){
		if($location.path() == '/login' || $location.path() == '/signup'){
			return false;
		}
		else return true;
	}
	$rootScope.$on('$routeChangeStart', function(event, next, current){
		if(Auth.getState().$getAuth()){
			console.log("Logged In");
		}
		else{
			if($location.path() != '/signup'){
				$location.path('/login');
			}
			console.log('Not Logged On');
		}
	});
});
app.controller('LoginCtrl', ['$scope', 'SweetAlert', 'Auth', '$location', function($scope, SweetAlert, Auth, $location){
	$scope.email="";
	$scope.password="";
	$scope.login = function(){
		Auth.getState().$signInWithEmailAndPassword($scope.email, $scope.password).then(function(){
			$location.path('/');
		}).catch(function(error){
            var errorCode = error.code;
            var errorMessage = error.message;
			SweetAlert.show({
				title: 'Error',
				text: error.message,
				type: 'info'
			});
        });
	}
}]);
app.controller('SignupCtrl', ['$scope', 'Auth', '$location', 'SweetAlert', function($scope, Auth, $location, SweetAlert){
	$scope.email="";
	$scope.password="";
	$scope.login = function(){
		Auth.getState().$createUserWithEmailAndPassword($scope.email, $scope.password).then(function(){
			$location.path('/');
		}).catch(function(error){
            var errorCode = error.code;
            var errorMessage = error.message;
			alert(errorMessage);
            console.log(errorMessage);
        });
	}
}]);
app.controller('MainCtrl', ['$scope', 'SweetAlert', function($scope, SweetAlert){
	$scope.title = "Hello World";
}]);

app.controller('CoursesCtrl', ['$scope', '$firebaseObject', function($scope, $firebaseObject, $filter){
	$scope.myFile=[];
	$scope.upload = false;
	$scope.submitted = false;
	$scope.allStudents = [];

	var ref = firebase.database().ref('Courses');
	$scope.courses = [];
	ref.on('value', function(snapshot){
		$scope.courses = snapshot.val();//Retrieves all courses being offered this semester for the auto complete Text View
	});

	$scope.selectedCourse = function(){
		$scope.upload = true;//Once a course is selected, the $scope.upload variable is set to true so that additional content will be displayed in the web view. 
	};
	$scope.uploadStudentData = function(){
		var allStudents = [];
		for(student in $scope.allStudents){
			temp = $scope.allStudents[student];
			delete temp.$$hashKey;
			allStudents.push(temp);
		}
		firebase.database().ref('Students/' + $scope.course).set(allStudents);
	}
	//The following function parses data retrieved from the csv file, extracts the required student data and stores in an indexed array of javascript objects. 
	$scope.processStudentData = function(data){
		var studentsRef = firebase.database().ref()
		$scope.allStudents = [];//All Students is an indexed array that will store associative arrays that contain the student Data. 
		var allLines = data.split(/\r\n|\n/);
		for(var i = 1; i < allLines.length; i++){
			var studentText = allLines[i].split(',');//Use commas as delimeters to seperate data retrieved from the csv file.  
			var studentObj = {};
			studentObj['studentID'] = studentText[5]; 
			studentObj['firstName'] = studentText[7];
			studentObj['lastName'] = studentText[6];
			$scope.allStudents.push(studentObj);	
		}
		$scope.submitted = true;
		//Because the reader.onload event is outside of the anular context, we must manually invoke a digest cycle by using $scope.apply()
		$scope.$apply();
	}
	$scope.handler = function(e, files){
		var reader = new FileReader();
		reader.onload = function(){
			var string = reader.result;//All data is extracted from the csv file. 
			//The process Student data function is called to parse the data retrieved from the csv file. 
			$scope.processStudentData(string);
		}
		//Only one file should be submitted so this file can be retirved from position 0 in the files array that was passed to the handler function. 
		reader.readAsText(files[0]);
	}
	$scope.data = $firebaseObject(ref);

}]);
app.controller('StudentsCtrl', ['$scope', '$firebaseObject', 'SweetAlert', function($scope, $firebaseObject, SweetAlert){
	const MONTHS = ['January', 'Febuary', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
	var ref = firebase.database().ref('Courses');
	var studentRef = firebase.database().ref('Students');
	var d = new Date();
	var totalClassesAttended = 0;
	var totalClasses = 0;
	var totalClassesAbsent = 0;
	$scope.labels = ["Absent", "Present"];
	$scope.series = ['Absent ', 'Present'];
	$scope.graphColors = ['blue', 'green'];
	$scope.graphData = [50, 50];
	$scope.courses = [];
	$scope.selectedCourse = '';
	$scope.studentNumber = "";
	$scope.studentList = [];
	$scope.studentMap = {};
	$scope.startDate = new Date();
	$scope.startDate.setFullYear(d.getFullYear(), 2, 1);
	$scope.endDate = new Date();
	$scope.options = {legend: {display: true}};
	$scope.showGraph = false;
	//Used to retrieve data from the cloud and populate the select element. 
	ref.on('value', function(snapshot){
		temp = snapshot.val();
		for (key in temp){
			$scope.courses.push(temp[key]);
		}
	});
	//The following function calculates attendance data for a specific student within a given range. 
	$scope.generateGraph = function(){
		if(!$scope.studentMap[$scope.studentNumber]){
			SweetAlert.show({
				title: 'Error',
				text: 'No Data exists for this Student ID',
				type: 'info'
			});
			return;
		}
		if($scope.studentNumber.localeCompare('') == 0 || $scope.selectedCourse.localeCompare('') == 0){
			SweetAlert.show({
				title: 'Error',
				text: 'Please Ensure all fields are completed',
				type: 'info'
			});
			return;
		}
		$scope.showGraph = true;
		var attendanceRef = firebase.database().ref(d.getFullYear() + '/' + $scope.selectedCourse);
		attendanceRef.on('value', function(snapshot){
			totalClasses = 0;
			totalClassesAttended = 0;
			currentDate = new Date($scope.startDate);
			//The loop repeats for every day within the specified range. 
			while(currentDate <= $scope.endDate){
				day = currentDate.getDate();
				if(snapshot.child(MONTHS[currentDate.getMonth()] + '/' + currentDate.getDate() + '/').val()){
					totalClasses++;//We increment the total number of classes
					identification = snapshot.child(MONTHS[currentDate.getMonth()] + '/' + currentDate.getDate() + '/' + $scope.studentNumber).val();
					if(identification != null) totalClassesAttended++;//The classattend variable is only incremented when their attendance is found. 
				}
				currentDate = currentDate.addDays(1);
			}
		totalClassesAbsent = totalClasses - totalClassesAttended
  		$scope.graphData = [totalClassesAbsent, totalClassesAttended];
		});
		$scope.myData = $firebaseObject(attendanceRef);
	}
	var listRef = firebase.database().ref('Students');
	//The following event Listener will be used to retrieve data on all students registered for courses. 
	listRef.once('value').then(function(allCourseSnapshot){
		allCourseSnapshot.forEach(function(courseSnapshot){
			courseSnapshot.forEach(function(studentSnapshot){
				var studentID = studentSnapshot.val().studentID;//we extract the student ID from the snapshot recieved from firebase. 
				if($scope.studentMap[studentID] == null){//If we have not already encountered this Student ID. We must add it to the list of distinct students. 
					$scope.studentMap[studentID] = studentSnapshot.val();
					$scope.studentMap[studentID].courseList = [];//Each student object will be given a course list attribute which is an indexed array that will be used to store a list of courses registered for the the corresponding student. 
					$scope.studentMap[studentID].courseList.push(courseSnapshot.key);//We add the current course under which the student is registered to the list of courses. 
					$scope.studentList.push(studentID)//we add this new student to the list of student Objects. 
				}
				else $scope.studentMap[studentID].courseList.push(courseSnapshot.key);
			})
		})
	})
	$scope.data = $firebaseObject(ref);
}]);
app.controller('ReportsCtrl', ['$scope', '$firebaseObject', function($scope, $firebaseObject){
	$scope.message = "Reports";
	$scope.upload = false;

	var ref = firebase.database().ref('Courses');
	$scope.courses = [];

	ref.on('value', function(snapshot){
		//console.log(snapshot.val());
		$scope.courses = snapshot.val();
	});

	$scope.selectedCourse = function(){
		console.log($scope.course);
		$scope.upload = true;
	};

	$scope.data = $firebaseObject(ref);
}]);
app.controller('allCoursesCtrl', ['$scope', '$firebaseObject', 'GeneratePDF', function($scope, $firebaseObject, GeneratePDF){
	const DAYS_OF_WEEK = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
	$scope.message = 'All Courses Reports';
	var d = new Date();
	var attendanceRef = firebase.database().ref(d.getFullYear());
	var allClassesRef = firebase.database().ref('class');
	var classListRef = firebase.database().ref('Students');
	var allClassesData = {};//This object will be used to store data on all classes for all courses on the database. A firebase unique key will be used to identify each class.
	var allClassNumStudents = {};//This object will be used to store the number of students registered for  each class. 
	var classPerSemester = {};
	var totalAveragePerSemester = {};//The totalAveragePerSemester array is an associative array that will be used to map the unique key generated by firebasse for each individual class to each individual class object. 
	var tableData = {};//The table data array is an associative array that will be used to map course codes to indexed arrays which contain a list of classes of that course code. 
	var preparePDF = function(){
		 var count = 0;//This count variable will keep track of the number of tables on the current page of the pdf.
		 var heading = '';//The heading tring variable will be used to construct headings for the tables of the all Courses Report. 
		 GeneratePDF.init();
		 GeneratePDF.addText('All Courses Report');
		 for(key in tableData){//This loop will output data to the pdf for  every course in the database.
			 var columns = [];//The headings array will store an array of strings which contain column headings for the table that will be provided on the pdf
			 var rows = [];//The rows array is a 2D Aarray that will be used to store dada that will be outputted to the pdf. Each row of the pdf will contain a row of data for the table in the pdf. 
			 var myRows = [];
			 for(i = 0; i < tableData[key].length; i++){
				 var rowValue = '' + totalAveragePerSemester[tableData[key][i]];
				 if(rowValue.localeCompare("No Data") != 0)rowValue += ' %';
				 heading = allClassesData[tableData[key][i]].dayOfWeek + ' ' + allClassesData[tableData[key][i]].startTime + ' - ' + allClassesData[tableData[key][i]].endTime;
				 columns.push(heading);
				 rows.push(rowValue);
				 heading = '';
			 }
			 myRows.push(rows);
			 GeneratePDF.addText(key);
			 GeneratePDF.addTable(columns, myRows);
			 count = count + 1;
			 //We would like ti limit the amount of tables on the page to 5 tables only.
			 if (count > 5){//If we have placed five (5) tables on the current page, we add a blank page to the document and continue. 
				 GeneratePDF.addPage();
				 count = 0;
			 }
			 GeneratePDF.addLineBreak();
		 }
	}
	$scope.viewPDF = function(){
		preparePDF();
		GeneratePDF.generatePDF();
	}
	$scope.downloadPDF = function(){
		preparePDF();
		GeneratePDF.downloadPDF();
	}
	allClassesRef.once('value').then(function(classSnapshot){
		allClassesData = classSnapshot.val();
		classSnapshot.forEach(function(childSnapshot){
			var currentCourse = childSnapshot.val().courseCode;
			if(tableData[currentCourse] == null){//If we Have Not already Encountered the current Course Code.
				tableData[currentCourse] = [];
				tableData[currentCourse].push(childSnapshot.key);
			}
			else {//We Have already Encountered the present Course Code.
				tableData[currentCourse].push(childSnapshot.key);
			}
			totalAveragePerSemester[childSnapshot.key] = 0;
			classListRef.child(currentCourse).once('value').then(function(listSnapshot){
				allClassNumStudents[childSnapshot.key] = listSnapshot.numChildren();//we retrieve the number of students registered for this courses. 
			});
			attendanceRef.child(currentCourse).once('value').then(function(courseSnapshot){//we parse the attendance data. 
				var courseAttendancePercentage = 0;
				var numClasses = 0;//We must keep track of the nuber of classes encountered. 
				courseSnapshot.forEach(function(monthSnapshot){
					monthSnapshot.forEach(function(daySnapshot){
						var attendancePercentage;
						var myDate = new Date();
						var dateString = monthSnapshot.key + ' ' + daySnapshot.key + ', ' + d.getFullYear();
						myDate = new Date(dateString);
						if(childSnapshot.val().dayOfWeek.localeCompare(DAYS_OF_WEEK[myDate.getDay()]) == 0){
							attendancePercentage = daySnapshot.numChildren() / allClassNumStudents[childSnapshot.key];//the number of students that attended the class tha day vs the total number of students that registered. 
							courseAttendancePercentage += attendancePercentage;
							numClasses = numClasses + 1;
						}
					})
				})
				courseAttendancePercentage = courseAttendancePercentage / numClasses;//We calculate the average attendance for the specific course
				if(isNaN(courseAttendancePercentage))//The result of the above line may give NAN if no attendance has been taken for that course. 
					totalAveragePerSemester[childSnapshot.key] = "No Data";//We can therefore indicate to the user that no data has yet been taken, 
				else {
					courseAttendancePercentage = courseAttendancePercentage * 100;
					courseAttendancePercentage = courseAttendancePercentage.toFixed(2);//We round the percentage to 2 decimal places.  
					totalAveragePerSemester[childSnapshot.key] = courseAttendancePercentage;
				}
			})
		});
	});
}]);
app.controller('courseReportCtrl', ['$scope', 'SweetAlert', '$firebaseObject', 'GeneratePDF', function($scope, SweetAlert, $firebaseObject, GeneratePDF){
	$scope.dataCapture = false;//The data capture variable will be used for displaying the form in the course Report page. Once this variable is set to false,the form will be displayed to the user. Once it is true, the form will not be displayed.
	$scope.courseCode = '';
	$scope.courses = [];
	$scope.attendanceData = [];
	$scope.labels = [];
	$scope.colors = [];
	var myDate = new Date();
	var ref = firebase.database().ref('Courses');
	$scope.classList = [];
	ref.on('value', function(snapshot){
		temp = snapshot.val();
		for (key in temp){
			$scope.courses.push(temp[key]);
		}
	});
	$scope.downloadPDF = function(){
		GeneratePDF.downloadPDF();
	}
	$scope.viewPDF = function(){
		GeneratePDF.generatePDF();
	}
	$scope.preparePDF = function(){
		var pageHeading = 'Attendance Data for ' + $scope.courseCode;
		var columns = ['Student ID', 'First Name', 'Last Name', 'Attendance'];//The columns array represent the headings that will be displayed in the table on the pdf document.
		var rows = [];
		var singleRow;
		var rowAttendance = '';
		for (i = 0; i < $scope.classList.length; i++){
			singleRow = [];
			rowAttendance = '' + $scope.attendanceData[i] + '%';
			singleRow.push($scope.classList[i].studentID);
			singleRow.push($scope.classList[i].firstName);
			singleRow.push($scope.classList[i].lastName);
			singleRow.push(rowAttendance);
			rows.push(singleRow);
		}
		GeneratePDF.init();
		GeneratePDF.addText(pageHeading);
		GeneratePDF.addTable(columns, rows);
	}
	$scope.generateData = function(){
		$scope.dataCapture = true;
		var classListRef = firebase.database().ref('Students');
		var attendanceRef = firebase.database().ref(myDate.getFullYear()).child($scope.courseCode);
		classListRef.child($scope.courseCode).once('value').then(function(listSnapshot){
			if(!listSnapshot.exists()){//We must check to see if data exists for the current Course being entered by the user.
				//If no data exists, an error message is displayed. 
				SweetAlert.show({
					title: 'Error',
					text: 'No Data for this Course Exists',
					type: 'info'
				});
				$scope.courseCode = '';
				return;
			}
			$scope.classList = listSnapshot.val();
			for(i = 0; i < $scope.classList.length; i++){
				$scope.labels[i] = '' + $scope.classList[i].studentID;//Retrieve all student ID's from the class List and add it to the labels array. These labels will be used in the graph to be generated.
				$scope.attendanceData[i] = 0;//Initialize att attendance values to 0. These values will be incremented once their attendance is processed.  
				$scope.colors[i] = '#821DB8';
			}
			attendanceRef.once('value').then(function(attendanceSnapshot){
				var totalNumClasses = 0;
				attendanceSnapshot.forEach(function(monthSnapshot){//repeat for every month snapshot. 
					monthSnapshot.forEach(function(daySnapshot){//repeat for every day in the month snapshot. 
						totalNumClasses++;
						for(i = 0; i < $scope.classList.length; i++){
							if(daySnapshot.child($scope.classList[i].studentID).exists()){//we determine if the student ID selected by the user exist. 
								$scope.attendanceData[i] = $scope.attendanceData[i] + 1;//we increment the number of classes found in the data. 
							}
						}
					})
				})
				for(i = 0; i < $scope.classList.length; i++)$scope.attendanceData[i] = (($scope.attendanceData[i] / totalNumClasses) * 100);
				$scope.$apply();//we must manuallly invoke a digest cycle. 
				$scope.preparePDF();
			})
		});
	}
	$scope.data = $firebaseObject(ref);
}])
app.controller('studentReportCtrl', ['$scope', 'GeneratePDF', function($scope, GeneratePDF){
	$scope.studentList = [];
	$scope.studentNumber = '';
	$scope.message = 'Student Report';
	$scope.studentMap = {};//Student Map is an associative array where the keys are student ID numbers and the values are javascript objects containing data for that student with the corresponding ID number.
	$scope.dataCapture = false;
	$scope.courseAttendanceData = []//Course attendanc data is an indexed array hst will be used to keep track of attendance for various courses registered for by th selected student.
	var myDate = new Date();
	var attendanceRef = firebase.database().ref(myDate.getFullYear());
	var currentCourse = '';
	var getData = function(myCourse){
		return attendanceRef.child(myCourse).once('value');
	}
	var preparePDF = function(){
		var pageHeading = 'Attendance Data for ' + $scope.studentMap[$scope.studentNumber].firstName + ' ' + $scope.studentMap[$scope.studentNumber].lastName;
		var columns = ['Course Code', 'Attendance Data'];
		var rows = [];
		for(i = 0; i < $scope.studentMap[$scope.studentNumber].courseList.length; i++){
			var currentRow = [];
			currentRow.push($scope.studentMap[$scope.studentNumber].courseList[i]);
			currentRow.push($scope.courseAttendanceData[i]);
			rows.push(currentRow);
		}
		GeneratePDF.init();
		GeneratePDF.addText(pageHeading);
		GeneratePDF.addTable(columns, rows);

	}
	$scope.viewPDF = function(){
		preparePDF();
		GeneratePDF.generatePDF();
	}
	//The downloadPDF function will be used to download the pdf with the generated data when the user requests to do so. 
	$scope.downloadPDF = function(){
		preparePDF();
		GeneratePDF.downloadPDF();
	}
	//The generate data function will calculate the attendance rate for the student selected by the user. 
	$scope.generateData = function(){
		var numClasses = 0;
		for(i = 0; i < $scope.studentMap[$scope.studentNumber].courseList.length; i++){
			currentCourse = $scope.studentMap[$scope.studentNumber].courseList[i];
			getData(currentCourse).then(function(attendanceSnapshot){//The firebase reference was put inside a seperate function as making multiple asynchronous request within a for loop that uses data the changes with the for loop gave incorrect reults.
				var currentNumClasses = 0;//Total Number of classes for the current Course
				var classesAttended = 0;//Total number of classes Attended by the curren Student. 
				var percentage = 0.0;
				attendanceSnapshot.forEach(function(monthSnapshot){
					monthSnapshot.forEach(function(daySnapshot){
						if(daySnapshot.child($scope.studentNumber).exists())classesAttended = classesAttended + 1;//If the student ID number exists within the daySnapshot, we increment the number of classes attended.
						currentNumClasses = currentNumClasses + 1;//We must always increment the total number of classes. 
					})
				})
				percentage = ((classesAttended / currentNumClasses) * 100)//We calculate the percentage of classes that were attended by the student. 
				if(isNaN(percentage)) $scope.courseAttendanceData[this.i] = 'No Data For this Course';
				else {
					percentage = percentage.toFixed(2) + '%';//The toFixed method was used to round the precentage calculated to 2 decimal places. This mthod also returns a string. 
					$scope.courseAttendanceData[this.i] = percentage;
				}
				//manually invoke a digest cycle as we are manipulating the scope variables outside the range of the scope. 
				$scope.$apply();

			}.bind({i: i}))
		}
		$scope.dataCapture = true;//Set the data capture varibale to true so that additional content can be displayed on the web page. 
	}
	var listRef = firebase.database().ref('Students');
	//The following event Listener will be used to retrieve data on all students registered for courses. 
	listRef.once('value').then(function(allCourseSnapshot){
		allCourseSnapshot.forEach(function(courseSnapshot){
			courseSnapshot.forEach(function(studentSnapshot){
				var studentID = studentSnapshot.val().studentID;//we extract the student ID from the snapshot recieved from firebase. 
				if($scope.studentMap[studentID] == null){//If we have not already encountered this Student ID. We must add it to the list of distinct students. 
					$scope.studentMap[studentID] = studentSnapshot.val();
					$scope.studentMap[studentID].courseList = [];//Each student object will be given a course list attribute which is an indexed array that will be used to store a list of courses registered for the the corresponding student. 
					$scope.studentMap[studentID].courseList.push(courseSnapshot.key);//We add the current course under which the student is registered to the list of courses. 
					$scope.studentList.push(studentID)//we add this new student to the list of student Objects. 
				}
				else $scope.studentMap[studentID].courseList.push(courseSnapshot.key);
			})
		})
	})
}])
