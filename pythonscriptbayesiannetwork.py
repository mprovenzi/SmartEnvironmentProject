import sys
import numpy
from libpgm.nodedata import NodeData
from libpgm.graphskeleton import GraphSkeleton
from libpgm.discretebayesiannetwork import DiscreteBayesianNetwork
from libpgm.tablecpdfactorization import TableCPDFactorization

#from datetime import datetime
#dtbef = datetime.now()

#path_bn1 = "BNstep1dict.txt"
#path_bn2 = "BNstep2dict.txt"

path_bn1 = "jade/src/smartenvironment/LightAgent/BNstep1dict.txt"
path_bn2 = "jade/src/smartenvironment/LightAgent/BNstep2dict.txt"

# capture arguments - order: wkday, hour, locat, activ
userinput = [sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]]

# setting lists of values and dictionary
wkdayValsList = ['Weekday', 'Weekend']
hourValsList = ["0to3", "3to6", "6to9", "9to12", "12to15", "15to18", "18to21", "21to24"]
locatValsList = ["Idle", "Bed", "Hall", "Both"]
activValsList = ["Away", "Sleeping", "Wandering", "Reading", "Diverse"]
dictionary = set().union(wkdayValsList, hourValsList, locatValsList, activValsList)

# checking if input from user was approppriate
if set(userinput).issubset(dictionary):
	# initializing probabilities lists
	wkdayProbList = []
	hourProbList  = []
	locatProbList = []
	activProbList = []
	
	#INITIALIZING BN 1
	# load nodedata and graphskeleton
	nd1 = NodeData()
	skel1 = GraphSkeleton()
	nd1.load(path_bn1)
	skel1.load(path_bn1)
	skel1.toporder() # toporder graph skeleton
	
	#INITIALIZING BN 2
	# load nodedata and graphskeleton
	nd2 = NodeData()
	skel2 = GraphSkeleton()
	nd2.load(path_bn2)
	skel2.load(path_bn2)
	skel2.toporder() # toporder graph skeleton

	# FINDING NEXT ACTIVITY ATTRIBUTES THROUGH INFERENCE ON BN 1
	# wkday variable query
	evidence1 = dict(wkdayT0=userinput[0])
	for i,item in enumerate(wkdayValsList):
		# loading bayesian network and factorization - needs to be done at every iteration
		bn1 = DiscreteBayesianNetwork(skel1, nd1)
		fn1 = TableCPDFactorization(bn1)
		# setting the query
		query1 = dict(wkdayT1 = [item])
		# querying in accordance to the given evidence and appending it to the list of probability of each value
		wkdayProbList.append(fn1.specificquery(query1, evidence1))
		#print "Iteration: " + str(i) + "-> wkdayTO (Input): " + userinput[0] + "; wkdayT1 (Output): " + item + " - prob: " + str(wkdayProbList[i])
	most_probable_wkdayT1 = wkdayValsList[numpy.argmax(wkdayProbList)]

	# hour variable query
	evidence1 = dict(hourT0=userinput[1])
	for i,item in enumerate(hourValsList):
		# loading bayesian network and factorization - needs to be done at every iteration
		bn1 = DiscreteBayesianNetwork(skel1, nd1)
		fn1 = TableCPDFactorization(bn1)
		# setting the query
		query1 = dict(hourT1 = [item])
		# querying in accordance to the given evidence and appending it to the list of probability of each value
		hourProbList.append(fn1.specificquery(query1, evidence1))
		#print "Iteration: " + str(i) + "-> hourTO (Input): " + userinput[1] + "; hourT1 (Output): " + item + " - prob: " + str(hourProbList[i])
	most_probable_hourT1 = hourValsList[numpy.argmax(hourProbList)]

	# locat variable query
	evidence1 = dict(locatT0=userinput[2])
	for i,item in enumerate(locatValsList):
		# loading bayesian network and factorization - needs to be done at every iteration
		bn1 = DiscreteBayesianNetwork(skel1, nd1)
		fn1 = TableCPDFactorization(bn1)
		# setting the query
		query1 = dict(locatT1 = [item])
		# querying in accordance to the given evidence and appending it to the list of probability of each value
		locatProbList.append(fn1.specificquery(query1, evidence1))
		#print "Iteration: " + str(i) + "-> locatTO (Input): " + userinput[2] + "; locatT1 (Output): " + item + " - prob: " + str(locatProbList[i])
	most_probable_locatT1 = locatValsList[numpy.argmax(locatProbList)]
	
	# FINDING NEXT ACTIVITY PROBABILITIES THROUGH INFERENCE ON BN 2
	evidence2 = dict(activT0=userinput[3], wkdayT1=most_probable_wkdayT1, hourT1=most_probable_hourT1, locatT1=most_probable_locatT1)
	for i,item in enumerate(activValsList):
		# loading bayesian network and factorization - needs to be done at every iteration
		bn2 = DiscreteBayesianNetwork(skel2, nd2)
		fn2 = TableCPDFactorization(bn2)
		# setting the query
		query2 = dict(activT1 = [item])
		# querying in accordance to the given evidence and appending it to the list of probability of each value
		activProbList.append(fn2.specificquery(query2, evidence2))
		#print "Iteration: " + str(i) + "-> activT0 (Input): " + userinput[3] + "; activT1 (Output): " + item + " - prob: " + str(activProbList[i])
	most_probable_activT1 = activValsList[numpy.argmax(activProbList)]
	
	print "." + most_probable_activT1 + "." + most_probable_wkdayT1 + "." + most_probable_hourT1 + "." + most_probable_locatT1 + "."
else: 
	print ".ERROR."
	#print "Incorrect arguments! Bayesian Networks did not run. Please check below the correct values."
	#print "Dictionary is: " + ", ".join(str(e) for e in dictionary) # print every element in the set
	#print "Entry was: " + ", ".join(str(e) for e in set(userinput))
	#print "Order should be: wkday hour locat activ"

#dtaft = datetime.now()
#dtdelta = dtaft - dtbef
#print "PYTHON PERIOD " + str((dtdelta.microseconds)/1000) # SEMPRE FICA EM TORNO DE 105 ms
