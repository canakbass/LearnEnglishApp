import json
import sqlite3
import os
import time

def setup_db():
    json_path = 'app/src/main/assets/system_words.json'
    db_path = 'app/src/main/assets/english_words_db.db'
    
    try:
        if not os.path.exists(json_path):
            print(f"Error: {json_path} not found")
            return

        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        if os.path.exists(db_path):
            os.remove(db_path)

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("PRAGMA foreign_keys = ON;")

        cursor.executescript('''
            CREATE TABLE words (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                engWord TEXT NOT NULL,
                turWord TEXT NOT NULL,
                picturePath TEXT,
                audioPath TEXT,
                level TEXT,
                category TEXT,
                source TEXT,
                createdAt INTEGER
            );
            CREATE TABLE word_samples (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                wordId INTEGER,
                sentence TEXT,
                FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE
            );
            CREATE TABLE word_progress (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                wordId INTEGER,
                easeFactor REAL,
                interval INTEGER,
                nextReview INTEGER,
                repetitions INTEGER,
                state INTEGER,
                FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE
            );
            CREATE TABLE quiz_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                startTime INTEGER,
                endTime INTEGER,
                score INTEGER
            );
            CREATE TABLE quiz_answers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sessionId INTEGER,
                wordId INTEGER,
                isCorrect INTEGER,
                FOREIGN KEY(sessionId) REFERENCES quiz_sessions(id) ON DELETE CASCADE,
                FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE
            );
            CREATE TABLE settings (
                id INTEGER PRIMARY KEY,
                dailyGoal INTEGER,
                isNotificationEnabled INTEGER
            );
        ''')

        current_millis = int(time.time() * 1000)
        
        for index, item in enumerate(data):
            # Check for keys 'word' or 'engWord' and 'translation' or 'turWord'
            eng = item.get('word') or item.get('engWord')
            tur = item.get('translation') or item.get('turWord')
            
            if not eng or not tur:
                print(f"Skipping item at index {index} due to missing data: {item}")
                continue

            cursor.execute('''
                INSERT INTO words (engWord, turWord, picturePath, audioPath, level, category, source, createdAt)
                VALUES (?, ?, NULL, NULL, ?, ?, 'system', ?)
            ''', (eng, tur, item.get('level'), item.get('category'), current_millis))
            
            word_id = cursor.lastrowid
            
            samples = item.get('samples', [])
            for sample in samples:
                cursor.execute('INSERT INTO word_samples (wordId, sentence) VALUES (?, ?)', (word_id, sample))

        cursor.execute('INSERT INTO settings (id, dailyGoal, isNotificationEnabled) VALUES (1, 20, 1)')
        
        conn.commit()

        cursor.execute("SELECT count(*) FROM words")
        words_count = cursor.fetchone()[0]
        cursor.execute("SELECT count(*) FROM word_samples")
        samples_count = cursor.fetchone()[0]
        cursor.execute("SELECT count(*) FROM settings")
        settings_count = cursor.fetchone()[0]
        
        file_size = os.path.getsize(db_path)
        
        print(f"Database created successfully.")
        print(f"Words count: {words_count}")
        print(f"Word samples count: {samples_count}")
        print(f"Settings count: {settings_count}")
        print(f"File size: {file_size} bytes")

        conn.close()

    except Exception as e:
        print(f"An error occurred: {str(e)}")

if __name__ == '__main__':
    setup_db()
