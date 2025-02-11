CREATE OR REPLACE procedure sp_createforeignkeys()
language plpgsql
as $$
declare
-- variable declaration
begin
	raise notice 'Create Foreign keys:';
--      Actually no foreign key the key path is bodyQuestionText->AssessmentVersion->BodyQuestion
-- 	if not exists (select constraint_name from information_schema.table_constraints where table_name = 'bodyquestiontext' and constraint_name = 'bodyquestiontext_fk_bodyquestion') then
-- 		raise notice 'Creating bodyquestiontext_fk_bodyquestion...';
-- 		alter table bodyquestiontext add constraint bodyquestiontext_fk_bodyquestion foreign key (bodyquestionid,assessmentid) references bodyquestion(bodyquestionid,assessmentid);
-- 	end if;

end; $$;

call public.sp_CreateForeignKeys();
drop procedure public.sp_CreateForeignKeys;

