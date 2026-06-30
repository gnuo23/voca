alter table learn_sessions
    add column goal varchar(30) not null default 'MASTER_ALL',
    add column answer_direction varchar(30) not null default 'BOTH',
    add column grading_mode varchar(30) not null default 'ACCENT_INSENSITIVE',
    add column enabled_question_types varchar(100) not null default 'MCQ,TRUE_FALSE,WRITTEN';
