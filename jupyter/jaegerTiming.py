import json
import requests
import sys
from datetime import datetime

def TimestampMicrosec64():
    td = (datetime.utcnow() - datetime(1970, 1, 1))
    return td.microseconds + (td.seconds + td.days * 24 * 3600) * 10**6

rows = {}
count = 0

while(True):

    # #API request to retrieve list of services
    # r = requests.get('http://localhost:16686/api/services')
    # data = r.json()
    # services = data['data']

    # #Remove jaeger-query service (not interested in traces related)
    # if 'jaeger-query' in services:
    #     services.remove('jaeger-query')

    #Select all traceIDs of traces related to service frontend
    traces = []
    #for service in services:
	#API request to retrieve list of traces related to a service in the precedent minute
    r = requests.get("http://localhost:16686/api/traces?service=frontend&start=" + str(TimestampMicrosec64() - 60000000))
    data = r.json()
    for trace in data['data']:
        traces.append(trace['traceID'])

    #Select unique traceIDs (a trace is seen by all services in the request flow)
    traces = set(traces)
    traces = list(traces)

    #Print total number of traces considered
    print(str(TimestampMicrosec64() - 60000000))
    print("Num traces:" + str(len(traces)))

    for trace in traces:

    	#API Request to retrieve all data related to a trace 
        r = requests.get('http://localhost:16686/api/traces/' + trace)
        data = r.json()

        tracesJson = data['data']
        query = ""
        referenceString = ""

        #Loop on the list of traces
        for i in range(len(tracesJson)):

            if (i > 0):
            	#Probably is impossible to have more than one trace
                print("ATTENTION more than one trace")

            traceJson = tracesJson[i]
            traceID = traceJson['traceID']

            #SPAN
            for span in traceJson['spans']:
                
                key = span['traceID'] + span['spanID']
                if key not in rows:
                    #traceId, spanId, startTime, duration, eventTime
                    row = span['traceID'] + ", " + span['spanID'] + ", " + str(span['startTime']) + ", " + str(span['duration']) + ", " + str(TimestampMicrosec64()) + "\n"
                    rows[key] = row

    
    if not traces:                      
        fd = open('/Users/Mario/Desktop/jaegerTiming.csv','w')
        for row in rows.keys():
            fd.write(rows[row])  
        fd.close()
        count += 1
        if count > 100:
            break
    else:
        count = 0

