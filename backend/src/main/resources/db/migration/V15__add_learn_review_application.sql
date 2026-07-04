alter table learn_session_items
    add column review_quality_override varchar(20),
    add column review_applied_at timestamp;
