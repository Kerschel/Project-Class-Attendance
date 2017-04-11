import csv
import sys
import json
import time
from firebase import firebase

Firebase = firebase.FirebaseApplication('https://classattend.firebaseio.com/')

def csv_dict_list(varibale_file):
	days_of_week = {'Mon': 'Monday', 'Tue': 'Tuesday', 'Wed': 'Wednesday', 
	'Thu': 'Thursday', 'Fri': 'Friday', 'Sat': 'Saturday', 'Sun': 'Sunday'}
	
	reader = csv.DictReader(open(varibale_file, 'r'))
	dict_list = []
	current = {}
	for line in reader:
		if(line['_event_id'] != ''):
			current = {} 
			current['eventId'] = line['_event_id']
			current['dayOfWeek'] = days_of_week[line['_day_of_week']]
			current['startTime'] = line['_start_time']
			current['endTime'] = line['_end_time']
			current['weeks'] = line['_weeks']
			current['type'] = line['_event_catName']
		else:
			if line['_resType'] == 'Module':
				current['courseCode'] = line['_resName']
			else:
				current['room'] = line['_resName']
				if current['room'] == 'FST CSL1' or current['room'] == 'FST CSL2':
					if 'courseCode' in current:
						dict_list.append(current)
	return dict_list

def main():
	count =0
	print("Uploading Data...")
	courseList = list()
	dict_list = csv_dict_list(sys.argv[1])
	#For Every Class found in the timetable file, we post it to Firebase. 
	for key in dict_list:
		if key['courseCode'] not in courseList and count !=15 and key['courseCode'] != "COMP 1601":
			courseResult = Firebase.post('/Courses', key['courseCode'])
			courseList.append(key['courseCode'])
			count +=1

	for key in dict_list:
		if key['courseCode'] in courseList and key['courseCode'] != "COMP 1601"and key['courseCode'] != "COMP 1011":
		   result = Firebase.post('/class', key)


	print(str(len(dict_list)) + " records Succesfully Uploaded")
if __name__ == '__main__':
    main()