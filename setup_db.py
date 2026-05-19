import sqlite3
import os
import re

db_path = r'app\src\main\assets\english_words_db.db'
sql_path = r'app\src\main\assets\complete_english_words_database.sql'

try:
    if os.path.exists(db_path): os.remove(db_path)
    os.makedirs(os.path.dirname(db_path), exist_ok=True)
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.executescript("""
    CREATE TABLE words (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, engWord TEXT, turWord TEXT, picturePath TEXT, audioPath TEXT, level TEXT, category TEXT, source TEXT, createdAt INTEGER);
    CREATE TABLE word_samples (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, wordId INTEGER NOT NULL, sampleSentence TEXT, translation TEXT, FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE);
    CREATE TABLE word_progress (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, wordId INTEGER NOT NULL, boxNumber INTEGER NOT NULL, lastReviewDate INTEGER, nextReviewDate INTEGER, isMastered INTEGER NOT NULL, FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE);
    CREATE TABLE quiz_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, startTime INTEGER NOT NULL, endTime INTEGER, score INTEGER NOT NULL, totalQuestions INTEGER NOT NULL);
    CREATE TABLE quiz_answers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sessionId INTEGER NOT NULL, wordId INTEGER NOT NULL, isCorrect INTEGER NOT NULL, givenAnswer TEXT, FOREIGN KEY(sessionId) REFERENCES quiz_sessions(id) ON DELETE CASCADE, FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE);
    CREATE TABLE settings (id INTEGER PRIMARY KEY NOT NULL, dailyGoal INTEGER NOT NULL, notificationsEnabled INTEGER NOT NULL, theme TEXT);
    """)
    cursor.execute("INSERT INTO settings (id, dailyGoal, notificationsEnabled, theme) VALUES (1, 10, 1, 'light')")
    
    if os.path.exists(sql_path):
        with open(sql_path, 'r', encoding='latin-1') as f:
            for line in f:
                # Matches: INSERT INTO Words (englishWord, turkishMeaning, exampleSentence, wordLevel, wordType) VALUES ('apple', 'elma', '...', 'A1', 'noun');
                match = re.search(r"INSERT INTO Words\s*\(englishWord, turkishMeaning, exampleSentence, wordLevel, wordType\)\s*VALUES\s*\('(.*?)', '(.*?)', '(.*?)', '(.*?)', '(.*?)'\)", line, re.IGNORECASE)
                if match:
                    # Map engWord, turWord, picturePath(example), audioPath(None), level, category(wordType), source, createdAt
                    cursor.execute("INSERT INTO words (engWord, turWord, picturePath, audioPath, level, category, source, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", (match.group(1), match.group(2), match.group(3), None, match.group(4), match.group(5), 'initial_import', 0))
    
    conn.commit()
    cursor.execute("SELECT COUNT(*) FROM words")
    w_count = cursor.fetchone()[0]
    cursor.execute("SELECT COUNT(*) FROM settings")
    s_count = cursor.fetchone()[0]
    print(f"Words: {w_count}")
    print(f"Settings: {s_count}")
    print(f"Size: {os.path.getsize(db_path)}")
    conn.close()
except Exception as e:
    print(f"Error: {e}")
