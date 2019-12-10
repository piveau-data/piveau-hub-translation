DROP TABLE IF EXISTS translation_request;
DROP TABLE IF EXISTS translation;
DROP TABLE IF EXISTS auth;
DROP TABLE IF EXISTS finished_translation;
DROP TABLE IF EXISTS active_translation;

CREATE TABLE IF NOT EXISTS translation_request (tr_id TEXT,
                                                original_language TEXT,
                                                target_languages TEXT,
                                                num_translations INT,
                                                data_dict TEXT,
                                                callback_url TEXT,
                                                callback_method TEXT,
                                                callback_auth TEXT,
                                                transmission_date TEXT,
                                                is_sended BOOLEAN,
                                                PRIMARY KEY (tr_id)
                                               );

CREATE TABLE IF NOT EXISTS translation (tr_id TEXT,
                                        language TEXT,
                                        field_name TEXT,
                                        translated_text TEXT,
                                        PRIMARY KEY (tr_id, language, field_name)
                                       );

CREATE TABLE IF NOT EXISTS auth (ex_reference TEXT,
                                 request_id TEXT,
                                 tr_id TEXT,
                                 create_date TEXT,
                                 PRIMARY KEY (ex_reference)
                                );

CREATE TABLE IF NOT EXISTS finished_translation (tr_id TEXT,
                                                 transmission_date TEXT,
                                                 finished_date TEXT,
                                                 duration INT,
                                                 original_language TEXT,
                                                 num_target_languages INT
                                                );

CREATE TABLE IF NOT EXISTS active_translation(tr_id TEXT, sended_date TEXT, PRIMARY KEY (tr_id));
