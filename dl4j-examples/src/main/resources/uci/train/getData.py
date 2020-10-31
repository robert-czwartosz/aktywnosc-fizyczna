import sqlite3
from sqlite3 import Error
import numpy
import random
import shutil
import os

shutil.rmtree('features\\', ignore_errors=True) 
shutil.rmtree('labels\\', ignore_errors=True) 
os.mkdir('features')
os.mkdir('labels\\')

from sklearn.preprocessing import LabelEncoder

 
def create_connection(db_file):
    """ create a database connection to the SQLite database
        specified by the db_file
    :param db_file: database file
    :return: Connection object or None
    """
    conn = None
    try:
        conn = sqlite3.connect(db_file)
    except Error as e:
        print(e)
 
    return conn

def select_all(conn):
    """
    Query all rows in the tasks table
    :param conn: the Connection object
    :return:
    """
    cur = conn.cursor()
    cur.execute("SELECT Timestamp, Przyspieszenie, Aktywnosc FROM Pomiary")
 
    rows = cur.fetchall()
    cur.close()
    return rows


database = r"SensorDatabase.db"
TimeSerieLen = 60

# create a database connection
conn = create_connection(database)
with conn:
    data = select_all(conn)

Inputs = []
Labels = []
acc0 = 10

time = data[0][0]
for row in data:
    Inputs.append([row[1], row[1] - acc0])
    Labels.append(row[2])
    acc0 = row[1]
    time = row[0]


le = LabelEncoder()
Labels = le.fit(Labels).transform(Labels)
print(Labels)
print(le.inverse_transform([0,1]))

file = open("label_dict.txt", "w", encoding="utf-8")
for clas in list(le.classes_):
    file.write(clas+":"+str(le.transform([clas])[0])+"\n")
file.close()

names = list(map( lambda x: 'X'+str(x), list(range(len(Labels)-TimeSerieLen)) ))
#names = np.array(names)
#np.random.shuffle(names)

TimeSeries = []
TimeSerie = []
for idx in range(TimeSerieLen,len(Labels)):
    TimeSerie = Inputs[idx-TimeSerieLen:idx]    
    numpy.savetxt(".\\features\\"+str(names[idx-TimeSerieLen])+".csv", numpy.asarray(TimeSerie), delimiter=",")
    numpy.savetxt(".\\labels\\"+str(names[idx-TimeSerieLen])+".csv", numpy.asarray([Labels[idx]], dtype=numpy.int), fmt='%i', delimiter=",")


