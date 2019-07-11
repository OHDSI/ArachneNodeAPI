alter table analyses add column if not exists study_title varchar;

update analyses set study_title = studies.title
	from studies
	where
		studies.id = analyses.study_id and analyses.study_id is not null;

alter table analyses drop constraint if exists fk_analyses_study_id;
alter table analyses drop column if exists study_id;

drop table studies;