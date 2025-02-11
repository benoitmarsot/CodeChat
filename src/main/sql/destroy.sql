/**
 * Author:  benoitmarsot
 * Created: Mar 30, 2023
 */
-- ALTER SEQUENCE assessment_assessmentid_seq RESTART WITH 1;
-- ALTER SEQUENCE assessmentversion_assessmentversionid_seq RESTART WITH 1;
-- ALTER SEQUENCE patient_patientid_seq RESTART WITH 1;
-- ALTER SEQUENCE provider_providerid_seq RESTART WITH 1;
ALTER SEQUENCE core.user_id_seq RESTART WITH 1;

-- drop table public.bodyquestiontext;
-- drop table public.bodyquestion;
-- drop table public.assessmentversion;
-- drop table public.assessment;
-- drop table public.providerpatient;
-- drop table public.patient;
-- drop table public.provider;
drop table core.user;

