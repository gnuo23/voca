alter table learn_answers
    add column verdict varchar(20),
    add column similarity_score double precision not null default 0,
    add column stage_before varchar(30);

update learn_answers
set verdict = case when correct then 'CORRECT' else 'INCORRECT' end
where verdict is null;

alter table learn_answers
    alter column verdict set not null;
