-- ALTER SEQUENCE assessment_assessmentid_seq RESTART WITH 1;
-- ALTER SEQUENCE assessmentversion_assessmentversionid_seq RESTART WITH 1;
-- ALTER SEQUENCE patient_patientid_seq RESTART WITH 1;
-- ALTER SEQUENCE provider_providerid_seq RESTART WITH 1;
-- ALTER SEQUENCE rwuser_rwuserid_seq RESTART WITH 1;

-- insert into rwUser (
-- 	rwusername, oktaclientid 
-- ) values (
-- 	'benoitmarsot@hotmail.com', '0oa8u3365eijW0uoT5d7'
-- );
-- insert into rwUser (
-- 	rwusername, oktaclientid 
-- ) values (
-- 	'benoitmarsot@gmail.com', 'someLongOktaCode'
-- );
-- insert into public.provider ( rwuserid) values ( 1 );

-- insert into public.patient  ( rwuserid) values ( 2 );

-- insert into public.assessment (
-- 	providerId, patientid 
-- ) values (
-- 	1,1
-- );
-- insert into public.assessmentversion (
-- 	assessmentid, note
-- ) values (1,'firstNote'	);

-- insert into public.bodyquestion (
-- 	bodyquestionid, assessmentid, x, y
-- ) values (
-- 	1, 1, 0, 0
-- );
-- insert into public.bodyquestiontext (
--     bodyquestionid,assessmentversionid,questionText
-- ) values (
--     1,1,'first ouch'
-- );
-- select * from rwuser;


-- delete from rwuser;
-- select * from bodyquestion b; 

-- select (value->'bodyQuestionId')::+1, value->'x',value->'y' from json_array_elements(
-- 	('{"assessmentId":0,"providerId":1,"patientId":1,"assessmentVersions":[{"assessmentVersionId":0,"note":"firstNote"}],"bodyQuestions":[{"bodyQuestionId":0,"x":144.109375,"y":133.71875,"versionTexts":[]},{"bodyQuestionId":1,"x":115.109375,"y":181.71875,"versionTexts":[]},{"bodyQuestionId":2,"x":97.109375,"y":268.71875,"versionTexts":[{"assessmentVersionId":0,"content":"First ouch"}]},{"bodyQuestionId":3,"x":105.109375,"y":281.71875,"versionTexts":[]},{"bodyQuestionId":4,"x":132.109375,"y":305.71875,"versionTexts":[{"assessmentVersionId":0,"content":"First ouch"}]},{"bodyQuestionId":5,"x":170.109375,"y":348.71875,"versionTexts":[]}]}'
-- 	)::json #>'{"bodyQuestions"}' 
-- );