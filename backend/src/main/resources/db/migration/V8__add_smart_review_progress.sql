alter table user_progress add column correct_count integer not null default 0;
alter table user_progress add column wrong_count integer not null default 0;
alter table user_progress add column streak_correct_count integer not null default 0;
alter table user_progress add column ease_factor double precision not null default 2.50;
alter table user_progress add column interval_days integer not null default 0;
alter table user_progress add column repetition_count integer not null default 0;
alter table user_progress add column lapse_count integer not null default 0;
alter table user_progress add column last_quality varchar(30);
alter table user_progress add column last_response_time_ms integer;
alter table user_progress add column last_reviewed_at timestamp;
alter table user_progress add column next_review_at timestamp;

update user_progress
set correct_count = known_count,
    wrong_count = unknown_count,
    lapse_count = difficult_count,
    streak_correct_count = known_count;

update user_progress set status = 'REVIEW' where status = 'KNOWN';
update user_progress set status = 'LEARNING' where status = 'UNKNOWN';
update user_progress set status = 'DIFFICULT' where status = 'DIFFICULT';

create index idx_user_progress_next_review_at on user_progress(next_review_at);
create index idx_user_progress_status on user_progress(status);
