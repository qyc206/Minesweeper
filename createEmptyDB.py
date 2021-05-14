import sqlite3
from sqlite3 import Error

db_file = "savedGames.db"

def create_connection(db_file):
    """ create a database connection to the SQLite database
        specified by db_file
    :param db_file: database file
    :return: Connection object or None
    """
    conn = None
    try:
        conn = sqlite3.connect(db_file)
        return conn
    except Error as e:
        print(e)

    return conn

def create_table(conn, create_table_sql):
    """ create a table from the create_table_sql statement
    :param conn: Connection object
    :param create_table_sql: a CREATE TABLE statement
    :return:
    """
    try:
        c = conn.cursor()
        c.execute(create_table_sql)
    except Error as e:
        print(e)

table = '''CREATE TABLE IF NOT EXISTS savedGames(
            hostname_ip text NOT NULL, 
            gameStatus BLOB NOT NULL,
            gameName text NOT NULL
        );'''

conn = create_connection(db_file)

# create tables
if conn is not None:
    # create projects table
    create_table(conn, table)
else:
    print("Error! cannot create the database connection.")